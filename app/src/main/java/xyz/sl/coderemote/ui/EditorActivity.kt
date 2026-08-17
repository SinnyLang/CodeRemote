package xyz.sl.coderemote.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.sl.coderemote.MainActivity
import xyz.sl.coderemote.MainActivity.Companion.resourceManager
import xyz.sl.coderemote.core.vfs.io.TextReader
import xyz.sl.coderemote.core.vfs.io.TextWriter
import xyz.sl.coderemote.ui.text.TextEditorControllerViewModel
import xyz.sl.coderemote.ui.text.TextEditorControllerViewModelFactory
import xyz.sl.coderemote.ui.text.UiTextAreaShow

import xyz.sl.coderemote.ui.theme.TextEditorComposeTheme
import java.io.File

const val TAG = "CR-Ui-Editor"

class EditorActivity : ComponentActivity() {
    private var savedText by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TextEditorComposeTheme(
                useDarkTheme = true
            ) {
                UiEditor(
                    onTextChange = { savedText = it },
                )
            }
        }
    }
}

data class OptionItem(val text: String, val action: () -> Unit)

// 加载状态
sealed class LoadState {
    object Idle : LoadState()
    object Loading : LoadState()
    data class Success(val content: String) : LoadState()
    data class Error(val message: String) : LoadState()
}

