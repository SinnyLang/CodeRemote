package xyz.sl.coderemote.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

class ProjectRootActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UiProjectRoot()
        }
    }
}

@Composable
fun UiProjectRoot() {
    val expandDrawer = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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

    val projectFileRoot = listOf(
        FileNode.Directory("src", listOf(
            FileNode.Directory("main", listOf(
                FileNode.File("MainActivity.kt"),
                FileNode.File("Utils.kt")
            )),
            FileNode.Directory("test", listOf(
                FileNode.File("MainActivityTest.kt")
            ))
        )),
        FileNode.File("README.md")
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
                        UiFileTreeView(projectFileRoot)
                    }
                }
            }
        },
        drawerState = expandDrawer,
    ) {
        Column (modifier = Modifier.fillMaxSize()) {
            UiEditor(
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
                Log.i("ProjectRootActivity", "AA")
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
    UiProjectRoot()
}