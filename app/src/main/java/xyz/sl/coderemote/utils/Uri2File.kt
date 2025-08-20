package xyz.sl.coderemote.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import xyz.sl.coderemote.ui.FileNode

object UriUtils {
    val debugTag = "UriUtils"

    fun findFileUri(context: Context, rootUri: Uri, relativePath: String): Uri? {
        Log.d(debugTag, "findFileUri()->Uri=${rootUri}, relativePath=${relativePath}")
        // TODO: 直接打开本地文件之后点击file出错
        // java.lang.IllegalArgumentException: Invalid URI: content://com.android.externalstorage.documents/document/0000-0000:Android/data/.nomedia
        // at android.provider.DocumentsContract.getTreeDocumentId(DocumentsContract.java:1236)
        // at androidx.core.provider.DocumentsContractCompat$DocumentsContractApi21Impl.getTreeDocumentId(DocumentsContractCompat.java:239)
        // at androidx.core.provider.DocumentsContractCompat.getTreeDocumentId(DocumentsContractCompat.java:105)
        // at androidx.documentfile.provider.DocumentFile.fromTreeUri(DocumentFile.java:133)
        // at xyz.sl.coderemote.utils.UriUtils.findFileUri(Uri2File.kt:14)
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return null
        var currentDoc = rootDoc

        val parts = relativePath.split("/").filter { it.isNotEmpty() }
            .drop(1)
        for (part in parts) {
            currentDoc = currentDoc.findFile(part) ?: return null
        }
        Log.d(debugTag, "findFileUri()->fileUri=${currentDoc.uri}")
        return currentDoc.uri
    }

    /**
     * 将单个 Uri（文件或文件夹）转换为 FileNode
     */
    fun uriToFileNode(context: Context, uri: Uri): FileNode {
        val docFile = when {
            isTreeUri(uri) -> DocumentFile.fromTreeUri(context, uri)
            else -> DocumentFile.fromSingleUri(context, uri)
        } ?: throw IllegalArgumentException("无法解析 Uri: $uri")

        return documentFileToNode(docFile, null, context)
    }

    // 判断 Uri 是否是 TreeUri
    private fun isTreeUri(uri: Uri): Boolean {
        // SAF TreeUri 一般是 content://.../tree/...
        return uri.path?.contains("/tree/") == true
    }

    /**
     * 递归把 DocumentFile 转为 FileNode
     */
    fun documentFileToNode(file: DocumentFile, parent: FileNode?, context: Context): FileNode {
        return if (file.isDirectory) {
            val dir = FileNode.Directory(file.name?:"Unnamed", parent as? FileNode.Directory, listOf())
            val childrenNodes = file.listFiles().map {
                documentFileToNode(it, dir, context)
            }
            dir.children = childrenNodes
            dir
        } else {
            FileNode.File(file.name ?: "Unnamed", parent as? FileNode.Directory)
        }
    }
}
