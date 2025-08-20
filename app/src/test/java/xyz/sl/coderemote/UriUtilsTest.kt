package xyz.sl.coderemote

import androidx.documentfile.provider.DocumentFile
import org.junit.Test
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import xyz.sl.coderemote.ui.FileNode
import xyz.sl.coderemote.utils.UriUtils.documentFileToNode

class UriUtilsTest {
    @Test
    fun testUriToFileNode() {
        // 模拟一个文件夹
        val folder = mockk<DocumentFile>()
        every { folder.isDirectory } returns true
        every { folder.name } returns "root"

        // 模拟子文件
        val file1 = mockk<DocumentFile>()
        every { file1.isDirectory } returns false
        every { file1.name } returns "file1.txt"

        val file2 = mockk<DocumentFile>()
        every { file2.isDirectory } returns false
        every { file2.name } returns "file2.txt"

        val folder1 = mockk<DocumentFile>()
        every { folder1.isDirectory } returns true
        every { folder1.name } returns "dir1"

        val file11 = mockk<DocumentFile>()
        every { file11.isDirectory } returns false
        every { file11.name } returns "file11.txt"

        every { folder1.listFiles() } returns arrayOf(file11)
        every { folder.listFiles() } returns arrayOf(folder1, file1, file2)

        // 调用转换函数
        val node = documentFileToNode(folder, null, mockk())

        assertEquals("root", node.name)
        val dir = node as FileNode.Directory
        assertEquals(3, dir.children.size)
        assert(dir.children[0] is FileNode.Directory)
        assertEquals("dir1", dir.children[0].name)
        assert(dir.children[1] is FileNode.File)
        assertEquals("file1.txt", dir.children[1].name)
        assertEquals("root", dir.children[0].parent?.name)
    }
}