package xyz.sl.coderemote.ui.dialog

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.sl.coderemote.utils.SshManager

const val tag = "SSHSelectDirectoryStepDialog"

@Composable
fun SSHSelectDirectoryStepDialog(
    onResult: (SshManager?, String) -> Unit, // 返回连接器和选中目录
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    // 连接参数
    var host by remember { mutableStateOf("172.16.155.87") }
    var port by remember { mutableStateOf("22") }
    var username by remember { mutableStateOf("jocker") }
    var password by remember { mutableStateOf("123") }

    // 连接状态
    var sshClient by remember { mutableStateOf<SshManager?>(null) }
    var isConnecting by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    // 目录浏览状态（第二步）
    var currentPath by remember { mutableStateOf("/") }
    var dirEntries by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedDirectory by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // 执行连接（异步）
    suspend fun performConnection(): Boolean {
        if (host.isBlank() || username.isBlank() || password.isBlank()) {
            connectionError = "请填写完整信息"
            return false
        }
        connectionError = null

        // 使用协程执行实际连接
        return withContext(Dispatchers.IO) {
            try {
                val client = SshManager()
                client.connect(host, port.toInt(), username, password)
                sshClient = client
                Log.d(tag, "ssh is connected: " + client.isConnected)
                // 加载根目录
                if (!client.isConnected) throw Exception("连接失败")

                currentPath = "/"
                dirEntries = client.listDirectoryFiles(currentPath)
                true
            } catch (e: Exception) {
                Log.e(tag, e.message, e)
                connectionError = e.message
                false
            }
        }
    }

    // 导航到子目录
    // 在 SSHSelectDirectoryStepDialog 内部定义
    suspend fun navigateTo(path: String) {
        if (sshClient == null) return
        isLoading = true
        // 使用 IO 线程执行网络操作
        val entries = withContext(Dispatchers.IO) {
            try {
                sshClient!!.listDirectoryFiles(path)
            } catch (e: Exception) {
                // 出错时返回空列表或抛出异常
                Log.e(tag, e.message, e)
                emptyList()
            } finally {
                isLoading = false
            }
        }
        // 更新 UI 状态（主线程）
        currentPath = path
        dirEntries = entries
    }

    // 定义两个步骤的 Composable 内容
    val step1: @Composable () -> Unit = {
        SSHConnectionContent(
            host = host,
            onHostChange = { host = it },
            port = port,
            onPortChange = { port = it },
            username = username,
            onUsernameChange = { username = it },
            password = password,
            onPasswordChange = { password = it },
            error = connectionError,
            isConnecting = isConnecting
        )
    }

    val step2: @Composable () -> Unit = {
        SSHCSelectDirectoryContent(
            currentPath = currentPath,
            dirEntries = dirEntries,
            selectedDirectory = selectedDirectory,
            onSelectDirectory = { selectedDirectory = it },
            onNavigateTo = ::navigateTo,
            coroutineScope = coroutineScope,
            isLoading = isLoading
        )
    }


    var currentStep by remember { mutableStateOf(0) }

    StepDialog(
        visible = true,
        onDismissRequest = onDismiss,
        currentStep = currentStep,
        steps = listOf(step1, step2),
        onStepChanging = { newStep -> currentStep = newStep},
        onComplete = {
            // 点击完成时传出结果
            onResult(sshClient, selectedDirectory ?: currentPath)
            onDismiss()
        },
        // TODO: 简化判断逻辑，连接按钮和下一步按钮分开，连接未成功时，下一步显示灰色表示不可达
        //   连接成功后，前置条件满足，下一步可达
        onNextClicked = { stepIndex ->
            when (stepIndex) {
                0 -> {
                    Log.d(tag, "第1步，连接终端")
                    // 防止连接过成中再次发起连接
                    if (isConnecting) return@StepDialog false
                    isConnecting = true

                    connectionError = null
                    coroutineScope.launch {
                        Log.d(tag, "协程启动")
                        val success = performConnection()
                        isConnecting = false
                        if (success) {
                            // 连接成功，允许跳转到下一步（onNextClicked 返回 true）
                            // 但我们现在是在异步回调中，不能直接返回 true
                            // 需要通知 StepDialog 可以前进
                            // 方法：在外部控制 currentStep 或重新触发点击
                            // 更简单的方式：在连接成功后自动跳转
                            currentStep = 1 // 直接跳转到第二步
                        } else {
                            // 连接失败，保持当前步骤
                        }
                    }
                    // 由于是异步，立即返回 false，阻止默认前进
                    return@StepDialog false
                } // 第一步 → 连接
                1 -> {
                    Log.d(tag, "第二步，选择工作目录")
                    // 第二步：验证是否选择了目录
                    if (selectedDirectory == null) {
                        // 可显示短暂提示，这里简单返回 false 阻止跳转
                        false
                    } else {
                        true
                    }
                }
                else -> true
            }
        },
        showPreviousButton = true
    )
}

