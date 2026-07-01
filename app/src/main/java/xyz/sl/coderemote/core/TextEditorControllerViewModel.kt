package xyz.sl.coderemote.core

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TextEditorControllerViewModel(
    textSavedStateHandle: SavedStateHandle,
) : ViewModel() {
    val controller: EditorController by lazy {
        val text = textSavedStateHandle.get<String>("text") ?: ""
        BaseEditorController(text)
    }

}

class TextEditorControllerViewModelFactory(
    private var text: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(TextEditorControllerViewModel::class.java)){
            return TextEditorControllerViewModel(
                textSavedStateHandle = SavedStateHandle(mapOf("text" to text)),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}