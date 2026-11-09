package xyz.sl.coderemote.core.fsm

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.serializer

class DataStoreSessionStore(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : SessionStore {

    private fun prefKey(workspaceKey: String) =
        stringPreferencesKey("session_$workspaceKey")

    override suspend fun load(workspaceKey: String): SessionData? {
        val raw = dataStore.data.first()[prefKey(workspaceKey)] ?: return null
        return runCatching { json.decodeFromString(SessionData.serializer(), raw) }.getOrNull()
    }

    override suspend fun save(workspaceKey: String, data: SessionData) {
        dataStore.edit {
            it[prefKey(workspaceKey)] = json.encodeToString(SessionData.serializer(), data)
        }
    }

    override suspend fun clear(workspaceKey: String) {
        dataStore.edit { it.remove(prefKey(workspaceKey)) }
    }
}