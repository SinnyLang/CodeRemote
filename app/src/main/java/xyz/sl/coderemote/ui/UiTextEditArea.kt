package xyz.sl.coderemote.ui

import android.app.Application
import android.util.Log
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.sl.coderemote.core.EditorAction
import xyz.sl.coderemote.core.TextEditorControllerViewModel
import xyz.sl.coderemote.core.TextEditorControllerViewModelFactory

@Composable
fun UiTextEditArea(controllerViewModel: TextEditorControllerViewModel){
    val controller = controllerViewModel.controller

    val textFieldValue = remember {
        mutableStateOf(TextFieldValue(controller.text.toString()))
    }

    fun handleBasicTextValueChange(oldTextValue: TextFieldValue, newTextValue: TextFieldValue){
        Log.d("TextEdit-Action",
            "Before Update: Controller Cursor in: "+controller.cursorOffset+
                    ". UI Cursor in:"+newTextValue.selection.start)
        // 1. diff -> EditorAction
        val editAction = diff(oldTextValue.text, newTextValue.text)
        Log.d(
            "TextEdit-Action",
            "In "+editAction.start+
                    ", del=\""+editAction.deleted+
                    "\", ins=\""+editAction.inserted+"\""
        )
        // 2. update cursor to edited position
        controller.cursorOffset = editAction.start
        // 3. controller.dispatch(...)
        controller.dispatch(
            EditorAction.Delete(editAction.deleted.length)
        )
        controller.dispatch(
            EditorAction.Insert(editAction.inserted)
        )
        // 4. update cursor to current position
        controller.cursorOffset = newTextValue.selection.start
        Log.d("TextEdit-Action",
            "After Update: Controller Cursor in: "+controller.cursorOffset+
                    ". UI Cursor in:"+newTextValue.selection.start)
        // 5. 更新 UI state
        textFieldValue.value =
            oldTextValue.copy(
                text = controller.text.toString(),
                selection =
                    TextRange(controller.cursorOffset, controller.cursorOffset)
            )
    }

    BasicTextField(
        value = textFieldValue.value,
        onValueChange = { newTextValue ->
            handleBasicTextValueChange(textFieldValue.value, newTextValue)
        },
    )

}

/**
 *    old: |a|b|    --> | start = 1 | oldEnd = 0 |  -->  del = [1,1] = ""
 *         |0|1|                    | newEnd = 1 |       ins = [1,2] = "c"
 *         -------
 *    new: |a|c|b|
 *         |0|1|2|
 */
fun diff(oldText: String, newText: String): DiffResult {
    // 从前向后找
    var start = 0
    while (
        start < oldText.length &&
        start < newText.length &&
        oldText[start] == newText[start]
    ) {
        start++
    }

    // 从后向前找
    var oldEnd = oldText.length - 1
    var newEnd = newText.length - 1

    while (
        oldEnd >= start &&
        newEnd >= start &&
        oldText[oldEnd] == newText[newEnd]
    ) {
        oldEnd--
        newEnd--
    }

    // 动作
    val deleted =
        if (oldEnd >= start) oldText.substring(start, oldEnd + 1)
        else ""
    val inserted =
        if (newEnd >= start) newText.substring(start, newEnd + 1)
        else ""

    return DiffResult(start, deleted, inserted)
}

data class DiffResult(
    val start: Int,
    val deleted: String,
    val inserted: String
)


@Preview
@Composable
fun UiTextEditAreaPreview(){
    val text = remember { """
        12345
        abcde
        hijkl
        
        
        """.trimIndent()
    }

    val application : Application = LocalContext.current.applicationContext as Application
    val controllerViewModel : TextEditorControllerViewModel = viewModel(
        factory = TextEditorControllerViewModelFactory(text, application)
    )


    UiTextEditArea(controllerViewModel = controllerViewModel )



}
