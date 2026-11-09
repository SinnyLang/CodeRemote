package xyz.sl.coderemote.core.fsm

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import xyz.sl.coderemote.core.vfs.DirectoryResource
import xyz.sl.coderemote.core.vfs.Resource
import java.net.URI

// ==== FileRepository ============
interface FileRepository {
    suspend fun list(dir: FileNode.Directory): List<FileNode>
    fun normalizeKey(toString: String): String
}

class FileRepositoryImpl : FileRepository{
    override suspend fun list(dir: FileNode.Directory): List<FileNode> {
        TODO("Not yet implemented")
    }

    override fun normalizeKey(toString: String): String {
        TODO("Not yet implemented")
    }

}

class ResourceFileRepository : FileRepository {
    override suspend fun list(dir: FileNode.Directory): List<FileNode> =
        withContext(Dispatchers.IO) {
            (dir.resource as? DirectoryResource)?.list().orEmpty().map { res ->
                val key = normalizeKey(res.uri.toString())
                val name = res.name ?: res.uri.toString()
                if (res.isDirectory) FileNode.Directory(key, name, dir, res)
                else FileNode.File(key, name, dir, res)
            }
        }

    override fun normalizeKey(uri: String): String {
        val u = runCatching { URI(uri).normalize() }.getOrNull() ?: return uri
        val path = u.path.orEmpty().trimEnd('/').ifEmpty { "/" }
        return URI(u.scheme, u.authority, path, null).toString()
    }
}


// ==== FileNodeOperations ============
interface FileNodeOperations {
    suspend fun createFile(parent: Resource, name: String): Resource
    suspend fun createDirectory(parent: Resource, name: String): Resource
    suspend fun rename(resource: Resource, newName: String): Resource?
    suspend fun delete(resource: Resource): Boolean
}

class FileNodeOperationsImpl : FileNodeOperations {
    override suspend fun createFile(
        parent: Resource,
        name: String
    ): Resource {
        TODO("Not yet implemented")
    }

    override suspend fun createDirectory(
        parent: Resource,
        name: String
    ): Resource {
        TODO("Not yet implemented")
    }

    override suspend fun rename(
        resource: Resource,
        newName: String
    ): Resource? {
        TODO("Not yet implemented")
    }

    override suspend fun delete(resource: Resource): Boolean {
        TODO("Not yet implemented")
    }
}

class ResourceFileOperations : FileNodeOperations {
    override suspend fun createFile(parent: Resource, name: String): Resource =
        withContext(Dispatchers.IO) { (parent as DirectoryResource).createFile(name) }

    override suspend fun createDirectory(parent: Resource, name: String): Resource =
        withContext(Dispatchers.IO) { (parent as DirectoryResource).createDirectory(name) }

    override suspend fun rename(resource: Resource, newName: String): Resource? =
        withContext(Dispatchers.IO) { resource.rename(newName) }

    override suspend fun delete(resource: Resource): Boolean =
        withContext(Dispatchers.IO) { resource.delete() }
}


// ===== File Tree ===================
class FileTree(
    private val repository: FileRepository,
    private val operations: FileNodeOperations,
) {
    /** 会话变化信号：展开/折叠/重载，供 ViewModel 防抖持久化 */
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes: SharedFlow<Unit> = _changes.asSharedFlow()

    // ---------- 加载 ----------
    suspend fun loadChildren(dir: FileNode.Directory, force: Boolean = false): List<FileNode> =
        dir.loadMutex.withLock {
            if (!force) {
                when (dir.loadStatus.value) {
                    LoadStatus.Loaded -> return@withLock dir.children.orEmpty()
                    LoadStatus.Loading -> return@withLock dir.children.orEmpty() // 展示旧数据
                    LoadStatus.NotLoaded, is LoadStatus.Failed -> Unit
                }
            }
            dir.setLoadStatus(LoadStatus.Loading)
            try {
                val children = repository.list(dir)
                dir.children = children
                dir.setLoadStatus(LoadStatus.Loaded)
                children
            } catch (e: CancellationException) {
                dir.setLoadStatus(
                    if (dir.children == null) LoadStatus.NotLoaded else LoadStatus.Loaded
                )
                throw e
            } catch (e: Throwable) {
                dir.setLoadStatus(LoadStatus.Failed(e))
                throw e
            }
        }

    suspend fun reload(dir: FileNode.Directory): List<FileNode> {
        val children = loadChildren(dir, force = true)
        _changes.tryEmit(Unit)
        return children
    }

    // ---------- 展开 ----------
    suspend fun expand(dir: FileNode.Directory): List<FileNode> {
        dir.setExpanded(true)
        _changes.tryEmit(Unit)
        return loadChildren(dir)
    }

    fun collapse(dir: FileNode.Directory) {
        dir.setExpanded(false)
        _changes.tryEmit(Unit)
    }

    // ---------- 命令 ----------
    suspend fun createFile(parent: FileNode.Directory, name: String) {
        operations.createFile(parent.resource, name)
        reload(parent)
    }
    suspend fun createDirectory(parent: FileNode.Directory, name: String) {
        operations.createDirectory(parent.resource, name)
        reload(parent)
    }
    suspend fun rename(node: FileNode, newName: String) {
        operations.rename(node.resource, newName)
        node.parent?.let { reload(it) }
    }
    suspend fun delete(node: FileNode) {
        val parent = node.parent ?: return
        operations.delete(node.resource)
        reload(parent)
    }

    // ---------- 会话快照 ----------
    fun expandedKeysSnapshot(root: FileNode.Directory): Set<String> {
        val out = LinkedHashSet<String>()
        fun dfs(d: FileNode.Directory) {
            if (d.isExpanded.value) out.add(d.key)
            d.children.orEmpty().filterIsInstance<FileNode.Directory>().forEach(::dfs)
        }
        dfs(root)
        return out
    }

    // ---------- 会话恢复：按深度升序逐级加载 ----------
    suspend fun restoreExpanded(root: FileNode.Directory, keys: Collection<String>) {
        if (keys.isEmpty()) return
        loadChildren(root)
        keys.sortedBy { k -> k.count { it == '/' } }.forEach { key ->
            val dir = findDirectory(root, key) ?: return@forEach
            runCatching {
                loadChildren(dir)          // 先让更深层目录实例出现
                dir.setExpanded(true)
            }
        }
        _changes.tryEmit(Unit)
    }

    private fun findDirectory(root: FileNode.Directory, key: String): FileNode.Directory? {
        if (root.key == key) return root
        val stack = ArrayDeque<FileNode.Directory>()
        stack.addLast(root)
        while (stack.isNotEmpty()) {
            val cur = stack.removeLast()
            cur.children.orEmpty().forEach { c ->
                if (c is FileNode.Directory) {
                    if (c.key == key) return c
                    stack.addLast(c)
                }
            }
        }
        return null
    }
}