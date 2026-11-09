package xyz.sl.coderemote.core.fsm

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OpenFileManager(
    private val scope: CoroutineScope,
    private val contentLoader: suspend (FileNode.File) -> Any,
) {
    enum class DocState { LOADING, LOADED, FAILED }

    inner class OpenDoc internal constructor(
        val key: String,
        initialNode: FileNode.File,
    ) {
        private val _node = MutableStateFlow(initialNode)
        val node: StateFlow<FileNode.File> = _node.asStateFlow()

        private val _state = MutableStateFlow(DocState.LOADING)
        val state: StateFlow<DocState> = _state.asStateFlow()

        private val _error = MutableStateFlow<Throwable?>(null)
        val error: StateFlow<Throwable?> = _error.asStateFlow()

        private val _content = MutableStateFlow<Any?>(null)
        val content: StateFlow<Any?> = _content.asStateFlow()

        internal var job: Job? = null
        internal fun updateNode(n: FileNode.File) { _node.value = n }
        internal fun setLoading() {
            _state.value = DocState.LOADING; _error.value = null
        }
        internal fun setLoaded(c: Any) {
            _content.value = c; _state.value = DocState.LOADED; _error.value = null
        }
        internal fun setFailed(e: Throwable) {
            _state.value = DocState.FAILED; _error.value = e
        }
        internal fun cancel() { job?.cancel(); job = null }
    }

    private val docs = LinkedHashMap<String, OpenDoc>()
    private val mru = ArrayDeque<String>()

    private val _active = MutableStateFlow<OpenDoc?>(null)
    val active: StateFlow<OpenDoc?> = _active.asStateFlow()

    private val _opened = MutableStateFlow<List<OpenDoc>>(emptyList())
    val opened: StateFlow<List<OpenDoc>> = _opened.asStateFlow()

    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes: SharedFlow<Unit> = _changes.asSharedFlow()

    private var shutdown = false

    fun open(file: FileNode.File, loadContent: Boolean = true) {
        check(!shutdown) { "OpenFileManager has been shut down" }
        val existing = docs[file.key]
        if (existing != null) {
            existing.updateNode(file)   // 树刷新后节点引用变了，同步更新
            setActive(existing)
            return
        }
        val doc = OpenDoc(file.key, file)
        docs[file.key] = doc
        emitOpened()
        setActive(doc)
        if (loadContent) startLoad(doc)
    }

    fun activate(key: String) { docs[key]?.let { setActive(it) } }

    private fun setActive(doc: OpenDoc) {
        touchMru(doc.key)
        if (_active.value !== doc) {
            _active.value = doc
            _changes.tryEmit(Unit)
        }
    }
    private fun touchMru(key: String) {
        mru.remove(key); mru.addFirst(key)
    }

    private fun startLoad(doc: OpenDoc) {
        doc.setLoading()
        doc.job = scope.launch {
            try {
                val content = contentLoader(doc.node.value)
                if (docs[doc.key] === doc) doc.setLoaded(content)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (docs[doc.key] === doc) doc.setFailed(e)
            }
        }
    }

    fun retry(key: String) {
        val doc = docs[key] ?: return
        if (doc.state.value == DocState.FAILED) startLoad(doc)
    }

    fun close(key: String) {
        val doc = docs.remove(key) ?: return
        doc.cancel()
        mru.remove(key)
        emitOpened()
        if (_active.value === doc) {
            _active.value = mru.firstOrNull()?.let { docs[it] }
        }
    }

    fun closeActive() { _active.value?.let { close(it.key) } }

    fun shutdown() {
        if (shutdown) return
        shutdown = true
        docs.values.forEach { it.cancel() }
        docs.clear(); mru.clear()
        _active.value = null; _opened.value = emptyList()
    }

    private fun emitOpened() {
        _opened.value = docs.values.toList()
        _changes.tryEmit(Unit)
    }

    // ---------- 会话 ----------
    data class Snapshot(val openKeys: List<String>, val activeKey: String?)

    fun snapshot(): Snapshot = Snapshot(mru.toList(), _active.value?.key)

    /**
     * @param resolve 从树中按 key 查 FileNode.File；查不到返回 null（文件已删/未加载），跳过
     * 只有 active 文档预加载内容，其余 tab 先占位
     */
    fun restore(snapshot: Snapshot, resolve: (String) -> FileNode.File?) {
        snapshot.openKeys.asReversed().forEach { key ->
            resolve(key)?.let { open(it, loadContent = (key == snapshot.activeKey)) }
        }
        snapshot.activeKey?.let { activate(it) }
    }
}