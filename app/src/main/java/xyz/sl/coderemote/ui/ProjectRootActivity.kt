package xyz.sl.coderemote.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.sl.coderemote.MainActivity
import xyz.sl.coderemote.ui.dialog.DangerConfirmDialog
import xyz.sl.coderemote.ui.dialog.TextInputDialog
import xyz.sl.coderemote.core.FileNode

const val ProjectRootActivityTag : String = "ProjectRootActivity"

class ProjectRootActivity : ComponentActivity() {

    private var uri : Uri = Uri.EMPTY

    private val vm : ProjectRootViewModel by viewModels()
    private lateinit var fileTreeVM : FileTreeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uriStr = intent.getStringExtra("uri")
        uri =  Uri.parse(uriStr)

        // TODO: avoid uri is null in StartProjectActivity
        Log.i(ProjectRootActivityTag,"uri "+uri.toString())

        // 点击文件列表中的文件触发此函数
        //   如果是文件则更新 currentUri
        val onDrawerFileItemClick = { node: FileNode ->
            Log.i(ProjectRootActivityTag, "onDrawerFileItemClick()->${node.name}")
            var tmpNode = node
            var relativePath = "/"+tmpNode.name

            // 子文件
            while (tmpNode.parent != null){
                relativePath = "/"+tmpNode.parent?.name + relativePath
                tmpNode = tmpNode.parent as FileNode.Directory
            }

            // 更新当前显示的文件
            vm.updateCurrentUri(node.resource.uri)
        }

        val _isLoading = MutableStateFlow(true)
        val isLoading = _isLoading.asStateFlow()
        val _error = MutableStateFlow<Throwable?>(null)
        val error = _error.asStateFlow()
        setContent {
            val isLoading = isLoading.collectAsState()
            val error = error.collectAsState()

            if (error.value != null) {
                Toast.makeText(
                    this,
                    error.toString(),
                    Toast.LENGTH_LONG
                ).show()

                LaunchedEffect(error) {
                    Log.e(ProjectRootActivityTag, "解析uri失败", error.value)
                    startActivity(
                        Intent(this@ProjectRootActivity, StartProjectActivity::class.java)
                    )
                    finish()
                }

            } else if (isLoading.value) {
                // 显示加载中
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                UiProjectRoot(
                    fileTreeVM,
                    onDrawerFileItemClick = onDrawerFileItemClick,
                )
            }
        }

        // 加载文件树放到协程
        // uri= sftp://[auth]/C:/Users/jocker/Desktop
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resolved = MainActivity.resourceManager.resolve(uri)
                val vm = ViewModelProvider(
                    this@ProjectRootActivity,
                    FileTreeViewModelFactory(
                        MainActivity.fileManger,
                        resolved
                    )
                ).get(FileTreeViewModel::class.java)

                fileTreeVM = vm
                _isLoading.value = false
            } catch (e: IllegalArgumentException) {
                _error.value = e
            }
        }
    }
}

/**
 * 保存数据。ProjectRootActivity 重组时，不会刷新该 ViewModel
 */
class ProjectRootViewModel : ViewModel() {
    /**
     * Activity 可以直接修改 uri，Composable 也能修改 uri，并实时刷新 UI，不会出现
     * “UI 有自己一份，Activity 有自己一份，彼此不同步”的问题。
     *
     * 在 Compose 里，这个问题的标准解法就是：单一数据源（Single Source of Truth），即
     * 把 uri 统一放在一个 状态容器（mutableStateOf 或 ViewModel）里，Activity 和
     * Composable 都读写它。
     */

    /**
     * 正在打开的文件的Uri
     */
    var currentUri by mutableStateOf(Uri.EMPTY)
        private set

    fun updateCurrentUri(newUri: Uri) {
        currentUri = newUri
        if (!recentUri.contains(currentUri)) {
            addRecentFile(currentUri)
        }
        Log.d(ProjectRootActivityTag, "currentUri: $currentUri")
    }

    // 最近打开过的文件 Uri
    var recentUri = mutableStateListOf<Uri>()

    fun addRecentFile(uri: Uri){
        recentUri.add(uri)
    }

