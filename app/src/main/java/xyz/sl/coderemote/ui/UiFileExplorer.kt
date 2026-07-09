package xyz.sl.coderemote.ui

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

sealed class FileNode(var name: String, var parent: Directory?, var uri: Uri) {
    class File(name: String, parent: Directory?, uri: Uri) : FileNode(name, parent, uri)
    class Directory(
        name: String,
        parent: Directory?,
        uri: Uri,
        var children: List<FileNode>,
        var isExpanded: MutableState<Boolean> = mutableStateOf(false)
    ) : FileNode(name, parent, uri)
}

fun doNothing(){}

@Composable
fun UiFileTreeView(
    nodes: List<FileNode>,
    indent: Int = 0,
    onFileClick: (file: FileNode) -> Unit = {},
    afterFileClick: ()->Unit = {},
    onLongClick: (FileNode) -> Unit = {}
) {
    Column (
        modifier = Modifier.fillMaxWidth()
    ) {
        nodes.forEach { node ->
            when (node) {
                is FileNode.File ->
                    UiFileExplorerItemOfFile(node, indent, onFileClick, afterFileClick, onLongClick)

                is FileNode.Directory -> {
                    UiFileExplorerItemOfDirectory(node, indent, onLongClick = onLongClick)
                    if (node.isExpanded.value) {
                        UiFileTreeView(
                            node.children,
                            indent + 1,
                            onFileClick,
                            afterFileClick,
                            onLongClick
                        )
                    }
                }
            }
        }
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
        Text(file.name, Modifier.fillMaxHeight().paddingFromBaseline(15.dp))
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
                    dir.isExpanded.value = !dir.isExpanded.value
                    Log.i("UiFileExplorer", "expand dir " + dir.name)
                },
                onLongClick = {
                    onLongClick(dir)
                }
            )
    ) {
        val icon = if (dir.isExpanded.value) Icons.Default.ExpandMore else Icons.Default.ChevronRight
        Icon(imageVector = icon, contentDescription = "expend/collapse")
//        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.Default.Folder, contentDescription = "folder")
        Spacer(modifier = Modifier.width(4.dp))
        Text(dir.name, Modifier.fillMaxHeight().paddingFromBaseline(15.dp))
    }
}

fun sampleFiles(): FileNode.Directory {
    val uri = Uri.EMPTY
    val src = FileNode.Directory("src", null, uri,listOf())
    val readme = FileNode.File("README.md", null, uri)
    val main = FileNode.Directory("main", src, uri, listOf())
    val test = FileNode.Directory("test", src, uri, listOf())
    val MainActivity = FileNode.File("MainActivity.kt", main, uri)
    val Utils = FileNode.File("Utils.kt", main, uri)
    val MainActivityTest = FileNode.File("MainActivityTest.kt", test, uri)
    src.children = listOf(main, test)
    main.children = listOf(MainActivity, Utils)
    test.children = listOf(MainActivityTest)
    return src
}

@Preview
@Composable
fun PreviewUiFileExplorer() {


    val sampleData = listOf(
        sampleFiles()
    )

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            UiFileTreeView(sampleData)
        }
    }
}