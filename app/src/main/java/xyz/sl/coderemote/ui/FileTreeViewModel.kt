package xyz.sl.coderemote.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.sl.coderemote.MainActivity
import xyz.sl.coderemote.utils.BaseFileManager
import xyz.sl.coderemote.core.FileNode
import xyz.sl.coderemote.core.vfs.DirectoryResource
import xyz.sl.coderemote.core.vfs.LocalResource
import xyz.sl.coderemote.core.vfs.Resource

open class FileTreeViewModel(
    private val fileManager: BaseFileManager,
    private val rootResource : Resource
) : ViewModel() {
    /**
     *  预览专用设置方法：直接设置数据，不执行真实加载
     */
    fun setPreviewData(nodes: List<FileNode>) {
        _fileTree.value = nodes
        _isLoading.value = false
        _error.value = null
    }

    val _rootNode = MutableStateFlow<List<FileNode>>(emptyList())
    val rootNode: StateFlow<List<FileNode>> = _rootNode.asStateFlow()

    // 加载状态
    val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误信息
    val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadRootDirectory()
    }

    // 文件树数据 - 使用 StateFlow 供 UI 观察
    @Deprecated("be removed after VFS")
    val _fileTree = MutableStateFlow<List<FileNode>>(emptyList())
    @Deprecated("be removed after VFS")
    val fileTree: StateFlow<List<FileNode>> = _fileTree.asStateFlow()
    // 加载文件树（从根目录开始）
    @Deprecated("be removed after VFS")
    open fun loadFileTree() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // 假设 EditFileManger 有扫描目录并返回 List<FileNode> 的方法
                val rootNodes : List<FileNode> = withContext(Dispatchers.IO) {
//                    fileManager.scanDirectory(Uri.EMPTY)
                    listOf()
                }
                _fileTree.value = rootNodes
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }


    open fun loadRootDirectory(){
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val root = withContext(Dispatchers.IO) {
                    createRootNode(rootResource)
                }
                _rootNode.value = listOf(root)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 创建根节点（包含加载子节点的逻辑）
     */
    private suspend fun createRootNode(rootResource: Resource): FileNode.Directory {
        val name = rootResource.name ?: "Root"

        // 定义加载子节点的函数
        val loadChildren: suspend (FileNode.Directory) -> List<FileNode> = { directory ->
            withContext(Dispatchers.IO) {
                val resourceList = (rootResource as DirectoryResource).list()
                resourceList.map { entry ->
                    when(entry.isDirectory){
                        true -> createDirectoryNode(entry, directory)
                        false -> createFileNode(entry, directory)
                    }
                }
            }
        }

        return FileNode.Directory(
            name = name,
            parent = null,
            resource = rootResource,
            loadChildren = loadChildren
        )
    }

    /** 创建目录节点
     */
    private fun createDirectoryNode(
        cNode: Resource,
        parent: FileNode.Directory
    ): FileNode.Directory {
        val loadChildren: suspend (FileNode.Directory) -> List<FileNode> = { directory ->
            withContext(Dispatchers.IO) {
                val resourceList = (cNode as DirectoryResource).list()
                resourceList.map { entry ->
                    when(entry.isDirectory){
                        true -> createDirectoryNode(entry, directory)
                        false -> createFileNode(entry, directory)
                    }
                }
            }
        }

        return FileNode.Directory(
            name = cNode.name,
            parent = parent,
            resource = cNode,
            loadChildren = loadChildren
        )
    }

    /**
     * 创建文件节点
     */
    private fun createFileNode(
        cNode: Resource,
        parent: FileNode.Directory
    ): FileNode.File {
        return FileNode.File(
            name = cNode.name,
            parent = parent,
            resource = cNode,
        )
    }

    // 刷新文件树
    open fun refresh() {
//        loadFileTree()
//        loadRootDirectory()
    }
    // 重新加载文件树
    open fun reloadFileTree(){
        loadRootDirectory()
    }

    // 创建文件
    @Deprecated("be removed after VFS")
    open fun createFile(parentUri: Uri, fileName: String): Result<Uri> {
        return try {
            val newUri = fileManager.createFile(parentUri, fileName)
            // 创建成功后刷新树
            loadFileTree()
            Result.success(newUri)
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        }
    }

    // 创建目录
    @Deprecated("be removed after VFS")
    open fun createDirectory(parentUri: Uri, dirName: String): Result<Uri> {
        return try {
            val newUri = fileManager.createDirectory(parentUri, dirName)
            loadFileTree()
            Result.success(newUri)
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        }
    }

    // 重命名
    @Deprecated("be removed after VFS")
    open fun renameFile(uri: Uri, newName: String): Result<Unit> {
        return try {
            fileManager.renameFile(uri, newName)
            loadFileTree()
            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        }
    }

    // 删除
    @Deprecated("be removed after VFS")
    open fun deleteNode(uri: Uri, isDirectory: Boolean): Result<Unit> {
        return try {
            if (isDirectory) {
                fileManager.deleteDirectory(uri)
            } else {
                fileManager.deleteFile(uri)
            }
            loadFileTree()
            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        }
    }

    // 创建文件
    open fun createFile(pResource: Resource, fileName: String): Result<Uri> {
        return try {
            val newFile = (pResource as DirectoryResource).createFile(fileName)
            // 创建成功后刷新树
            loadRootDirectory()
            Result.success(newFile.uri)
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        }
    }

    // 创建目录
    open fun createDirectory(pResource: Resource, dirName: String): Result<Uri> {
        return try {
            val newDir = (pResource as DirectoryResource).createDirectory(dirName)
            // 创建成功后刷新树
            loadRootDirectory()
            Result.success(newDir.uri)
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        }
    }

    // 重命名
    open fun renameFile(resource: Resource, newName: String): Result<Unit> {
        return try {
            resource.rename(newName)
            loadRootDirectory()
            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        }
    }

    // 删除
    open fun deleteNode(resource: Resource, isDirectory: Boolean): Result<Unit> {
        return try {
            if (isDirectory) {
                resource.delete()
            } else {
                resource.delete()
            }
            loadRootDirectory()
            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        }
    }

    /**
     * 展开/关闭文件夹
     */
    fun toggleDirectory(directory: FileNode.Directory) {
        if (directory.isExpanded.value == false){
            // 1.加载子文件
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                try {
                    directory.getCachedChildren()
                    directory.reload()
                } catch (e: Exception) {
                    _error.value = e.message
                } finally {
                    _isLoading.value = false
                }
            }
        }

        // 2.切换展开状态
        directory.toggleExpanded()
    }
}

