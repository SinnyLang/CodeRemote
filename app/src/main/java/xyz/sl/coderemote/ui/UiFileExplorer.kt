package xyz.sl.coderemote.ui

import android.util.Log
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import xyz.sl.coderemote.MainActivity
import xyz.sl.coderemote.core.dataStore
import xyz.sl.coderemote.core.fsm.DataStoreSessionStore
import xyz.sl.coderemote.core.fsm.FileNode
import xyz.sl.coderemote.core.fsm.LoadStatus
import xyz.sl.coderemote.utils.LocalFileManger
import kotlin.collections.listOf

fun doNothing(){}

val inComposePreview: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalInspectionMode.current

/**
 * 显示文件树
 * @param nodes 文件树根节点
 * @param indent 缩进。子项目会缩进
 * @param onClickFileNode
 * @param afterClickFileNode
 * @param onLongClickNode 长按事件
 */
@Composable
fun UiFileTreeView(
    fileTreeViewModel: FileTreeViewModel,
    indent: Int = 0,
    onClickFileNode: (file: FileNode) -> Unit = {},
    afterClickFileNode: ()->Unit = {},
    onLongClickNode: (FileNode) -> Unit = {}
) {


    val root = fileTreeViewModel.root
    val rootStatus by root.loadStatus.collectAsState()

    val modifier = Modifier

    when {
        // 首次加载：根还没有任何 children
        root.children == null && rootStatus is LoadStatus.Loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // 首次加载失败
        root.children == null && rootStatus is LoadStatus.Failed -> {
            val err = (rootStatus as LoadStatus.Failed).error
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("加载失败: ${err.message}", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { fileTreeViewModel.retryLoad(root) }) { Text("重试") }
            }
        }

        // 未加载但没在加载、也没失败（极短暂状态，先给个转圈）
        root.children == null && rootStatus is LoadStatus.NotLoaded -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // 有数据（含刷新中保留旧数据）
        else -> {
            FileTreeNodeView(
                nodes = listOf(root),
                viewModel = fileTreeViewModel,
                modifier = modifier,
                depth = 0,
                onClickFileNode = onClickFileNode,
                afterClickFileNode = afterClickFileNode,
                onLongClickNode = onLongClickNode,
            )
        }
    }

}

@Composable
fun FileTreeNodeView(
    nodes: List<FileNode>,
    viewModel: FileTreeViewModel,
    modifier: Modifier = Modifier,
    depth: Int = 0,
    onClickFileNode: (file: FileNode) -> Unit,
    afterClickFileNode: () -> Unit,
    onLongClickNode: (FileNode) -> Unit
) {
    nodes.forEach { node ->
        when (node) {
            is FileNode.File -> {
                FileItemView(
                    file = node,
                    depth = depth,
                    modifier = modifier,
                    onClickFileNode = onClickFileNode,
                    afterClickFileNode = afterClickFileNode,
                    onLongClickNode = onLongClickNode
                )
            }
            is FileNode.Directory -> {
                DirectoryItemView(
                    directory = node,
                    viewModel = viewModel,
                    depth = depth,
                    modifier = modifier,
                    onClickFileNode = onClickFileNode,
                    afterClickFileNode = afterClickFileNode,
                    onLongClickNode = onLongClickNode
                )
            }
        }
    }
}

@Composable
fun DirectoryItemView(
    directory: FileNode.Directory,
    viewModel: FileTreeViewModel,
    depth: Int,
    modifier: Modifier = Modifier,
    onClickFileNode: (file: FileNode) -> Unit,
    afterClickFileNode: () -> Unit,
    onLongClickNode: (FileNode) -> Unit
) {
    val isExpanded by directory.isExpanded.collectAsState()
    val loadStatus by directory.loadStatus.collectAsState()
    val isLoading = loadStatus is LoadStatus.Loading

    Column(modifier = modifier) {
        // 目录行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(25.dp)
                .padding(start = (depth * 10).dp)
                .combinedClickable(
                    onClick = { viewModel.toggleDirectory(directory) },
                    onLongClick = { onLongClickNode(directory) }
                )
                .padding(vertical = 1.dp, horizontal = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = directory.name,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 10.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
        }

        // 子节点
        if (isExpanded) {
            when {
                // 展开但还没拿到数据，正在加载
                directory.children == null && isLoading -> {
                    // 目录行已有转圈，这里可以留空
                }

                // 展开但加载失败
                directory.children == null && loadStatus is LoadStatus.Failed -> {
                    val err = (loadStatus as LoadStatus.Failed).error
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = (depth + 1) * 10.dp)
                            .padding(4.dp),
                    ) {
                        Text(
                            text = "加载失败：${err.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                // 真正加载完成且为空
                directory.children?.isEmpty() == true -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = (depth + 1) * 10.dp)
                            .padding(4.dp),
                    ) {
                        Text(
                            text = "空目录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // 有子节点（刷新中仍保留旧数据，能正常渲染）
                directory.children != null -> {
                    FileTreeNodeView(
                        nodes = directory.children!!,
                        viewModel = viewModel,
                        depth = depth + 1,
                        onClickFileNode = onClickFileNode,
                        afterClickFileNode = afterClickFileNode,
                        onLongClickNode = onLongClickNode,
                    )
                }

            }
        }
    }
}

@Composable
fun FileItemView(
    file: FileNode.File,
    depth: Int,
    modifier: Modifier = Modifier,
    onClickFileNode: (file: FileNode) -> Unit,
    afterClickFileNode: () -> Unit,
    onLongClickNode: (FileNode) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(25.dp)
            .padding(start = (depth * 10).dp)
            .combinedClickable(
                onClick = { onClickFileNode(file); afterClickFileNode() },
                onLongClick = { onLongClickNode(file) }
            )
            .padding(vertical = 1.dp, horizontal = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = file.name,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 10.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp
        )

        // size >= 0 视为已知；未知大小（例如 -1）不显示
        if (file.resource.size >= 0) {
            Text(
                text = formatFileSize(file.resource.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
        else -> "${size / (1024 * 1024 * 1024)} GB"
    }
}

@Preview
@Composable
fun PreviewUiFileExplorer() {
    val applicationContext = LocalContext.current.applicationContext

    MainActivity.setFileManger(LocalFileManger(applicationContext))
    val previewData = remember {
        PreviewFileTreeViewModel(
            sessionStore = DataStoreSessionStore(applicationContext.dataStore)
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            UiFileTreeView(previewData)
        }
    }
}