    fun delRecentFile(uri: Uri){
        // 如果 uri 正在打开，则先关闭
        if (currentUri.equals(uri)) {

        }

        // 将 uri 资源文件保存回原始文件
        {  } /* TODO */

        // 从最近文件列表中移除 uri
        recentUri.removeIf { it.equals(uri) }
        currentUri = recentUri.getOrElse(0, { Uri.EMPTY })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UiProjectRoot(
    fileTreeViewModel: FileTreeViewModel,
    onDrawerFileItemClick: (file: FileNode) -> Unit = {},
    vm: ProjectRootViewModel = viewModel()
) {
    val expandDrawer = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var tasksData = List(100) { it ->
        OptionItem("Task $it") {}
    }
    var menusData = listOf(
        OptionItem("新建") {},
        OptionItem("保存") {},
        OptionItem("打开") {}
    )

    // 记住 UiEditor 中保存文件的函数
    var saveCurrentFile by remember { mutableStateOf<(() -> Unit)?>(null) }

    // 长按目录或文件，弹出菜单选项
    var selectedNode by remember { mutableStateOf<FileNode?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var onLongClick = fun (fileNode: FileNode){ selectedNode = fileNode }

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Text("Code Remote", modifier = Modifier.padding(16.dp), fontSize = 26.sp)
                HorizontalDivider()

                Text("Recent", Modifier.padding(5.dp))
                UiRecentFileExplorer(
                    vm.recentUri,
                    onCloseFileItem = { uri: Uri ->
                        // 如果当前编辑的文件是要移除的文件，先保存
                        //    UiEditor 监听 currentUri 变化，会自动保存文件。这里会重复保存
//                        if (vm.currentUri == uri) {
//                            saveCurrentFile?.invoke()
//                        }

                        // 移除文件
                        vm.delRecentFile(uri)
                    },
                    onClickFileItem = { uri: Uri -> vm.updateCurrentUri(uri) }
                )

                Text("Project", Modifier.padding(5.dp))
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        UiFileTreeView(
                            fileTreeViewModel,
                            onClickFileNode = onDrawerFileItemClick,
                            afterClickFileNode = {
                                // TODO: 点击file item之后，不能成功收起侧边栏
                                scope.launch {
                                    expandDrawer.close()
                                }
                            },
                            onLongClickNode = onLongClick
                        )
                    }
                }

            }
        },
        drawerState = expandDrawer,
    ) {
        Column (modifier = Modifier.fillMaxSize()) {
            UiEditor(
                fileUri = vm.currentUri,
                tasksData = tasksData,
                menusData = menusData,
                onTextChange = {},
                modifier = Modifier.weight(1f),
                onSaveReady = { saveFn ->
                    saveCurrentFile = saveFn
                }
            )

            val tabs = listOf("输出", "错误", "日志", "tty1","tty2","tty3","tty4","tttty5","tty6","tty7","tty8")
            val contents = listOf(
                "程序执行成功。\n结果：42\n结果：42\n结果：42\n结果：42\n结果：42\n\n\n\n\n\n\n\n\n\n\n结果：42\n结果：42\n结果：42\n结果：42\n结果：424",
                "错误：变量未定义。\n第5行：x = y + 1",
                "日志：程序启动于 12:00\n日志：已加载配置文件",
                "1程序执行成功。\n结果：42",
                "2错误：变量未定义。\n第5行：x = y + 1",
                "3日志：程序启动于 12:00\n日志：已加载配置文件",
                "4程序执行成功。\n结果：42",
                "5错误：变量未定义。\n第5行：x = y + 1",
                "6日志：程序启动于 12:00\n日志：已加载配置文件",
                "7错误：变量未定义。\n第5行：x = y + 1",
                "8日志：程序启动于 12:00\n日志：已加载配置文件"
            )

            UiOutputPanel(tabTitles = tabs, tabContents = contents,
                modifier = Modifier
                    .fillMaxWidth()
//                .align(Alignment.BottomCenter)
                .zIndex(1f) // 保证浮在上面
            )
        }
        IconButton(
            onClick = {
                Log.i("ProjectRootActivity", "expendDrawer = $expandDrawer")
                scope.launch {
                    expandDrawer.apply {
                        if (isClosed) open() else close()
                    }
                }
            },
            modifier = Modifier.padding(horizontal = 0.dp, vertical = 10.dp)
        ) {
            Icon(Icons.Default.Menu, contentDescription = "menu")
        }
    }


    // 各个对话框的显示状态
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showNewDirDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // 抽屉效果的菜单选项
    if (selectedNode != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedNode = null },
            sheetState = sheetState
        ) {
            when(val node = selectedNode) {

                is FileNode.File -> {
                    Text("  文件：${node.name}")
                    ListItem(
                        headlineContent = { Text("重命名") },
                        modifier = Modifier.clickable {
                            showRenameDialog = !showRenameDialog
                        }
                    )
                    ListItem(
                        headlineContent = { Text("删除") },
                        modifier = Modifier.clickable {
                            showDeleteConfirmDialog = !showDeleteConfirmDialog
                        }
                    )
                }

                is FileNode.Directory -> {
                    Text("目录：${node.name}")
                    ListItem(
                        headlineContent = { Text("新建文件") },
                        modifier = Modifier.clickable {
                            showNewFileDialog = !showNewFileDialog
                        }
                    )
                    ListItem(
                        headlineContent = { Text("新建目录") },
                        modifier = Modifier.clickable {
                            showNewDirDialog = !showNewDirDialog
                        }
                    )
                    ListItem(
                        headlineContent = { Text("重命名") },
                        modifier = Modifier.clickable {
                            showRenameDialog = !showRenameDialog
                        }
                    )
                    ListItem(
                        headlineContent = { Text("删除目录") },
                        modifier = Modifier.clickable {
                            showDeleteConfirmDialog = !showDeleteConfirmDialog
                        }
                    )
                }
                null -> {}

            }
            Spacer(Modifier.height(30.dp))
        }
    }

    // ========== 对话框区域（放在 ModalBottomSheet 外面） ==========

