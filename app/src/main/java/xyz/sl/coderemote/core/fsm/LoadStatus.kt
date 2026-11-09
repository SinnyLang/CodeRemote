package xyz.sl.coderemote.core.fsm

sealed interface LoadStatus {
    data object NotLoaded : LoadStatus
    data object Loading : LoadStatus
    data object Loaded : LoadStatus
    data class Failed(val error: Throwable) : LoadStatus
}