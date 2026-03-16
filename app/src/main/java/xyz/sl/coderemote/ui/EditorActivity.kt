package xyz.sl.coderemote.ui

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.sl.coderemote.core.TextEditorControllerViewModel
import xyz.sl.coderemote.core.TextEditorControllerViewModelFactory

import xyz.sl.coderemote.ui.theme.TextEditorComposeTheme
import java.io.File

class EditorActivity : ComponentActivity() {
    private var savedText by mutableStateOf("")

    private var openFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val content = readTextFromUri(it)
            savedText = content
        }
    }

    private var saveFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            writeTextToUri(it, savedText)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TextEditorComposeTheme(
                useDarkTheme = true
            ) {
                UiEditor(
                    text = savedText,
                    onTextChange = { savedText = it },
//                    onNew = { savedText = "" },
//                    onOpen = { openFileLauncher.launch(arrayOf("*/*")) },
//                    onSave = { saveFileLauncher.launch("newfile.txt") },
                )
            }
        }
    }

    private fun readTextFromUri(uri: Uri): String {
        return try {
            contentResolver.openInputStream(uri)?.use { it.reader().readText() } ?: ""
        } catch (e: Exception) {
            Toast.makeText(this, "读取失败: ${e.message}", Toast.LENGTH_SHORT).show()
            ""
        }
    }

    private fun writeTextToUri(uri: Uri, text: String) {
        try {
            contentResolver.openOutputStream(uri)?.use {
                it.write(text.toByteArray())
                Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

data class OptionItem(val text: String, val action: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UiEditor(
    text: String,
    fileUri: Uri = Uri.EMPTY,
    onTextChange: (String) -> Unit,
    tasksData: List<OptionItem> = listOf(),
    menusData: List<OptionItem> = listOf(),
    modifier: Modifier = Modifier
) {
    val application : Application = LocalContext.current.applicationContext as Application
    var file = DocumentFile.fromSingleUri(application, fileUri)
        ?: DocumentFile.fromFile(File("UnknownFile"))

    val controllerViewModel : TextEditorControllerViewModel = viewModel(
        factory = TextEditorControllerViewModelFactory(text, application)
    )

    var textStyle = LocalTextStyle.current.copy(fontSize = 20.sp)
    var editorBackgroundModifier = Modifier.background(Color.LightGray)


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
                    text = file.name?: "UnknownFile",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(start = 5.dp)
                )
                UiTextAreaShow(
                    textEditorControllerViewModel = controllerViewModel,
                    textStyle = textStyle,
                    editorBackgroundModifier = editorBackgroundModifier
                )

            }
        },
    )
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
            text = "savedText",
            onTextChange = { "savedText = it" },
            tasksData = tasksData,
            menusData = menusData
        )
    }
}