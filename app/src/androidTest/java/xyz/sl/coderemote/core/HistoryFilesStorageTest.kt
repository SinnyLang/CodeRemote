package xyz.sl.coderemote.core

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryFilesStorageTest {
    private lateinit var context: Context
    private lateinit var storage: HistoryFilesStorage

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        storage = DataStoreHistoryFiles(context)
    }

    @Test
    fun testAddAndGetUris() = runBlocking {
        val uri1 = Uri.parse("file:///test/file1.txt")
        val uri2 = Uri.parse("file:///test/file2.txt")

        storage.addUri(uri1)
        storage.addUri(uri2)

        val list = storage.getAllUris()
        println(list)

        assertTrue(list.contains(uri1))
        assertTrue(list.contains(uri2))
    }

    @Test
    fun testClear() = runBlocking {
        val uri = Uri.parse("file:///test/file3.txt")
        storage.addUri(uri)
        storage.clear()

        val list = storage.getAllUris()
        assertTrue(list.isEmpty())
    }
}
