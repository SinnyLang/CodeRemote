package xyz.sl.coderemote.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import xyz.sl.coderemote.core.DataStoreHistoryFiles
import xyz.sl.coderemote.core.HistoryFilesStorage
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

class FileViewModel(private val storage: HistoryFilesStorage) : ViewModel() {
    fun addUri(uri: Uri) {
        viewModelScope.launch {
            storage.addUri(uri)
        }
    }
}

@Composable
fun UiStartProject(onClickLocal: () -> Unit){
    val context = LocalContext.current
    val storage = DataStoreHistoryFiles(context)
    val viewModel: FileViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FileViewModel(storage) as T
            }
        }
    )

    val pickFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                viewModel.addUri(it)
                // 跳转到 FileTreeActivity，并传递 uri
                val intent = Intent(context, ProjectRootActivity::class.java).apply {
                    putExtra("uri", it.toString())
                }
                context.startActivity(intent)
                (context as? Activity)?.finish()// 销毁 MainActivity
            }
        }

    val pickFolderLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                viewModel.addUri(it)
                val intent = Intent(context, ProjectRootActivity::class.java).apply {
                    putExtra("uri", it.toString())
                }
                context.startActivity(intent)
                (context as? Activity)?.finish()// 销毁 MainActivity
            }
        }


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = Dp(200f))
    ) {
        Button(
            onClick = {}, Modifier.width(Dp(300f))
        ) { Text("Start From Remote with SSH") }

        Row {
            Button(
                onClick = {
                    pickFileLauncher.launch(arrayOf("*/*"))
                }, Modifier.width(Dp(150f))
            ) { Text("Local File") }
            Button(
                onClick = {
                    pickFolderLauncher.launch(null)
                }, Modifier.width(Dp(150f))
            ) { Text("Local Project") }
        }

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