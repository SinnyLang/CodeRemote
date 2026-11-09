package xyz.sl.coderemote.core.fsm
// File Status Manager
// 用户看到的文件树和打开的文档，现在处于什么状态？
// 它不负责 VFS 怎么读文件（那是 Resource 的事），
// 也不负责像素怎么画（那是 UI 的事），
// 只负责节点身份、加载状态、展开状态、打开文档集合、当前活动文档、会话快照这六件事

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import xyz.sl.coderemote.core.vfs.Resource

sealed class FileNode{
    abstract val key: String
    abstract val name: String
    abstract var parent: Directory?   // val
    abstract val resource: Resource

    /** 文件节点 */
    class File(
        override val key: String,
        override var name: String,
        override var parent: Directory?,
        override var resource: Resource,
    ) : FileNode()

    /** 目录节点 - 支持懒惰加载
     * @param loadChildren 加载该文件夹的子文件
     */
    class Directory(
        override val key: String,
        override var name: String,
        override var parent: Directory?,
        override var resource: Resource,
//        private val loadChildren: suspend (Directory) -> List<FileNode> // 加载子节点的挂起函数
    ) : FileNode() {


        /** 已加载的子节点；null 表示尚未加载。由 FileTree 写入。 */
        var children: List<FileNode>? = null
            internal set

        private val _loadStatus = MutableStateFlow<LoadStatus>(LoadStatus.NotLoaded)
        val loadStatus: StateFlow<LoadStatus> = _loadStatus.asStateFlow()

        /** 同目录串行、不同目录并行；生命周期与节点绑定 */
        internal val loadMutex = Mutex()

        internal fun setLoadStatus(s: LoadStatus) { _loadStatus.value = s }
        internal fun setExpanded(e: Boolean) { _isExpanded.value = e }

        private val _isExpanded = MutableStateFlow(false)  //
        val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()



        //  ====================
        @Deprecated("move to FileTree")
        var _children: List<FileNode>? = null // 子节点列表（只有加载后才会有数据）

        @Deprecated("move to FileTree")
        private var _isLoaded: Boolean = false // 是否已加载

        @Deprecated("move to FileTree")
        private var _isLoading = MutableStateFlow(false) // 是否正在加载
        @Deprecated("move to FileTree")
        val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

        /**
         * 获取子节点（懒惰加载）
         * 如果未加载，则调用 loadChildren 加载
         */
        @Deprecated("move to FileTree")
        suspend fun loadChildren(dir: Directory) : List<FileNode> {
            return listOf()
        }

        @Deprecated("move to FileTree")
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

        @Deprecated("move to FileTree")
        fun isLoaded(): Boolean = _isLoaded

        @Deprecated("move to FileTree")
        fun isLoading(): Boolean = _isLoading.value

        /**  获取缓存的子节点（不会触发加载）
         */
        @Deprecated("move to FileTree")
        fun getCachedChildren(): List<FileNode> = _children ?: emptyList()

        @Deprecated("move to FileTree")
        suspend fun reload() {
            _children = null
            _isLoaded = false
            getChildren()
        }

        /** 手动设置子节点（用于刷新后更新）
         */
        @Deprecated("move to FileTree")
        fun setChildren(children: List<FileNode>) {
            _children = children
            _children?.forEach { it.parent = this }
            _isLoaded = true
        }

        // 切换展开状态
        @Deprecated("move to FileTree")
        fun toggleExpanded() {
            _isExpanded.value = !_isExpanded.value
        }
    }
}