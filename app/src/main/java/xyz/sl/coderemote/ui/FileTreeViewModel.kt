package xyz.sl.coderemote.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.sl.coderemote.utils.BaseFileManager

open class FileTreeViewModel(
    private val fileManager: BaseFileManager,
    private val rootUri: Uri
) : ViewModel() {
    /**
     *  预览专用设置方法：直接设置数据，不执行真实加载
     */
    fun setPreviewData(nodes: List<FileNode>) {
        _fileTree.value = nodes
        _isLoading.value = false
        _error.value = null
    }

    // 文件树数据 - 使用 StateFlow 供 UI 观察
    val _fileTree = MutableStateFlow<List<FileNode>>(emptyList())
    val fileTree: StateFlow<List<FileNode>> = _fileTree.asStateFlow()

    // 加载状态
    val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误信息
    val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadFileTree()
    }

    // 加载文件树（从根目录开始）
    open fun loadFileTree() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // 假设 EditFileManger 有扫描目录并返回 List<FileNode> 的方法
                val rootNodes : List<FileNode> = withContext(Dispatchers.IO) {
                    fileManager.scanDirectory(rootUri)
                }
                _fileTree.value = rootNodes
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 刷新文件树（重新加载）
    open fun refresh() {
        loadFileTree()
    }

    // 创建文件
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
}

class FileTreeViewModelFactory(
    private val fileManager: BaseFileManager,
    private val rootUri : Uri
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileTreeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FileTreeViewModel(fileManager = fileManager, rootUri = rootUri) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}