package xyz.sl.coderemote.core

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

interface HistoryFilesStorage {
    suspend fun addUri(uri: Uri)
    suspend fun getAllUris(): List<Uri>
    suspend fun clear()
}


val Context.dataStore by preferencesDataStore("recent_files")

class DataStoreHistoryFiles(private val context: Context) : HistoryFilesStorage {
    private val RECENT_FILES_KEY = stringSetPreferencesKey("recent_files")

    override suspend fun addUri(uri: Uri) {
        context.dataStore.edit { prefs ->
            val current = prefs[RECENT_FILES_KEY] ?: emptySet()
            // 加到最前面（保持最新优先）
            val updated = listOf(uri.toString()) + current.filter { it != uri.toString() }
            // 限制最多保存 20 条
            prefs[RECENT_FILES_KEY] = updated.take(20).toSet()
        }
    }

    override suspend fun getAllUris(): List<Uri> {
        val prefs = context.dataStore.data.first()
        return prefs[RECENT_FILES_KEY]?.map { Uri.parse(it) } ?: emptyList()
    }

    override suspend fun clear() {
        context.dataStore.edit { it.remove(RECENT_FILES_KEY) }
    }
}