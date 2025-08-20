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
import androidx.compose.runtime.remember
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
import xyz.sl.coderemote.utils.UriUtils.findFileUri
import xyz.sl.coderemote.utils.UriUtils.uriToFileNode

class ProjectRootActivity : ComponentActivity() {
    var debugTag : String = "ProjectRootActivity"

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

        val onDrawerFileItemClick = { node: FileNode ->
            Log.i(debugTag, "onDrawerFileItemClick()->${node.name}")
            var tmpNode = node
            var relativePath = "/"+tmpNode.name
            while (tmpNode.parent != null){
                relativePath = "/"+tmpNode.parent?.name + relativePath
                tmpNode = tmpNode.parent as FileNode.Directory
            }
            vm.updateUri( findFileUri(this, uri, relativePath) ?: Uri.EMPTY )
        }

        setContent {
            UiProjectRoot(projectFileRoot, onDrawerFileItemClick)
        }
    }
}

class ProjectRootViewModel : ViewModel() {
    /**
     * Activity 可以直接修改 uri，Composable 也能修改 uri，并实时刷新 UI，不会出现
     * “UI 有自己一份，Activity 有自己一份，彼此不同步”的问题。
     *
     * 在 Compose 里，这个问题的标准解法就是：单一数据源（Single Source of Truth），即
     * 把 uri 统一放在一个 状态容器（mutableStateOf 或 ViewModel）里，Activity 和
     * Composable 都读写它。
     */
    var currentUri by mutableStateOf(Uri.EMPTY)
        private set

    fun updateUri(newUri: Uri) {
        currentUri = newUri
    }
}

@Composable
fun UiProjectRoot(
    projectFileRoot: List<FileNode> = listOf(),
    onDrawerFileItemClick: (file: FileNode) -> Unit = {},
    vm: ProjectRootViewModel = viewModel()
) {
    val context = LocalContext.current
    val expandDrawer = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    vm

    val recentFile = remember { mutableStateListOf<String>() }
    recentFile.add("file 1")
    recentFile.add("file 2")
    val projectFile = remember { mutableStateListOf<String>() }
    projectFile.add("file 1")
    projectFile.add("file 2")

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
                UiRecentFileExplorer(recentFile)
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
                text = "This ids this ids texthis ids texthis ids texthis ids texthis ids texthis ids texthis ids texthis ids textext\nhisd is text\nahis is text" +
                        "his dis text\nhis is tdext" +
                        "\n\n\n\n\n\n\n\n\n\n\n\n\ns\n" +
                        "dhids is text\n" +
                        "dhzzis is text\n" +
                        "dhis is text\n" +
                        "d\n" +
                        "s\n" +
                        "a\n" +
                        "d\n" +
                        "d\n" +
                        "f\n" +
                        "f\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\ndasa" +
                        "This id1s text\n" +
                        "hisd is text\n" +
                        "ahis is2 textT" +
                        "his i3ds text\nhi" +
                        "sd is 4text\nahis is" +
                        "\n texet" +
                        "This i5ds text\n" +
                        "hisd 4is text\n" +
                        "ahis i6s textT" +
                        "his i7ds text\nhi" +
                        "sd i8 text\nahis is" +
                        "\n t9ext"+
                        "his i7ds text\nhi" +
                        "sd i8 text\nahis is" +
                        "\n t29ext"+
                        "his i17ds text\nhi" +
                        "sd i18 text\nahis is" +
                        "\n t92ext",
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
    val sampleData = listOf(
        sampleFiles()
    )
    UiProjectRoot(sampleData)
}