package xyz.sl.coderemote.ui.text

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.sl.coderemote.MainActivity
import kotlin.math.log10

/** 该组件能显示行号和文本
 *
 * @param textEditorControllerViewModel 要显示的文本被保存在此处
 * @param textStyle
 * @param editorBackgroundColor
 */
@Composable
fun UiTextAreaShow(
    textEditorControllerViewModel: TextEditorControllerViewModel,
    textStyle : TextStyle = LocalTextStyle.current.copy(),
    editorBackgroundColor: Color = Color.LightGray
){
    // 记录文本区域滚动的状态
    var scrollState = rememberScrollState()
    var horizontalScrollState = rememberScrollState()

    Row (
        modifier = Modifier.padding(0.dp).fillMaxSize()
    ) {
        LineNumberColumnComponent(
            lines = textEditorControllerViewModel.controller.textRows,
            scrollState = scrollState,
            fontSize = textStyle.fontSize
        )
        Box (
            modifier = Modifier.background(editorBackgroundColor)
                .padding(start = 2.dp, end = 5.dp, top = 0.dp, bottom = 0.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            UiTextEditArea(
                textEditorControllerViewModel = textEditorControllerViewModel,
                textEditAreaModifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScrollState),
                textStyle = textStyle
            )
        }
    }


}

/** 显示行号的组件
 *
 *    当前Bug：改变数字的字体可能会导致显示到第二行；
 *            行号是固定的，不会随文本行数增减而变化；
 *            滚动到头会有粘滞效应
 *  @param lines 总行数
 *  @param scrollState 滚动状态
 *  @param fontSize
 */
@Composable
fun LineNumberColumnComponent(
    lines: Int,
    scrollState: ScrollState,
    fontSize: TextUnit
) {
    var width = 10 * (log10(lines.toDouble()).toInt() + 2)


    Column(
        modifier = Modifier
            .width(width.dp)
            .fillMaxHeight()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.End
    ) {
        for (i in 1..lines.coerceAtLeast(1)) {
            Text(
                text = i.toString(),
                fontSize = fontSize,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .padding(end = 0.dp)
//                    .height(22.dp) // 与行高保持一致
            )
        }
    }
}

@Preview
@Composable
fun UiTextAreaShowPreview(){
    MainActivity.setEditFileMangerForUiPreview(LocalContext.current)

    val text = remember { """
        > Configure project :app
        Configuration '_internal-unified-test-platform-android-device-provider-ddmlib' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.
        Configuration '_internal-unified-test-platform-android-device-provider-gradle' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.
        Configuration '_internal-unified-test-platform-android-driver-instrumentation' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.
        Configuration '_internal-unified-test-platform-android-test-plugin' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.
        Configuration '_internal-unified-test-platform-android-test-plugin-host-additional-test-output' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.
        Configuration '_internal-unified-test-platform-android-test-plugin-host-apk-installer' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.
        Configuration '_internal-unified-test-platform-android-test-plugin-host-coverage' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.
        Configuration '_internal-unified-test-platform-android-test-plugin-host-device-info' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.
        Configuration '_internal-unified-test-platform-android-test-plugin-host-emulator-control' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.
        Configuration '_internal-unified-test-platform-android-test-plugin-host-logcat' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.
        Configuration '_internal-unified-test-platform-android-test-plugin-host-retention' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.
        Configuration '_internal-unified-test-platform-android-test-plugin-result-listener-gradle' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.
        Configuration '_internal-unified-test-platform-core' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.
        Configuration '_internal-unified-test-platform-launcher' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.
        Configuration 'androidApis' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.
        Configuration 'androidJdkImage' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.
        Configuration 'androidTestApiDependenciesMetadata' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.
        Configuration 'androidTestCompileOnlyDependenciesMetadata' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.
        Configuration 'androidTestDebugApiDependenciesMetadata' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.
        Configuration 'androidTestDebugCompileOnlyDependenciesMetadata' was resolved during configuration time.
        This is a build performance and scalability issue.
        See https://github.com/gradle/gradle/issues/2298
        Run with --info for a stacktrace.

        
        
        """.trimIndent()
    }

    val controllerViewModel : TextEditorControllerViewModel = viewModel(
        factory = TextEditorControllerViewModelFactory(text)
    )


    var textStyle = LocalTextStyle.current.copy(fontSize = 20.sp)

    UiTextAreaShow(
        textEditorControllerViewModel = controllerViewModel,
        textStyle = textStyle,
    )

}