/**
 * 绘制编辑界面，包含编辑区和一些控制按钮、控制菜单和任务菜单。
 * @param fileUri 当前编辑的文件的 Uri
 * @param onTextChange 当文本变化时调用
 * @param tasksData 任务菜单
 * @param menusData 选项菜单
 * @param modifier
 * @param onSaveReady 将保存当前文件的函数传递到父组件，用于父组件主动保存当前文件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UiEditor(
    fileUri: Uri = Uri.EMPTY,
    onTextChange: (String) -> Unit = {},
    tasksData: List<OptionItem> = listOf(),
    menusData: List<OptionItem> = listOf(),
    modifier: Modifier = Modifier,
    onSaveReady: ((() -> Unit)?) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val textStyle = LocalTextStyle.current.copy(fontSize = 20.sp)

    // ---------- 内部状态 ----------
    // 当前已加载并正在编辑的文件 URI（独立于外部传入的 fileUri）
    var currentFileUri by remember { mutableStateOf<Uri>(Uri.EMPTY) }
    var currentViewModel by remember { mutableStateOf<TextEditorControllerViewModel?>(null) }
    var loadState by remember { mutableStateOf<LoadState>(LoadState.Idle) }
    var loadJob by remember { mutableStateOf<Job?>(null) }

    // 文件名（基于 currentFileUri）
    val fileName = remember(currentFileUri) {
        if (currentFileUri != Uri.EMPTY) getFileNameFromUri(currentFileUri) else "未打开"
    }

    // ---------- 保存函数（参数化，依赖内部状态） ----------
    suspend fun saveCurrent(uri: Uri, vm: TextEditorControllerViewModel) {
        if (uri == Uri.EMPTY) return
        val text = vm.controller.text.toString()
        try {
            withContext(Dispatchers.IO) {
                val resource = resourceManager.resolve(uri)
                TextWriter().write(resource, text)
            }
            Log.i(TAG, "File saved: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "Save failed: $uri", e)
        }
    }

    // 非挂起版本供父组件调用（保存当前文件）
    val saveBlock: () -> Unit = {
        val vm = currentViewModel
        val uri = currentFileUri
        if (vm != null && uri != Uri.EMPTY) {
            scope.launch {
                saveCurrent(uri, vm)
            }
        }
    }

    LaunchedEffect(saveBlock) {
        onSaveReady(saveBlock)
    }

    // ---------- 处理外部 fileUri 变化（切换文件） ----------
    LaunchedEffect(fileUri) {
        // 外部请求关闭所有文件
        if (fileUri == Uri.EMPTY) {
            // 如果当前有文件，先保存
            if (currentViewModel != null && currentFileUri != Uri.EMPTY) {
                saveCurrent(currentFileUri, currentViewModel!!)
            }
            // 重置状态
            loadJob?.cancel()
            loadJob = null
            currentFileUri = Uri.EMPTY
            currentViewModel = null
            loadState = LoadState.Idle
            return@LaunchedEffect
        }

        // 如果请求的文件与当前文件相同，不做任何事
        if (fileUri == currentFileUri && currentViewModel != null) {
            return@LaunchedEffect
        }

        // --- 切换文件 ---
        // 1. 保存当前文件（如果有）
        if (currentViewModel != null && currentFileUri != Uri.EMPTY) {
            saveCurrent(currentFileUri, currentViewModel!!)
        }

        // 2. 取消旧加载任务
        loadJob?.cancel()
        loadJob = null

        // 3. 开始加载新文件
        currentFileUri = fileUri  // 更新当前 URI
        currentViewModel = null
        loadState = LoadState.Loading

        loadJob = scope.launch(Dispatchers.IO) {
            try {
                val resource = resourceManager.resolve(fileUri)
                ensureActive()
                val text = TextReader().read(resource)
                ensureActive()

                withContext(Dispatchers.Main) {
                    if (!isActive) return@withContext
                    // ★ 关键：不再在协程中创建 ViewModel，只更新 loadState
                    loadState = LoadState.Success(text)
                    onTextChange(text)
                }
            } catch (e: CancellationException) {
                // 任务取消，忽略
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isActive) return@withContext
                    loadState = LoadState.Error(e.message ?: "加载文件失败")
                    currentFileUri = Uri.EMPTY // 加载失败，清空当前 URI
                }
            }
        }
    }

    // ---------- 组件销毁时保存 ----------
    DisposableEffect(Unit) {
        onDispose {
            loadJob?.cancel()
            if (currentViewModel != null && currentFileUri != Uri.EMPTY) {
                // 尽力保存，使用 GlobalScope
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val text = currentViewModel!!.controller.text.toString()
                        val resource = resourceManager.resolve(currentFileUri)
                        TextWriter().write(resource, text)
                        Log.i(TAG, "Auto-save on dispose: $currentFileUri")
                    } catch (e: Exception) {
                        Log.e(TAG, "Auto-save on dispose failed", e)
                    }
                }
            }
            onSaveReady(null)
        }
    }

    // ---------- UI 渲染 ----------
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "文本编辑器",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(start = 30.dp)
                    )
                },
                actions = {
                    IconButton(onClick = { /* 撤销 */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "撤销")
                    }
                    IconButton(onClick = { /* 重做 */ }) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "撤回“撤销”")
                    }
                    UiTaskMenuButton(tasksData)
                    UiMoreMenuButton(menusData)
                }
            )
        },
        content = { innerPadding ->
            Column {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(start = 5.dp)
                )

                when (val state = loadState) {
                    is LoadState.Idle -> {
                        Text("未打开文件")
                    }
                    is LoadState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                            Text("加载中...", modifier = Modifier.padding(top = 60.dp))
                        }
                    }
                    is LoadState.Error -> {
                        Text(
                            text = "加载失败: ${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    is LoadState.Success -> {
                        // ★ 在 Composable 上下文中创建 ViewModel
                        val vm: TextEditorControllerViewModel = viewModel(
                            key = currentFileUri.toString(),
                            factory = TextEditorControllerViewModelFactory(state.content)
                        )
                        // 保存 ViewModel 引用，供保存函数使用
                        currentViewModel = vm

                        UiTextAreaShow(
                            textEditorControllerViewModel = vm,
                            textStyle = textStyle,
                            editorBackgroundColor = Color.LightGray
                        )
                    }
                }
            }
        }
    )
}

// 辅助函数（根据实际实现替换）
fun getFileNameFromUri(uri: Uri): String = uri.lastPathSegment ?: "未命名"

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
    MainActivity.setResourceManagerForUiPreview(LocalContext.current)

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