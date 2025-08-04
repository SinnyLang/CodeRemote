package xyz.sl.coderemote.ui

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
fun UiRecentFileExplorer(recentFile: List<String>){
    Column {
        recentFile.forEach { fileItem ->
            NavigationDrawerItem(
                label = {
                    Row {
                        Icon(Icons.Default.InsertDriveFile, contentDescription = "")
                        Text(fileItem, modifier = Modifier.paddingFromBaseline(15.dp))
                    }
                },
                selected = false,
                onClick = {},
                modifier = Modifier.height(20.dp),
                badge = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Close, contentDescription = "")
                    }
                }
            )
        }
    }
}