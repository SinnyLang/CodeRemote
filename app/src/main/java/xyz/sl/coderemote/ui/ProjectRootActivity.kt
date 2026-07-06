package xyz.sl.coderemote.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import xyz.sl.coderemote.MainActivity
import xyz.sl.coderemote.utils.UriUtils.findFileUri
import xyz.sl.coderemote.utils.UriUtils.uriToFileNode

var debugTag : String = "ProjectRootActivity"

class ProjectRootActivity : ComponentActivity() {

    private var projectFileRoot: List<FileNode> = listOf()
    private var uri : Uri = Uri.EMPTY

    private val vm : ProjectRootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uriStr = intent.getStringExtra("uri")
        uri =  Uri.parse(uriStr)
        try {
            projectFileRoot = listOf(
                uriToFileNode(this, uri)
            )
        } catch (e: IllegalArgumentException) {
            Log.e(debugTag, "解析uri失败 返回null", e)
            Toast.makeText(this, "目录或文件不存在", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, StartProjectActivity::class.java))
            finish()
        }

        // TODO: avoid uri is null in StartProjectActivity
        Log.i(debugTag,"uri"+uri.toString())

        // 点击文件列表中的文件触发此函数
        //   如果是文件则更新 currentUri
        val onDrawerFileItemClick = { node: FileNode ->
            Log.i(debugTag, "onDrawerFileItemClick()->${node.name}")
            var tmpNode = node
            var relativePath = "/"+tmpNode.name

            // 子文件
            while (tmpNode.parent != null){
                relativePath = "/"+tmpNode.parent?.name + relativePath
                tmpNode = tmpNode.parent as FileNode.Directory
            }

            // 更新当前显示的文件
            vm.updateCurrentUri( findFileUri(this, uri, relativePath) ?: Uri.EMPTY )
        }

        setContent {
            UiProjectRoot(
                projectFileRoot,
                onDrawerFileItemClick = onDrawerFileItemClick,
            )
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
        Log.d(Tag, "currentUri: $currentUri")
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

@Composable
fun UiProjectRoot(
    projectFileRoot: List<FileNode> = listOf(),
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

//    val imeBottomDp = with(LocalDensity.current) { WindowInsets.ime.getBottom(this).toDp() }

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
                    onCloseFileItem = { uri: Uri -> vm.delRecentFile(uri) },
                    onClickFileItem = { uri: Uri -> vm.updateCurrentUri(uri) }
                )

                Text("Project", Modifier.padding(5.dp))
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        UiFileTreeView(
                            projectFileRoot,
                            onFileClick = onDrawerFileItemClick,
                            afterFileClick = {
                                // TODO: 点击file item之后，不能成功收起侧边栏
                                scope.launch {
                                    expandDrawer.close()
                                }
                            }
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
                modifier = Modifier.weight(1f)
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
}

@Preview(backgroundColor = 0x888888)
@Composable
fun PreviewUiProjectRoot() {
    MainActivity.setEditFileMangerForUiPreview(LocalContext.current)

    val sampleData = listOf(
        sampleFiles()
    )
    UiProjectRoot(sampleData)
}