//    var selectedNode

    // 重命名对话框
    if (showRenameDialog && selectedNode != null) {
        TextInputDialog(
            title = "重命名",
            initialValue = selectedNode?.name ?: "",
            label = "新名称",
            confirmText = "重命名",
            onDismiss = {
                showRenameDialog = false
                selectedNode = null
            },
            onConfirm = { newName ->
                try {
                    when (val node = selectedNode) {
                        is FileNode.File -> {
                            fileTreeViewModel.renameFile(node.resource, newName)
                        }
                        is FileNode.Directory -> {
                            fileTreeViewModel.renameFile(node.resource, newName)
                        }
                        else -> {}
                    }
                    showRenameDialog = false
                    selectedNode = null
                } catch (e: Exception) {
                    // 处理错误
                }
            }
        )
    }

    // 新建文件对话框
    if (showNewFileDialog && selectedNode is FileNode.Directory) {
        TextInputDialog(
            title = "新建文件",
            initialValue = "",
            label = "文件名",
            placeholder = "请输入文件名",
            confirmText = "创建",
            onDismiss = {
                showNewFileDialog = false
                selectedNode = null
            },
            onConfirm = { fileName ->
                try {
                    val dir = selectedNode as FileNode.Directory
                    fileTreeViewModel.createFile(dir.resource, fileName)
                    showNewFileDialog = false
                    selectedNode = null
                } catch (e: Exception) {
                    Log.e(ProjectRootActivityTag, e.message, e)
                }
            }
        )
    }

    // 新建目录对话框
    if (showNewDirDialog && selectedNode is FileNode.Directory) {
        TextInputDialog(
            title = "新建目录",
            initialValue = "",
            label = "目录名",
            placeholder = "请输入目录名",
            confirmText = "创建",
            onDismiss = {
                showNewDirDialog = false
                selectedNode = null
            },
            onConfirm = { dirName ->
                try {
                    val dir = selectedNode as FileNode.Directory
                    fileTreeViewModel.createDirectory(dir.resource, dirName)
                    showNewDirDialog = false
                    selectedNode = null
                } catch (e: Exception) {
                    // 处理错误
                    Log.e(ProjectRootActivityTag, e.message, e)
                }
            }
        )
    }

    // 删除确认对话框
    if (showDeleteConfirmDialog && selectedNode != null) {
        DangerConfirmDialog(
            title = "确认删除",
            message = when (selectedNode) {
                is FileNode.File -> "确定要删除文件 \"${selectedNode?.name}\" 吗？\n此操作不可恢复。"
                is FileNode.Directory -> "确定要删除目录 \"${selectedNode?.name}\" 及其所有内容吗？\n此操作不可恢复。"
                else -> ""
            },
            confirmText = "删除",
            onDismiss = {
                showDeleteConfirmDialog = false
                selectedNode = null
            },
            onConfirm = {
                try {
                    when (val node = selectedNode) {
                        is FileNode.File -> {
                            fileTreeViewModel.deleteNode(node.resource, false)
                        }
                        is FileNode.Directory -> {
                            fileTreeViewModel.deleteNode(node.resource, true)
                        }
                        else -> {}
                    }
                    showDeleteConfirmDialog = false
                    selectedNode = null
                } catch (e: Exception) {
                    // 处理错误
                }
            }
        )
    }

}

@Preview(backgroundColor = 0x888888)
@Composable
fun PreviewUiProjectRoot() {
    MainActivity.setEditFileMangerForUiPreview(LocalContext.current)

    val sampleData : List<FileNode> = listOf(sampleFiles())
    val previewData = remember { PreviewFileTreeViewModel(sampleData) }

    UiProjectRoot(previewData)
}