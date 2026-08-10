package xyz.sl.coderemote.core

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.sl.coderemote.core.vfs.Resource

sealed class FileNode(
    open var name: String,
    open var parent: Directory?,
    open var resource: Resource
) {
    /** 文件节点
     */
    class File(
        override var name: String,
        override var parent: Directory?,
        override var resource: Resource,
    ) : FileNode(name, parent, resource)

    /** 目录节点 - 支持懒惰加载
     * @param loadChildren 加载该文件夹的子文件
     */
    class Directory(
        override var name: String,
        override var parent: Directory?,
        override var resource: Resource,
        private val loadChildren: suspend (Directory) -> List<FileNode> // 加载子节点的挂起函数
    ) : FileNode(name, parent, resource) {
        private var _children: List<FileNode>? = null // 子节点列表（只有加载后才会有数据）
        private var _isLoaded: Boolean = false // 是否已加载

        private var _isLoading = MutableStateFlow(false) // 是否正在加载
        val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

        private val _isExpanded = MutableStateFlow(false)  //
        val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()

        /**
         * 获取子节点（懒惰加载）
         * 如果未加载，则调用 loadChildren 加载
         */
        suspend fun getChildren(): List<FileNode> {
            if (_children == null && !_isLoading.value) {
                _isLoading.value = true
                try {
                    val loaded = loadChildren(this)
                    _children = loaded
                    // 设置父节点引用
                    _children?.forEach { it.parent = this }
                    _isLoaded = true
                } finally {
                    _isLoading.value = false
                }
            }
            return _children ?: emptyList()
        }

        fun isLoaded(): Boolean = _isLoaded
        fun isLoading(): Boolean = _isLoading.value

        /**  获取缓存的子节点（不会触发加载）
         */
        fun getCachedChildren(): List<FileNode> = _children ?: emptyList()

        suspend fun reload() {
            _children = null
            _isLoaded = false
            getChildren()
        }

        /** 手动设置子节点（用于刷新后更新）
         */
        fun setChildren(children: List<FileNode>) {
            _children = children
            _children?.forEach { it.parent = this }
            _isLoaded = true
        }

        // 切换展开状态
        fun toggleExpanded() {
            _isExpanded.value = !_isExpanded.value
        }
    }
}