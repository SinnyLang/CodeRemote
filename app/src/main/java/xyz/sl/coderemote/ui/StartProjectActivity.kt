package xyz.sl.coderemote.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import xyz.sl.coderemote.ui.theme.TextEditorComposeTheme

class StartProjectActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UiStartProject (
                onClickLocal = {
                    startActivity(Intent(this, ProjectRootActivity::class.java))
                    finish()
                }
            )
        }
    }
}

@Composable
fun UiStartProject(onClickLocal: () -> Unit){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = Dp(200f))
    ) {
        Button(
            onClick = {}, Modifier.width(Dp(300f))
        ) { Text("Start From Remote with SSH") }

        Button(
            onClick = {
                onClickLocal()
            }, Modifier.width(Dp(300f))
        ) { Text("Start From Local") }

        Button(
            onClick = {}, Modifier.width(Dp(300f))
        ) { Text("Start From history") }
    }
}

@Preview(backgroundColor = 0x888888, device = "spec:width=400dp,height=860dp,dpi=440")
@Composable
fun PreviewUiStartProject(){
    TextEditorComposeTheme(
        useDarkTheme = true
    ) {
        UiStartProject({})
    }
}