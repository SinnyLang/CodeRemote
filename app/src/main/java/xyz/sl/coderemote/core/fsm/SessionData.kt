package xyz.sl.coderemote.core.fsm

import kotlinx.serialization.Serializable

@Serializable
data class SessionData(
    val version: Int = 1,
    val rootKey: String,
    val expandedKeys: List<String> = emptyList(),
    val openFiles: List<String> = emptyList(),
    val activeFile: String? = null,
)

interface SessionStore {
    suspend fun load(workspaceKey: String): SessionData?
    suspend fun save(workspaceKey: String, data: SessionData)
    suspend fun clear(workspaceKey: String)
}