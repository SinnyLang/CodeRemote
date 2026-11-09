package xyz.sl.coderemote.core.fsm;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;


/*
 * 重构 FileNode 和 FileNodeManager
 * https://chat.deepseek.com/share/dsos9ngzzo1epu00h9
 * 是否能让 UI 直接订阅 FileNode 而是不订阅 树加载器 和 文档管理器 。
 ┌─────────────────────────────────────────────────────┐
 │  UI（Compose/View）                                  │
 │   订阅 StateFlow，渲染树和编辑器                        │
 └───────────────┬─────────────────────┬───────────────┘
                 │                     │
 ┌───────────────▼──────────┐  ┌───────▼────────────────┐
 │ FileTree（树加载器）       │  │ OpenDocumentManager    │
 │  - 驱动 Directory 加载    │  │  - 打开的 File 集合      │
 │  - 刷新/重载子树           │  │  - active / MRU        │
 │  - 不关心编辑器            │  │  - File 内容加载状态     │
 └───────────────┬──────────┘  └───────┬────────────────┘
                 │                     │
 ┌───────────────▼─────────────────────▼───────────────┐
 │ FileNode（纯模型）                                    │
 │  File / Directory，只存数据 + 受控状态，不含加载逻辑      │
 └─────────────────────────────────────────────────────┘

 */

/**
 * 管理“被用户点击打开”的 FileNode：
 *  - 按 FileNode 身份去重（同一棵文件树中每个节点唯一）
 *  - File 节点触发内容加载（通过 ContentLoader，由 Kotlin 侧协程实现）
 *  - Directory 节点不加载内容，展开/子节点加载由 Directory 自身负责
 *  - 维护 MRU 顺序与当前 active 节点
 *
 * 线程约定：所有公开方法必须在主线程调用。
 */
public final class FileNodeManager {

    public enum State { LOADING, LOADED, FAILED }

    public static final class Entry {
        private final FileNode node;
        private volatile State state;
        private volatile Throwable error;

        Entry(FileNode node) { this.node = node; }

        public FileNode getNode() { return node; }
        public State getState() { return state; }
        public Throwable getError() { return error; }
    }

    /** 由 Kotlin 侧实现，内部用协程真正加载内容 */
    public interface ContentLoader {
        void load(FileNode node, LoadCallback callback);
    }

    public interface LoadCallback {
        void onLoaded();
        void onError(Throwable error);
    }

    public interface Listener {
        default void onNodeOpened(FileNode node) {}
        default void onNodeClosed(FileNode node) {}
        default void onNodeStateChanged(FileNode node, State oldState, State newState, Throwable error) {}
        default void onActiveNodeChanged(FileNode previous, FileNode current) {}
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<FileNode, Entry> registry = new IdentityHashMap<>();
    private final Deque<FileNode> mru = new ArrayDeque<>();
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    private ContentLoader contentLoader;
    private FileNode active;
    private boolean shutdown;

    public FileNodeManager() {}

    public void setContentLoader(ContentLoader loader) { this.contentLoader = loader; }

    public void addListener(Listener l)    { listeners.add(l); }
    public void removeListener(Listener l) { listeners.remove(l); }

    // ---------------- 查询 ----------------
    public Entry getEntry(FileNode node)     { return registry.get(node); }
    public FileNode getActive()              { return active; }
    public List<FileNode> getOpenNodes()     {
        return Collections.unmodifiableList(new ArrayList<>(registry.keySet()));
    }
    public List<FileNode> getMruNodes()      {
        return Collections.unmodifiableList(new ArrayList<>(mru));
    }

    // ---------------- 打开 / 激活 ----------------
    /**
     * 用户点击文件树节点时调用。
     *  - 已打开：仅刷新 active/MRU，不重复加载。
     *  - 新 File 节点：注册并触发内容加载。
     *  - 新 Directory 节点：注册，状态直接 LOADED（展开/子节点加载由 UI 调 toggleExpanded 处理）。
     */
    public void open(FileNode node) {
        assertMainThread();
        ensureRunning();
        if (node == null) return;

        Entry entry = registry.get(node);
        if (entry == null) {
            entry = new Entry(node);
            registry.put(node, entry);
            for (Listener l : listeners) l.onNodeOpened(node);

            if (node instanceof FileNode.File) {
                startLoad(entry);
            } else {
                // Directory：管理器不参与其懒加载
                entry.state = State.LOADED;
                for (Listener l : listeners) {
                    l.onNodeStateChanged(node, null, State.LOADED, null);
                }
            }
        }

        setActive(node);
    }

    public void setActive(FileNode node) {
        assertMainThread();
        if (node == null || !registry.containsKey(node)) return;
        touchMru(node);
        if (active != node) {
            FileNode prev = active;
            active = node;
            for (Listener l : listeners) l.onActiveNodeChanged(prev, node);
        }
    }

    private void touchMru(FileNode node) {
        mru.remove(node);
        mru.addFirst(node);
    }

    // ---------------- 加载（仅 File 节点） ----------------
    private void startLoad(Entry entry) {
        State old = entry.state;

        if (contentLoader == null) {
            entry.state = State.LOADED;
            entry.error = null;
            notifyState(entry, old, State.LOADED, null);
            return;
        }

        entry.state = State.LOADING;
        entry.error = null;
        notifyState(entry, old, State.LOADING, null);

        contentLoader.load(entry.node, new LoadCallback() {
            @Override public void onLoaded() {
                main.post(() -> finishLoad(entry, null));
            }
            @Override public void onError(Throwable error) {
                main.post(() -> finishLoad(entry, error));
            }
        });
    }

    private void finishLoad(Entry entry, Throwable error) {
        // 加载期间节点已被关闭
        if (registry.get(entry.node) != entry) return;

        State old = entry.state;
        if (error != null) {
            entry.state = State.FAILED;
            entry.error = error;
            notifyState(entry, old, State.FAILED, error);
        } else {
            entry.state = State.LOADED;
            entry.error = null;
            notifyState(entry, old, State.LOADED, null);
        }
    }

    public void retry(FileNode node) {
        assertMainThread();
        Entry entry = registry.get(node);
        if (entry != null
                && entry.state == State.FAILED
                && node instanceof FileNode.File) {
            startLoad(entry);
        }
    }

    private void notifyState(Entry entry, State old, State now, Throwable error) {
        for (Listener l : listeners) {
            l.onNodeStateChanged(entry.node, old, now, error);
        }
    }

    // ---------------- 关闭 ----------------
    public void close(FileNode node) {
        assertMainThread();
        if (node == null) return;
        Entry entry = registry.remove(node);
        if (entry == null) return;

        entry.state = null;
        for (Listener l : listeners) l.onNodeClosed(node);

        mru.remove(node);
        if (active == node) {
            FileNode prev = active;
            active = mru.peekFirst();
            for (Listener l : listeners) l.onActiveNodeChanged(prev, active);
        }
    }

    public void closeActive() {
        assertMainThread();
        if (active != null) close(active);
    }

    // ---------------- 生命周期 ----------------
    public void shutdown() {
        main.post(() -> {
            if (shutdown) return;
            shutdown = true;
            for (FileNode node : new ArrayList<>(registry.keySet())) {
                registry.remove(node);
                for (Listener l : listeners) l.onNodeClosed(node);
            }
            active = null;
            mru.clear();
        });
    }

    // ---------------- 工具 ----------------
    private void ensureRunning() {
        if (shutdown) throw new IllegalStateException("FileNodeManager has been shut down");
    }

    private static void assertMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("FileNodeManager must be called on main thread");
        }
    }
}