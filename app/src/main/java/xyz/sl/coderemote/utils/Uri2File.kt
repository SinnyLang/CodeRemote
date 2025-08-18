package xyz.sl.coderemote.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import xyz.sl.coderemote.ui.FileNode

object UriUtils {
    /**
     * 将单个 Uri（文件或文件夹）转换为 FileNode
     */
    fun uriToFileNode(context: Context, uri: Uri): FileNode {
        val docFile = when {
            isTreeUri(uri) -> DocumentFile.fromTreeUri(context, uri)
            else -> DocumentFile.fromSingleUri(context, uri)
        } ?: throw IllegalArgumentException("无法解析 Uri: $uri")

        return documentFileToNode(docFile, context)
    }

    // 判断 Uri 是否是 TreeUri
    private fun isTreeUri(uri: Uri): Boolean {
        // SAF TreeUri 一般是 content://.../tree/...
        return uri.path?.contains("/tree/") == true
    }

    /**
     * 递归把 DocumentFile 转为 FileNode
     */
    fun documentFileToNode(file: DocumentFile, context: Context): FileNode {
        return if (file.isDirectory) {
            val childrenNodes = file.listFiles().map { documentFileToNode(it, context) }
            FileNode.Directory(file.name ?: "Unnamed", childrenNodes)
        } else {
            FileNode.File(file.name ?: "Unnamed")
        }
    }
}
