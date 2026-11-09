package xyz.sl.coderemote.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.sl.coderemote.core.fsm.FileNode
import xyz.sl.coderemote.core.fsm.FileNodeOperations
import xyz.sl.coderemote.core.fsm.FileRepository
import xyz.sl.coderemote.core.fsm.FileTree
import xyz.sl.coderemote.core.fsm.OpenFileManager
import xyz.sl.coderemote.core.fsm.ResourceFileOperations
import xyz.sl.coderemote.core.fsm.SessionData
import xyz.sl.coderemote.core.fsm.SessionStore
import xyz.sl.coderemote.core.vfs.DirectoryResource
import xyz.sl.coderemote.core.vfs.LocalResource
import xyz.sl.coderemote.core.vfs.Resource

/**
 * FileTreeViewModel - 将 Resource 文件映射成 FileNode，供 UI 使用
 */
// todo: 构建 File Status Manager （FSM）
//  doing:解构 FileNode FileTree FileTreeviewModel 作用
//  next: 编写 OpenFileManager.kt
open class FileTreeViewModel(
    private val repository: FileRepository,
    private val operations: FileNodeOperations,
    private val sessionStore: SessionStore,
    private val rootResource : Resource,
    private val contentLoader: suspend (FileNode.File) -> Any,
) : ViewModel() {

    @Deprecated("")
    val _rootNode = MutableStateFlow<List<FileNode>>(emptyList())
    @Deprecated("")
    val rootNode: StateFlow<List<FileNode>> = _rootNode.asStateFlow()

    // 加载状态
    @Deprecated("")
    val _isLoading = MutableStateFlow(false)
    @Deprecated("")
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误信息
    @Deprecated("")
    val _error = MutableStateFlow<String?>(null)
    @Deprecated("")
    val error: StateFlow<String?> = _error.asStateFlow()



    val workspaceKey: String = repository.normalizeKey(rootResource.uri.toString())

    var root: FileNode.Directory = FileNode.Directory(
        key = workspaceKey,
        name = rootResource.name ?: "Root",
        resource = rootResource,
        parent = null,
    )

    private val fileTree = FileTree(repository, operations)
    val openFileManager = OpenFileManager(viewModelScope, contentLoader)

    private val _commandError = MutableStateFlow<String?>(null)
    val commandError: StateFlow<String?> = _commandError.asStateFlow()

    init {
         viewModelScope.launch {
             fileTree.loadChildren(root)
//             restoreSession()
         }
        // startAutoSave()
    }

    // ---------- 会话恢复 ----------
    private suspend fun restoreSession() {
        val saved = sessionStore.load(workspaceKey)
        if (saved == null || saved.rootKey != workspaceKey) {
            runCatching { fileTree.loadChildren(root) }
                .onFailure { _commandError.value = it.message }
            return
        }
        runCatching { fileTree.restoreExpanded(root, saved.expandedKeys) }
            .onFailure { _commandError.value = it.message }
        openFileManager.restore(
            OpenFileManager.Snapshot(saved.openFiles, saved.activeFile),
            resolve = ::findFile,
        )
    }
    private fun findFile(key: String): FileNode.File? {
        if (key == root.key) return null
        val stack = ArrayDeque<FileNode>()
        stack.addLast(root)
        while (stack.isNotEmpty()) {
            when (val cur = stack.removeLast()) {
                is FileNode.File -> if (cur.key == key) return cur
                is FileNode.Directory -> cur.children.orEmpty().forEach { stack.addLast(it) }
            }
        }
        return null
    }

    // ---------- UI 命令 ----------
    fun toggleDirectory(dir: FileNode.Directory) {
        if (dir.isExpanded.value) {
            fileTree.collapse(dir)
        } else {
            viewModelScope.launch {
                runCatching { fileTree.expand(dir) }
                    .onFailure { _commandError.value = it.message }
            }
        }
    }

    fun openFile(file: FileNode.File) = openFileManager.open(file)

    fun retryLoad(dir: FileNode.Directory) = viewModelScope.launch {
        runCatching { fileTree.reload(dir) }
            .onFailure { _commandError.value = it.message }
    }

    fun createFile(parent: FileNode.Directory, name: String) = viewModelScope.launch {
        runCatching { fileTree.createFile(parent, name) }
            .onFailure { _commandError.value = it.message }
    }

    fun createDirectory(parent: FileNode.Directory, name: String) = viewModelScope.launch {
        runCatching { fileTree.createDirectory(parent, name) }
            .onFailure { _commandError.value = it.message }
    }

    fun rename(node: FileNode, newName: String) = viewModelScope.launch {
        runCatching { fileTree.rename(node, newName) }
            .onFailure { _commandError.value = it.message }
    }

    fun delete(node: FileNode) = viewModelScope.launch {
        runCatching { fileTree.delete(node) }
            .onFailure { _commandError.value = it.message }
    }

    fun consumeCommandError() { _commandError.value = null }

    // ---------- 自动保存 ----------
    @OptIn(FlowPreview::class)
    private fun startAutoSave() {
        viewModelScope.launch {
            merge(fileTree.changes, openFileManager.changes)
                .debounce(500)
                .collect { persistSession() }
        }
    }

    private suspend fun persistSession() {
        val snap = openFileManager.snapshot()
        sessionStore.save(
            workspaceKey,
            SessionData(
                rootKey = workspaceKey,
                expandedKeys = fileTree.expandedKeysSnapshot(root).toList(),
                openFiles = snap.openKeys,
                activeFile = snap.activeKey,
            ),
        )
    }

    override fun onCleared() {
        super.onCleared()
        openFileManager.shutdown()
    }


    /** 命令统一入口：捕获异常、成功后刷新父目录 */
    private fun runCommand(block: suspend () -> Unit): Result<Unit> {
        return try {
            viewModelScope.launch {
                runCatching { block() }.onFailure { _commandError.value = it.message }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            _commandError.value = e.message
            Result.failure(e)
        }
    }
}

class FileTreeViewModelFactory(
    private val repository: FileRepository,
    private val operations: FileNodeOperations,
    private val sessionStore: SessionStore,
    private val rootResource: Resource,
    private val contentLoader: suspend (FileNode.File) -> Any,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(FileTreeViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return FileTreeViewModel(
            repository, operations, sessionStore, rootResource, contentLoader,
        ) as T
    }
}

/** 预览专用的 ViewModel
 *  该类应对：预览时不支持动态创建 ViewModel
 */
class FakeRepository() : FileRepository {
    override suspend fun list(dir: FileNode.Directory) = dir.children.orEmpty()
    override fun normalizeKey(uri: String) = uri
}

class PreviewFileTreeViewModel(
    private val mockData: FileNode = sampleFiles(),
    private val sessionStore: SessionStore
) : FileTreeViewModel(
    repository = FakeRepository(),
    operations = ResourceFileOperations(),
    sessionStore = sessionStore,
    rootResource = mockData.resource,
    contentLoader = { file ->
        withContext(Dispatchers.IO) {  }
    },) {

    init {
        root = mockData as FileNode.Directory
    }
}

/**
 *  预览使用的数据
 */
fun sampleFiles(): FileNode.Directory {
    // root
    //  |- src
    //  |   |- main
    //  |   |   |- MainActivity.kt
    //  |   |   |- Utils.kt
    //  |   |- test
    //  |   |- readme
    val resource = LocalResource.emptyLocalResource()

    val MainActivity = FileNode.File("","MainActivity.kt", null, resource)
    val Utils = FileNode.File("","Utils.kt", null, resource)

    val readme = FileNode.File("","README.md", null, resource)
    val main = FileNode.Directory("","main", null, resource)
    val test = FileNode.Directory("","test", null, resource)

    val src = FileNode.Directory("","src", null, resource)

    src.children = listOf(main, test, readme)
    main.children= listOf(MainActivity, Utils)

    return src
}