class FileTreeViewModelFactory(
    private val fileManager: BaseFileManager,
    private val rootResource : Resource
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileTreeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FileTreeViewModel(fileManager = fileManager, rootResource = rootResource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/** 预览专用的 ViewModel
 *  该类应对：预览时不支持动态创建 ViewModel
 */
class PreviewFileTreeViewModel(
    private val mockData: List<FileNode> =
        listOf(FileNode.Directory(
            "AA",
            null,
            LocalResource.emptyLocalResource(),
            {listOf()})
        )
) : FileTreeViewModel(
    fileManager = MainActivity.fileManger, // 不会被使用
    rootResource = LocalResource.emptyLocalResource()
) {
    init {
        // 初始化时直接设置模拟数据
        _fileTree.value = mockData
        _rootNode.value = mockData
        _isLoading.value = false
        _error.value = null
    }

    // 覆盖加载方法，不执行任何 I/O 操作
    override fun loadFileTree() {
        // 预览模式不执行任何操作
        _isLoading.value = false
    }

    override fun loadRootDirectory() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _rootNode.value
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun refresh() {
        // 预览模式不执行任何操作
    }

    override fun createFile(parentUri: Uri, fileName: String): Result<Uri> {
        return Result.success(Uri.EMPTY)
    }

    override fun createDirectory(parentUri: Uri, dirName: String): Result<Uri> {
        return Result.success(Uri.EMPTY)
    }

    override fun renameFile(uri: Uri, newName: String): Result<Unit> {
        return Result.success(Unit)
    }

    override fun deleteNode(uri: Uri, isDirectory: Boolean): Result<Unit> {
        return Result.success(Unit)
    }
}

/**
 *  预览使用的数据
 */
fun sampleFiles(): FileNode.Directory {
    val resource = LocalResource.emptyLocalResource()

    val MainActivity = FileNode.File("MainActivity.kt", null, resource)
    val Utils = FileNode.File("Utils.kt", null, resource)
    val mainLoadChild: suspend (FileNode.Directory) -> List<FileNode> = { directory ->
        delay(2000)
        listOf(MainActivity, Utils)
    }

    val readme = FileNode.File("README.md", null, resource)
    val main = FileNode.Directory("main", null, resource, mainLoadChild)

    val testLoadChild: suspend (FileNode.Directory) -> List<FileNode> = { directory ->
        delay(1000)
        listOf()
    }
    val test = FileNode.Directory("test", null, resource, testLoadChild)

    val srcLoadChild: suspend (FileNode.Directory) -> List<FileNode> = { directory ->
        delay(1000)
        listOf(main, test, readme)
    }
    val src = FileNode.Directory("src", null, resource, srcLoadChild)
    return src
}