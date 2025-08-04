package xyz.sl.coderemote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UiOutputPanel(
    modifier: Modifier,
    tabTitles: List<String>,
    tabContents: List<String>
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var isPanelVisible by remember { mutableStateOf(true) }

    Column (modifier = modifier) {
        // 顶部标签栏
        Row(
            modifier = Modifier.height(30.dp).fillMaxWidth()
        ) {
            IconButton(
                modifier = Modifier.background(Color.White).padding(start = 10.dp).width(20.dp),
                onClick = {isPanelVisible = !isPanelVisible}
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "")
            }
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 0.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        modifier = Modifier.height(30.dp).width(30.dp),
                    ) {
                        Row (
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(title)
                        }
                    }
                }
            }
        }

        // 输出框
        if (isPanelVisible)
            UiOutputContent(modifier, tabContents[selectedTabIndex])
    }
}

@Composable
fun UiOutputContent(
    modifier: Modifier,
    tabContent: String
){
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxHeight(0.4f)
            .background(Color(0xFF1E1E1E)) // 黑色背景
            .padding(4.dp)
    ) {
        Text(
            text = tabContent,
            color = Color.White,
            modifier = Modifier
                .verticalScroll(scrollState)
                .fillMaxSize(),
            fontSize = 12.sp
        )
    }
}

@Preview
@Composable
fun PreviewUiOutputPanel() {
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

    UiOutputPanel(tabTitles = tabs, tabContents = contents, modifier = Modifier
        .fillMaxWidth()
        .height(300.dp)
    )
}