@Composable
private fun SSHConnectionContent(
    host: String,
    onHostChange: (String) -> Unit,
    port: String,
    onPortChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    error: String?,
    isConnecting: Boolean
) {
    Column {
        TextField(
            value = host,
            onValueChange = onHostChange,
            placeholder = { Text("远程主机地址") },
            enabled = !isConnecting,
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = port,
            onValueChange = onPortChange,
            placeholder = { Text("端口") },
            enabled = !isConnecting,
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = username,
            onValueChange = onUsernameChange,
            placeholder = { Text("用户名") },
            enabled = !isConnecting,
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = { Text("密码") },
            enabled = !isConnecting,
            modifier = Modifier.fillMaxWidth()
        )
        if (isConnecting) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("正在连接...")
            }
        }
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SSHCSelectDirectoryContent(
    currentPath: String,
    dirEntries: List<String>,
    selectedDirectory: String?,
    onSelectDirectory: (String) -> Unit,
    onNavigateTo: suspend (String) -> Unit,
    coroutineScope: CoroutineScope,
    isLoading: Boolean // 新增参数
) {
    // 过滤掉 "." 和 ".."
    val filteredEntries = dirEntries.filter { it != "." && it != ".." }

    // 计算父路径
    val parentPath = remember(currentPath) {
        when (currentPath) {
            "/" -> null
            else -> {
                val trimmed = currentPath.trimEnd('/')
                val lastSlash = trimmed.lastIndexOf('/')
                if (lastSlash <= 0) "/" else trimmed.substring(0, lastSlash)
            }
        }
    }

    Column {
        Text("当前路径: $currentPath", style = MaterialTheme.typography.bodyMedium)

        if (isLoading) {
            // 加载状态
            Box(
                modifier = Modifier
                    .heightIn(max = 200.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // 正常列表
            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                if (parentPath != null) {
                    item {
                        ListItem(
                            headlineContent = { Text(".. 返回上一层") },
                            leadingContent = {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回上层")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        onNavigateTo(parentPath)
                                    }
//                                    onSelectDirectory(null) // 可选
                                }
                        )
                    }
                }
                items(filteredEntries) { dir ->
                    val fullPath = if (currentPath == "/") "/$dir" else "$currentPath/$dir"
                    val isSelected = selectedDirectory == fullPath
                    ListItem(
                        headlineContent = { Text(dir) },
                        leadingContent = {
                            Icon(Icons.Default.Folder, contentDescription = null)
                        },
                        trailingContent = {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isLoading) { // 加载时禁用点击
                                coroutineScope.launch {
                                    onNavigateTo(fullPath)
                                }
                                onSelectDirectory(fullPath)
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (selectedDirectory != null) "已选择: $selectedDirectory" else "请点击目录选择",
            style = MaterialTheme.typography.bodySmall,
            color = if (selectedDirectory != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
@Preview
@Composable
fun SSHSelectDirectoryStepDialogPreview() {
    var showDialog by remember { mutableStateOf(false) }

    TextButton (onClick = { showDialog = true }) {
        Text("选择远程目录")
    }

    var ssh by remember { mutableStateOf<SshManager?>(null) }
    var workDir by remember { mutableStateOf("") }

    if (showDialog) {
        SSHSelectDirectoryStepDialog(
            onResult = { client, dir ->
                // 接收结果，存入 ViewModel 或全局
                if (client != null) {
                    // 保存 client 和 dir
                    ssh = client
                    workDir = dir
                    // 关闭对话框后可使用
                }
                showDialog = false
                Log.d(tag, workDir)
            },
            onDismiss = { showDialog = false }
        )
    }
}