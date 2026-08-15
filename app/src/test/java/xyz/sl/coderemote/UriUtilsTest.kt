package xyz.sl.coderemote

import androidx.documentfile.provider.DocumentFile
import org.junit.Test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import xyz.sl.coderemote.core.FileNode
import xyz.sl.coderemote.utils.UriUtils.documentFileToNode
import java.nio.file.Paths

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

        runBlocking {
            assertEquals("root", node.name)
            val dir = node as FileNode.Directory
            assertEquals(3, dir.getChildren().size)
            assert(dir.getChildren()[0] is FileNode.Directory)
            assertEquals("dir1", dir.getChildren()[0].name)
            assert(dir.getChildren()[1] is FileNode.File)
            assertEquals("file1.txt", dir.getChildren()[1].name)
            assertEquals("root", dir.getChildren()[0].parent?.name)
        }
    }

    @Test
    fun textPathRelativize() {
        // ..\..\..\d\c.txt
        var p1 = Paths.get("/C%3A/a/b/c/d.txt")
        var p2 = Paths.get("/c%3A/a/d/c.txt")
        println(p1.relativize(p2))

        // d\e.txt
        var p3 = Paths.get("/C%3A/a/b/c")
        var p4 = Paths.get("/C%3A/a/b/c/d/e.txt")
        println(p3.relativize(p4))

        // ..\..\..\..\D%3A\z\y\x\w\v.txt
        var p5 = Paths.get("/C%3A/a/b/c")
        var p6 = Paths.get("/D%3A/z/y/x/w/v.txt")
        println(p5.relativize(p6))

        // ..\..\..\z\y\x\w\v.txt
        var p7 = Paths.get("/a/b/c")
        var p8 = Paths.get("/z/y/x/w/v.txt")
        println(p7.relativize(p8))
    }
}