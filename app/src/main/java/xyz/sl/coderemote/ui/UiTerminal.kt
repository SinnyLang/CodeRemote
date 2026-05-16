package xyz.sl.coderemote.ui

import android.graphics.Typeface
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.connectbot.terminal.Terminal
import org.connectbot.terminal.TerminalEmulator
import org.connectbot.terminal.TerminalEmulatorFactory
import xyz.sl.coderemote.BuildConfig
import xyz.sl.coderemote.utils.SshManager
import java.util.concurrent.CancellationException

/**
 * 一个真正可工作的 SSH Terminal UI
 *
 * 功能：
 * 1. termlib 终端绘制
 * 2. JSch Shell 交互
 * 3. PTY 支持
 * 4. ANSI/VT100 支持
 * 5. 键盘输入
 * 6. 远程输出显示
 */
var sshSendTag = "SSH-Send"
var sshReceiveTag = "SSH-Receive"
@Composable
fun SshTerminalScreen(
    sshManager: SshManager,
    modifier: Modifier = Modifier
) {
    val scopeSend = rememberCoroutineScope()
    val scopeReceive = rememberCoroutineScope()
    var emulator by remember {
        mutableStateOf<TerminalEmulator?>(null)
    }
    var started by remember {
        mutableStateOf(false)
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                sshManager.disconnect()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(Unit) {
        if (started) return@LaunchedEffect
        started = true

        /*
         * 打开 shell
         */
        val channel = sshManager.channelShell

        val shellInput = channel.inputStream
        val shellOutput = channel.outputStream

        /*
         * 创建终端仿真器
         */
        lateinit var terminalEmulator: TerminalEmulator

        terminalEmulator = TerminalEmulatorFactory.create(
            initialRows = 24,
            initialCols = 80,
            defaultForeground = Color.White,
            defaultBackground = Color.Black,

            /*
             * 用户键盘输入
             */
            onKeyboardInput = { data ->
                scopeSend.launch(Dispatchers.IO) {
                    try {
                        shellOutput.write(data)
                        shellOutput.flush()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(
                            "SSH",
                            "send failed",
                            e
                        )
                    }
                }
            }
        )

        emulator = terminalEmulator

        /*
         * 后台读取远程输出
         */
        scopeReceive.launch(Dispatchers.IO) {
            val buffer = ByteArray(8192)
            try {
                while (isActive && channel.isConnected) {
                    val len = shellInput.read(buffer)
                    if (len <= 0) {
                        break
                    }
                    terminalEmulator.writeInput(
                        buffer.copyOf(len)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(
                    "SSH",
                    "read loop failed",
                    e
                )
            } finally {
                Log.d("SSH", "read loop closed")
                try {
                    channel.disconnect()
                } catch (_: Exception) {
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val currentEmulator = emulator
        if (currentEmulator != null) {
            Terminal(
                terminalEmulator = currentEmulator,
                modifier = Modifier.fillMaxSize(),
                typeface = Typeface.MONOSPACE,
                backgroundColor = Color.Black,
                foregroundColor = Color.White,
                keyboardEnabled = true,
                showSoftKeyboard = true,
                /*
                 * 固定终端大小
                 *
                 * 后续你可以改成动态 resize
                 */
                forcedSize = Pair(24, 80)
            )
        }
    }
}

@Preview
@Composable
fun PreviewUiSshTerminalScreen(){
    val sshManagerWin : SshManager = remember { SshManager() }
    val sshManagerLinux : SshManager = remember { SshManager() }
    var scope : CoroutineScope = rememberCoroutineScope()
    var connected : Boolean by remember{ mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO){
            sshManagerWin.connect(
                BuildConfig.SSH_WIN_HOST,
                BuildConfig.SSH_WIN_PORT.toInt(),
                BuildConfig.SSH_WIN_USER,
                BuildConfig.SSH_WIN_PASSWORD
            )
            sshManagerLinux.connect(
                BuildConfig.SSH_LINUX_HOST,
                BuildConfig.SSH_LINUX_PORT.toInt(),
                BuildConfig.SSH_LINUX_USER,
                BuildConfig.SSH_LINUX_PASSWORD
            )
            connected = true
        }
    }

    if (connected) {
        Column() {
            SshTerminalScreen(sshManagerWin, Modifier.height(400.dp))
            Text("分割线")
            SshTerminalScreen(sshManagerLinux, Modifier.height(400.dp))
        }
    }
}
