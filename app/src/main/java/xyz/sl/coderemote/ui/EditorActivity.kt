package xyz.sl.coderemote.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.sl.coderemote.MainActivity
import xyz.sl.coderemote.ui.text.TextEditorControllerViewModel
import xyz.sl.coderemote.ui.text.TextEditorControllerViewModelFactory
import xyz.sl.coderemote.ui.text.UiTextAreaShow

import xyz.sl.coderemote.ui.theme.TextEditorComposeTheme
import java.io.File
import java.io.IOException

val Tag = "CR-Ui-Editor"

class EditorActivity : ComponentActivity() {
    private var savedText by mutableStateOf("")

//    private var openFileLauncher = registerForActivityResult(
//        ActivityResultContracts.OpenDocument()
//    ) { uri: Uri? ->
//        uri?.let {
//            val content = readTextFromUri(it)
//            savedText = content
//        }
//    }

//    private var saveFileLauncher = registerForActivityResult(
//        ActivityResultContracts.CreateDocument("text/plain")
//    ) { uri: Uri? ->
//        uri?.let {
//            writeTextToUri(it, savedText)
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TextEditorComposeTheme(
                useDarkTheme = true
            ) {
                UiEditor(
                    onTextChange = { savedText = it },
//                    onNew = { savedText = "" },
//                    onOpen = { openFileLauncher.launch(arrayOf("*/*")) },
//                    onSave = { saveFileLauncher.launch("newfile.txt") },
                )
            }
        }
    }

//    private fun readTextFromUri(uri: Uri): String {
//        return try {
//            contentResolver.openInputStream(uri)?.use { it.reader().readText() } ?: ""
//        } catch (e: Exception) {
//            Toast.makeText(this, "读取失败: ${e.message}", Toast.LENGTH_SHORT).show()
//            ""
//        }
//    }
//
//    private fun writeTextToUri(uri: Uri, text: String) {
//        try {
//            contentResolver.openOutputStream(uri)?.use {
//                it.write(text.toByteArray())
//                Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
//            }
//        } catch (e: Exception) {
//            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
//        }
//    }
}

data class OptionItem(val text: String, val action: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UiEditor(
    fileUri: Uri = Uri.EMPTY,
    onTextChange: (String) -> Unit,
    tasksData: List<OptionItem> = listOf(),
    menusData: List<OptionItem> = listOf(),
    modifier: Modifier = Modifier,
    onSaveReady: ((()->Unit)?) -> Unit = {}
) {
//    var file = DocumentFile.fromSingleUri(LocalContext.current.applicationContext, fileUri)
//        ?: DocumentFile.fromFile(File("UnknownFile"))

    var textStyle = LocalTextStyle.current.copy(fontSize = 20.sp)

    var isOpeningFile : Boolean = remember( fileUri ) { !Uri.EMPTY.equals(fileUri) }
    var fileName : String = MainActivity.fileManger.getFileNameFromUri(fileUri)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "文本编辑器",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(start = 30.dp)
                    )},
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "撤销")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "撤回“撤销”")
                    }
                    UiTaskMenuButton(tasksData)
                    UiMoreMenuButton(menusData)
                },
            )
        },
        content = { innerPadding ->
            Column {
                // 当前文件名标注
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(start = 5.dp)
                )

                if (!isOpeningFile) {
                    Text("未打开文件")
                } else {
                    // TODO : 当前：文件打开异常会将异常信息显示到编辑区
                    //        应该：文件打开异常，捕捉异常然后使用Text等组件展示
                    val content = remember(fileUri) {
                        MainActivity.fileManger.readTextFromUri(fileUri)
                    }
                    val controllerViewModel: TextEditorControllerViewModel = viewModel(
                        key = fileUri.toString(),  // 绑定到 fileUri
                        factory = TextEditorControllerViewModelFactory(content)
                    )

                    var saveFunction by remember { mutableStateOf<(() -> Unit)?>(null) }

                    // 保存当前文件内容的函数
                    val saveCurrentFile: () -> Unit = {
                        try {
                            val currentText = controllerViewModel.controller.text.toString()
                            MainActivity.fileManger.writeTextToUri(fileUri, currentText)
                            Log.i("UiEditor", "File saved: $fileUri")
                        } catch (e: IOException) {
                            Log.e("UiEditor", "Save failed", e)
                        }
                    }

                    // 当 fileUri 变化时更新保存函数
                    LaunchedEffect(fileUri) {
                        saveFunction = saveCurrentFile
                        onSaveReady(saveFunction)
                    }

                    // 组件销毁时自动保存
                    DisposableEffect(fileUri) {
                        onDispose {
                            // 如果需要在关闭时自动保存，可以在这里调用
                             saveFunction?.invoke()
                        }
                    }

                    UiTextAreaShow(
                        textEditorControllerViewModel = controllerViewModel,
                        textStyle = textStyle,
                        editorBackgroundColor = Color.LightGray
                    )
                }

            }
        },
    )

    // 当组件从组合中移除时清理引用
    DisposableEffect(Unit) {
        onDispose {
            onSaveReady(null)
        }
    }
}


@Composable
fun UiTaskMenuButton(
    tasksData: List<OptionItem>
){
    var expandedTasks by remember { mutableStateOf(false) }
    Box() {
        TextButton(onClick = {expandedTasks = !expandedTasks}) {
            Text("Task")
        }
        DropdownMenu(
            expanded = expandedTasks,
            onDismissRequest = { expandedTasks = false },
            modifier = Modifier.height(500.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Option 1") },
                onClick = { /* Do something... */ }
            )
            DropdownMenuItem(
                text = { Text("Option 2") },
                onClick = { /* Do something... */ }
            )
            HorizontalDivider()
            tasksData.forEach { taskItem ->
                DropdownMenuItem(
                    text = { Text(taskItem.text) },
                    onClick = taskItem.action
                )
            }
        }
    }
}

@Composable
fun UiMoreMenuButton(
    moreMenusData: List<OptionItem>
){
    var expandedMenus by remember { mutableStateOf(false) }
    Box() {
        IconButton(onClick = {expandedMenus = !expandedMenus}) {
            Icon(Icons.Default.MoreVert, contentDescription = "Task")
        }
        DropdownMenu(
            expanded = expandedMenus,
            onDismissRequest = { expandedMenus = false },
        ) {
            moreMenusData.forEach { moreMenuItem ->
                DropdownMenuItem(
                    text = { Text(moreMenuItem.text) },
                    onClick = moreMenuItem.action
                )
            }

        }
    }
}

@Preview(backgroundColor = 0xff8800L)
@Composable
fun PreviewUiEditor() {
    MainActivity.setEditFileMangerForUiPreview(LocalContext.current)

    var tasksData = List(100) { it ->
        OptionItem("Task $it") {}
    }
    var menusData = listOf(
        OptionItem("新建") {},
        OptionItem("保存") {},
        OptionItem("打开") {}
    )

    TextEditorComposeTheme(
        useDarkTheme = false
    ) {
        UiEditor(
            onTextChange = { "savedText = it" },
            tasksData = tasksData,
            menusData = menusData,
            fileUri = Uri.fromFile(File("abc"))
        )
    }
}