package xyz.sl.coderemote.ui

import android.util.Log
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import xyz.sl.coderemote.MainActivity
import xyz.sl.coderemote.core.FileNode
import xyz.sl.coderemote.utils.LocalFileManger
import kotlin.collections.listOf

fun doNothing(){}

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


    // rootNodes 是一个根节点列表，用于处理多个根目录情况
    val rootNodes by fileTreeViewModel.rootNode.collectAsState()
    val isLoading by fileTreeViewModel.isLoading.collectAsState()
    val error by fileTreeViewModel.error.collectAsState()

    val modifier = Modifier

    when {
        // 初始化文件树--显示加载中
        isLoading && rootNodes.isEmpty() -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        // 文件数加载失败--显示重试
        error != null && rootNodes.isEmpty() -> {
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("加载失败: $error", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { fileTreeViewModel.refresh() }) {
                    Text("重试")
                }
            }
        }
        // 文件树成功加载--渲染文件树
        rootNodes.isNotEmpty() -> {
            FileTreeNodeView(
                nodes = rootNodes,
                viewModel = fileTreeViewModel,
                modifier = modifier,
                depth = 0,
                onClickFileNode = onClickFileNode,
                afterClickFileNode = afterClickFileNode,
                onLongClickNode = onLongClickNode
            )
        }
    }

//    Column (
//        modifier = Modifier.fillMaxWidth()
//    ) {
//        rootNodes.forEach { node ->
//            when (node) {
//                is FileNode.File ->
//                    UiFileExplorerItemOfFile(node, indent, onClickFileNode, afterClickFileNode, onLongClickNode)
//
//                is FileNode.Directory -> {
//                    UiFileExplorerItemOfDirectory(node, indent, onLongClick = onLongClickNode)
//                    if (node.isExpanded.collectAsState().value) {
//                        UiFileTreeView(
////                            node.getChildren(),
//                            listOf(),
//                            indent + 1,
//                            onFileClick,
//                            afterFileClick,
//                            onLongClick
//                        )
//                    }
//                }
//            }
//        }
//    }

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
    val isExpanded = directory.isExpanded.collectAsState()
    val isLoading = directory.isLoading.collectAsState()
    val children = if (isExpanded.value) directory.getCachedChildren() else emptyList()

    Column(modifier = modifier) {
        // 目录项
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
                if (isExpanded.value)
                    Icons.Default.FolderOpen
                else
                    Icons.Default.Folder,
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
            if (isLoading.value) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        // 子节点
        if (isExpanded.value) {
            if (isLoading.value && children.isEmpty()) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(start = (depth + 1) * 16.dp)
//                        .padding(8.dp),
//                    contentAlignment = Alignment.Center
//                ) {
//                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
//                }
            } else if (children.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (depth + 1) * 10.dp)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "空目录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                FileTreeNodeView(
                    nodes = children,
                    viewModel = viewModel,
                    depth = depth + 1,
                    onClickFileNode = onClickFileNode,
                    afterClickFileNode = afterClickFileNode,
                    onLongClickNode = onLongClickNode
                )
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
                onLongClick = {
                    onLongClickNode(file)
                },
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
        if (file.resource.size > 0) {
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

@Composable
fun UiFileExplorerItemOfFile(
    file: FileNode,
    indent: Int,
    onFileClick: (file: FileNode) -> Unit,
    afterFileClick: () -> Unit = {},
    onLongClick: (FileNode) -> Unit = {}
){
    Row (
        modifier = Modifier
            .padding(horizontal = (indent * 16).dp, vertical = 4.dp)
            .fillMaxSize()
            .height(20.dp)
            .combinedClickable(
                onClick = {
                    onFileClick(file)
                    afterFileClick()
                },
                onLongClick = {
                    onLongClick(file)
                }
            )
    ) {
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Default.InsertDriveFile, contentDescription = "")
        Spacer(modifier = Modifier.width(4.dp))
        Text(file.name, Modifier
            .fillMaxHeight()
            .paddingFromBaseline(15.dp))
    }
}

@Composable
fun UiFileExplorerItemOfDirectory(
    dir: FileNode.Directory,
    indent: Int,
    onLongClick: (FileNode) -> Unit = {}
){
    Row (
        modifier = Modifier
            .padding(horizontal = (indent * 16).dp, vertical = 4.dp)
            .fillMaxWidth()
            .height(20.dp)
            .combinedClickable(
                onClick = {
                    dir.toggleExpanded()
                    Log.i("UiFileExplorer", "expand dir " + dir.name)
                },
                onLongClick = {
                    onLongClick(dir)
                }
            )
    ) {
        val icon =
            if (dir.isExpanded.collectAsState().value)
                Icons.Default.ExpandMore
            else
                Icons.Default.ChevronRight
        Icon(imageVector = icon, contentDescription = "expend/collapse")
        Icon(Icons.Default.Folder, contentDescription = "folder")
        Spacer(modifier = Modifier.width(4.dp))
        Text(dir.name, Modifier
            .fillMaxHeight()
            .paddingFromBaseline(15.dp))
    }
}

@Preview
@Composable
fun PreviewUiFileExplorer() {


    val sampleData = listOf(
        sampleFiles()
    )

    MainActivity.setFileManger(LocalFileManger(LocalContext.current.applicationContext))
    val previewData = remember { PreviewFileTreeViewModel(sampleData) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            UiFileTreeView(previewData)
        }
    }
}