package xyz.sl.coderemote.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun UiRecentFileExplorer(
    recentFileUri: List<Uri>,
    onClickFileItem : (Uri) -> Unit = {},
    onCloseFileItem : (Uri) -> Unit = {}
){
    Column {
        recentFileUri.forEach { fileUriItem ->
            val fileItemName:String = fileUriItem.lastPathSegment?:"UnknownFile"
            NavigationDrawerItem(
                label = {
                    Row {
                        Icon(Icons.Default.InsertDriveFile, contentDescription = "")
                        Text(fileItemName, modifier = Modifier.paddingFromBaseline(15.dp))
                    }
                },
                selected = false,
                onClick = { onClickFileItem(fileUriItem) },   // 此处点击打开文件
                modifier = Modifier.height(20.dp),
                badge = {
                    IconButton(onClick = { onCloseFileItem(fileUriItem) }) {
                        Icon(Icons.Default.Close, contentDescription = "")
                    }
                }
            )
        }
    }
}