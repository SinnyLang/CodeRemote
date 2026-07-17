package xyz.sl.coderemote.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// 核心对话框容器 - 所有对话框的基础
@Composable
fun BaseDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    icon: (@Composable () -> Unit)? = null,
    confirmButton: @Composable (() -> Unit) = {},
    dismissButton: @Composable (() -> Unit) = {},
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = title?.let { { Text(it) } },
        icon = icon,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        },
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}

// 单行文本输入对话框
@Composable
fun TextInputDialog(
    title: String,
    initialValue: String = "",
    label: String = "",
    placeholder: String = "",
    confirmText: String = "确定",
    dismissText: String = "取消",
    validator: (String) -> Boolean = { it.isNotBlank() },
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    var isError by remember { mutableStateOf(false) }

    BaseDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmButton = {
            TextButton(
                onClick = { if (validator(text)) onConfirm(text) },
                enabled = validator(text) && !isError
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        }
    ) {
        TextField(
            value = text,
            onValueChange = {
                text = it
                isError = !validator(it)
            },
            label = label.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
            placeholder = placeholder.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
            singleLine = true,
            isError = isError || errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                if (isError && errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

// 多行文本输入对话框
@Composable
fun MultiLineTextInputDialog(
    title: String,
    initialValue: String = "",
    label: String = "",
    placeholder: String = "",
    confirmText: String = "确定",
    dismissText: String = "取消",
    maxLines: Int = 5,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    BaseDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        }
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            label = label.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
            placeholder = placeholder.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
            maxLines = maxLines,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// 单选列表对话框
@Composable
fun <T> SingleSelectDialog(
    title: String,
    items: List<T>,
    selectedItem: T? = null,
    itemLabel: (T) -> String,
    confirmText: String = "确定",
    dismissText: String = "取消",
    onDismiss: () -> Unit,
    onConfirm: (T) -> Unit
) {
    var selected by remember { mutableStateOf(selectedItem) }

    BaseDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmButton = {
            TextButton(
                onClick = { selected?.let { onConfirm(it) } },
                enabled = selected != null
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        }
    ) {
        items.forEach { item ->
            ListItem(
                headlineContent = { Text(itemLabel(item)) },
                leadingContent = {
                    RadioButton(
                        selected = selected == item,
                        onClick = { selected = item }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selected = item }
            )
            Divider()
        }
    }
}

// 多选列表对话框
@Composable
fun <T> MultiSelectDialog(
    title: String,
    items: List<T>,
    selectedItems: List<T> = emptyList(),
    itemLabel: (T) -> String,
    confirmText: String = "确定",
    dismissText: String = "取消",
    onDismiss: () -> Unit,
    onConfirm: (List<T>) -> Unit
) {
    var selected by remember { mutableStateOf(selectedItems.toSet()) }

    BaseDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmButton = {
            TextButton(onClick = { onConfirm(selected.toList()) }) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        }
    ) {
        items.forEach { item ->
            ListItem(
                headlineContent = { Text(itemLabel(item)) },
                leadingContent = {
                    Checkbox(
                        checked = selected.contains(item),
                        onCheckedChange = {
                            if (it) {
                                selected = selected + item
                            } else {
                                selected = selected - item
                            }
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (selected.contains(item)) {
                            selected = selected - item
                        } else {
                            selected = selected + item
                        }
                    }
            )
            Divider()
        }
    }
}


// 标准确认对话框
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确定",
    dismissText: String = "取消",
    icon: ImageVector? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    BaseDialog(
        onDismissRequest = onDismiss,
        title = title,
        icon = icon?.let { { Icon(it, contentDescription = null) } },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        }
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// 危险操作确认对话框（红色按钮）
@Composable
fun DangerConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "删除",
    dismissText: String = "取消",
    icon: ImageVector? = Icons.Default.Warning,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    BaseDialog(
        onDismissRequest = onDismiss,
        title = title,
        icon = icon?.let {
            { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        }
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// 信息展示对话框
@Composable
fun InfoDialog(
    title: String,
    confirmText: String = "确定",
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    BaseDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(confirmText) }
        }
    ) {
        content()
    }
}

// 带进度条的对话框
@Composable
fun ProgressDialog(
    title: String,
    progress: Float,
    message: String? = null,
    onDismiss: () -> Unit
) {
    BaseDialog(
        onDismissRequest = onDismiss,
        title = title,
//        confirmButton = null,
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            message?.let {
                Text(it, modifier = Modifier.padding(bottom = 16.dp))
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// DSL 风格的对话框构建器
class DialogBuilder {
    var title: String = ""
    var icon: (@Composable () -> Unit)? = null
    var content: (@Composable ColumnScope.() -> Unit)? = null
    var confirmButton: (@Composable () -> Unit) = {  }
    var dismissButton: (@Composable () -> Unit) = {  }
    var onDismiss: () -> Unit = {}

    fun build(): @Composable () -> Unit {
        return {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = title.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
                icon = icon,
                text = content?.let { {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        content = it
                    )
                } },
                confirmButton = confirmButton,
                dismissButton = dismissButton
            )
        }
    }
}

// DSL 扩展函数
@Composable
fun dialog(block: DialogBuilder.() -> Unit) {
    val builder = DialogBuilder().apply(block)
    builder.build()()
}

@Preview
@Composable
fun DialogPreview(){
    val title = "标题"
    val message = "消息"

    val selectItems = listOf("item1","item2","item3")


    var index by remember { mutableIntStateOf(0) }

    Column {
        TextButton(onClick = {index=1}) { Text("BaseDialog") }
        TextButton(onClick = {index=2}) { Text("TextInputDialog") }
        TextButton(onClick = {index=3}) { Text("MultiLineTextInputDialog") }
        TextButton(onClick = {index=4}) { Text("SingleSelectDialog") }
        TextButton(onClick = {index=5}) { Text("MultiSelectDialog") }
        TextButton(onClick = {index=6}) { Text("ConfirmDialog") }
        TextButton(onClick = {index=7}) { Text("DangerConfirmDialog") }
        TextButton(onClick = {index=8}) { Text("InfoDialog") }
        TextButton(onClick = {index=9}) { Text("ProgressDialog") }
        TextButton(onClick = {index=10}) { Text("DialogBuilder") }
    }
    when(index){
        0 -> {}
        1 ->
            BaseDialog(
                onDismissRequest = {},
                title = "title",
                dismissButton = { TextButton(onClick = { index = 0 }) { Text("按钮") } }
            ) { Text("123") }
        2 ->
            TextInputDialog(
                title = title,
                onDismiss = {index=0},
                onConfirm = {}
            )
        3 ->
            MultiLineTextInputDialog(
                title = title,
                onDismiss = {index=0},
                onConfirm = {}
            )
        4 ->
            SingleSelectDialog(
                title = title,
                items = selectItems,
                itemLabel = {"$it.txt" },
                onDismiss = {index=0},
                onConfirm = {}
            )
        5 ->
            MultiSelectDialog(
                title = title,
                items = selectItems,
                itemLabel = {"$it.txt"},
                onDismiss = {index=0},
                onConfirm = {}
            )
        6 ->
            ConfirmDialog(
                title = title,
                message = message,
                onDismiss = {index=0},
                onConfirm = {}
            )
        7 ->
            DangerConfirmDialog(
                title = title,
                message = message,
                onDismiss = {index=0},
                onConfirm = {}
            )
        8 ->
            InfoDialog(
                title = title,
                onDismiss = {index=0}
            ) { Text(message) }
        9 ->
            ProgressDialog(
                title = title,
                progress = 0.3378f,
                message = message,
                onDismiss = {index=0}
            )
        10 ->
            dialog {
                content = {
                    Text("这是自定义内容")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(value = "", onValueChange = {})
                }
                confirmButton = {
                    TextButton(onClick = {}) { Text("确定") }
                }
                onDismiss = { index=0 }
            }
    }


}