package xyz.sl.coderemote.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


/** 步骤对话框，每完成一个步骤点击下一步，直到完成所有步骤
 * @param visible 是否弹出对话框
 * @param onDismissRequest
 * @param currentStep 当前处于第 currentStep 步
 * @param onStepChanging 执行 onStepChanging 改变 currentStep
 * @param onComplete 点击完成后执行 onComplete
 * @param showPreviousButton
 * @param onNextClicked 点击下一步后，调用 onNextClicked 判断是否可以进入下一步
 * @param onPreviousClicked 点击上一步后，调用 onPreviousClicked 判断是否可以返回上一步
 *
 */
@Composable
fun StepDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    currentStep: Int,
    steps: List<@Composable () -> Unit>,
    onStepChanging: ((Int) -> Unit),
    onComplete: () -> Unit,
    showPreviousButton: Boolean = true,
    onNextClicked: ((currentStep: Int) -> Boolean)? = null,
    onPreviousClicked: ((currentStep: Int) -> Unit)? = null,
) {
    if (!visible) return

    //将 currentStep 状态提升，配合 onStepChanging 更改 currentStep
    //var currentStep by remember { mutableStateOf(0) }

    val totalSteps = steps.size

    LaunchedEffect(currentStep) {
        onStepChanging.invoke(currentStep)
    }

    fun goNext() {
        val allow = onNextClicked?.invoke(currentStep) ?: true
        if (!allow) return
        if (currentStep < totalSteps - 1) {
            onStepChanging.invoke(currentStep+1)
        } else {
            onComplete()
            onDismissRequest()
        }
    }

    fun goPrevious() {
        onPreviousClicked?.invoke(currentStep)
        if (currentStep > 0) onStepChanging.invoke(currentStep-1)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (i in 0 until totalSteps) {
                    val isCompleted = i < currentStep
                    val isActive = i == currentStep
                    val symbol = when {
                        isCompleted -> "✔"
                        isActive -> "●"
                        else -> "○"
                    }
                    Text(
                        text = symbol,
                        style = MaterialTheme.typography.titleMedium,
                        color = when {
                            isCompleted -> MaterialTheme.colorScheme.primary
                            isActive -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp)
            ) {
                steps[currentStep]()
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (showPreviousButton && currentStep > 0) {
                    TextButton(onClick = ::goPrevious) {
                        Text("上一步")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                val buttonText = if (currentStep == totalSteps - 1) "完成" else "下一步"
                Button(onClick = ::goNext) {
                    Text(buttonText)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}

@Preview
@Composable
fun StepDialogPreview(){
    val s1 = @Composable {
        Column {
            Text("第一步：输入用户名")
            OutlinedTextField(value = "", onValueChange = {}, label = { Text("用户名") })
        }
    }

    val s2 = @Composable {
        Column {
            Text("第二步：选择偏好")
            // 选择控件...
        }
    }

    val s3 = @Composable {
        Column {
            Text("第三步：确认信息")
            // 显示摘要
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(0) }

    Button(onClick = { showDialog = true }) {
        Text("开始流程")
    }

    if (showDialog) {
        StepDialog(
            visible = showDialog,
            onDismissRequest = { showDialog = false },
            steps = listOf(
                { s1() },
                { s2() },
                { s3() }
            ),
            onComplete = {
                // 所有步骤完成
                showDialog = false
                // 执行最终操作
            },
            onStepChanging = { stepIndex ->
                // 可在此记录步骤，或进行验证
                currentStep = stepIndex
                println("当前步骤: $stepIndex")
            },
            currentStep = currentStep
        )
    }


}
