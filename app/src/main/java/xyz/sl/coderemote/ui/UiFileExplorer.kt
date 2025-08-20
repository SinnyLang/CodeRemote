package xyz.sl.coderemote.ui

import android.util.Log
import androidx.compose.foundation.clickable
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

sealed class FileNode(var name: String, var parent: Directory?) {
    class File(name: String, parent: Directory?) : FileNode(name, parent)
    class Directory(
        name: String,
        parent: Directory?,
        var children: List<FileNode>,
        var isExpanded: MutableState<Boolean> = mutableStateOf(false)
    ) : FileNode(name, parent)
}

fun doNothing(){}

@Composable
fun UiFileTreeView(
    nodes: List<FileNode>,
    indent: Int = 0,
    onFileClick: (file: FileNode) -> Unit = {},
    afterFileClick: ()->Unit = {}
) {
    Column (
        modifier = Modifier.fillMaxWidth()
    ) {
        nodes.forEach { node ->
            when (node) {
                is FileNode.File -> UiFileExplorerItemOfFile(node, indent, onFileClick, afterFileClick)
                is FileNode.Directory -> {
                    UiFileExplorerItemOfDirectory(node, indent)
                    if (node.isExpanded.value) {
                        UiFileTreeView(node.children, indent + 1, onFileClick)
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
    afterFileClick: () -> Unit = {}
){
    Row (
        modifier = Modifier
            .padding(horizontal = (indent * 16).dp, vertical = 4.dp)
            .fillMaxSize()
            .height(20.dp)
            .clickable {
                onFileClick(file)
                afterFileClick()
            }
    ) {
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Default.InsertDriveFile, contentDescription = "")
        Spacer(modifier = Modifier.width(4.dp))
        Text(file.name, Modifier.fillMaxHeight().paddingFromBaseline(15.dp))
    }
}

@Composable
fun UiFileExplorerItemOfDirectory(dir: FileNode.Directory, indent: Int){
    Row (
        modifier = Modifier
            .padding(horizontal = (indent * 16).dp, vertical = 4.dp)
            .fillMaxWidth()
            .height(20.dp)
            .clickable {
                dir.isExpanded.value = !dir.isExpanded.value
                Log.i("UiFileExplorer", "expand dir " + dir.name)
            }
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
    val src = FileNode.Directory("src", null, listOf())
    val readme = FileNode.File("README.md", null)
    val main = FileNode.Directory("main", src, listOf())
    val test = FileNode.Directory("test", src, listOf())
    val MainActivity = FileNode.File("MainActivity.kt", main)
    val Utils = FileNode.File("Utils.kt", main)
    val MainActivityTest = FileNode.File("MainActivityTest.kt", test)
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