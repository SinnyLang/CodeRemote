package xyz.sl.coderemote.utils;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.documentfile.provider.DocumentFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * DocumentFile 工具类
 * 提供递归创建父目录等功能
 */
public class DocumentFileUtils {

    private static final String tag = "DocumentFileUtils";

    /**
     * 确保父目录存在
     * 如果父目录不存在，则递归创建
     *
     * @param context 上下文
     * @param file DocumentFile 对象（文件或目录）
     * @return 父目录的 DocumentFile 对象，如果无法创建则返回 null
     */
    public static DocumentFile ensureParentDirectory(Context context, DocumentFile file) {
        if (file == null) {
            return null;
        }

        // 获取父目录
        DocumentFile parent = file.getParentFile();
        if (parent == null) {
            // 已经是根目录，无法创建父目录
            return null;
        }

        // 如果父目录已经存在，直接返回
        if (parent.exists() && parent.isDirectory()) {
            return parent;
        }

        // 递归创建父目录
        return ensureDirectoryExists(context, parent);
    }

    /**
     * 确保目录存在
     * 如果目录不存在，则递归创建
     *
     * @param context 上下文
     * @param directory 要确保存在的目录
     * @return 存在或成功创建的目录，如果失败则返回 null
     */
    public static DocumentFile ensureDirectoryExists(Context context, DocumentFile directory) {
        if (directory == null) {
            return null;
        }

        // 如果目录已存在且是目录，直接返回
        if (directory.exists() && directory.isDirectory()) {
            return directory;
        }

        // 获取父目录
        DocumentFile parent = directory.getParentFile();
        if (parent == null) {
            // 没有父目录，可能是根目录，无法创建
            return null;
        }

        // 递归确保父目录存在
        DocumentFile ensuredParent = ensureDirectoryExists(context, parent);
        if (ensuredParent == null) {
            return null;
        }

        // 在父目录下创建当前目录
        DocumentFile createdDir = ensuredParent.createDirectory(directory.getName());
        if (createdDir != null && createdDir.exists()) {
            return createdDir;
        }

        return null;
    }

    /**
     * 根据路径递归创建目录
     *
     * @param context 上下文
     * @param baseTreeUri 基础树根 Uri（通过 ACTION_OPEN_DOCUMENT_TREE 获取）
     * @param path 要创建的路径，如 "home/user/documents"
     * @return 创建后的目录，如果失败则返回 null
     */
    public static DocumentFile ensureDirectoryPathExists(Context context, Uri baseTreeUri, String path) {
        if (baseTreeUri == null || path == null || path.isEmpty()) {
            return null;
        }

        DocumentFile root = DocumentFile.fromTreeUri(context, baseTreeUri);
        if (root == null) {
            return null;
        }

        // 分割路径
        String[] segments = path.split("/");
        DocumentFile current = root;

        for (String segment : segments) {
            if (segment.isEmpty()) continue;

            DocumentFile child = findChild(context, current, segment, true);
            if (child == null) {
                // 如果目录不存在，创建它
                child = current.createDirectory(segment);
                if (child == null) {
                    return null;
                }
            }
            current = child;
        }

        return current;
    }

    /**
     * 在父目录中查找子目录或文件
     *
     * @param context 上下文
     * @param parent 父目录
     * @param name 子项名称
     * @param isDirectory 是否为目录
     * @return 找到的 DocumentFile，如果不存在则返回 null
     */
    public static DocumentFile findChild(Context context, DocumentFile parent, String name, boolean isDirectory) {
        if (parent == null || !parent.exists() || !parent.isDirectory()) {
            return null;
        }

        DocumentFile[] children = parent.listFiles();
        for (DocumentFile child : children) {
            if (child.isDirectory() == isDirectory && child.getName() != null && child.getName().equals(name)) {
                return child;
            }
        }
        return null;
    }

    /**
     * 获取或创建目录（不递归）
     *
     * @param parent 父目录
     * @param dirName 目录名称
     * @return 存在或创建的目录，如果失败则返回 null
     */
    public static DocumentFile getOrCreateDirectory(DocumentFile parent, String dirName) {
        if (parent == null || !parent.exists() || !parent.isDirectory()) {
            return null;
        }

        // 查找是否已存在
        for (DocumentFile child : parent.listFiles()) {
            if (child.isDirectory() && child.getName() != null && child.getName().equals(dirName)) {
                return child;
            }
        }

        // 不存在则创建
        return parent.createDirectory(dirName);
    }

    /**
     * 获取或创建文件
     *
     * @param parent 父目录
     * @param fileName 文件名称
     * @param mimeType MIME 类型（如 "text/plain"）
     * @return 存在或创建的文件，如果失败则返回 null
     */
    public static DocumentFile getOrCreateFile(DocumentFile parent, String fileName, String mimeType) {
        if (parent == null || !parent.exists() || !parent.isDirectory()) {
            return null;
        }

        // 查找是否已存在
        for (DocumentFile child : parent.listFiles()) {
            if (!child.isDirectory() && child.getName() != null && child.getName().equals(fileName)) {
                return child;
            }
        }

        // 如果未指定 MIME 类型，尝试自动推断
        if (mimeType == null || mimeType.isEmpty()) {
            mimeType = getMimeType(fileName);
        }

        // 不存在则创建
        return parent.createFile(mimeType, fileName);
    }

    /**
     * 根据文件名推断 MIME 类型
     */
    public static String getMimeType(String fileName) {
        if (fileName == null) return "application/octet-stream";

        String extension = "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            extension = fileName.substring(lastDot + 1).toLowerCase();
        }

        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    /**
     * 递归创建目录的完整路径
     * 基于树根 Uri 和相对路径
     *
     * @param context 上下文
     * @param treeUri 树根 Uri
     * @param relativePath 相对路径
     * @return 创建后的 DocumentFile
     * @throws IOException 如果创建失败
     */
    public static DocumentFile ensureDirectoryRecursive(Context context, Uri treeUri, String relativePath) throws IOException {
        if (treeUri == null) {
            throw new IOException("Tree URI is null");
        }
        if (relativePath == null || relativePath.isEmpty()) {
            return DocumentFile.fromTreeUri(context, treeUri);
        }

        DocumentFile root = DocumentFile.fromTreeUri(context, treeUri);
        if (root == null) {
            throw new IOException("Cannot access tree root");
        }

        return ensureDirectoryRecursiveInternal(root, relativePath);
    }

    private static DocumentFile ensureDirectoryRecursiveInternal(DocumentFile parent, String relativePath) throws IOException {
        if (relativePath == null || relativePath.isEmpty()) {
            return parent;
        }

        // 分割路径
        String[] segments = relativePath.split("/");
        DocumentFile current = parent;

        for (String segment : segments) {
            if (segment.isEmpty()) continue;

            // 查找子目录
            DocumentFile child = findChild(null, current, segment, true);
            if (child == null) {
                // 创建子目录
                child = current.createDirectory(segment);
                if (child == null) {
                    throw new IOException("Failed to create directory: " + segment);
                }
            }
            current = child;
        }

        return current;
    }

    /**
     * 获取父目录路径
     *
     * @param file 文件或目录
     * @return 父目录的字符串表示
     */
    public static String getParentPath(DocumentFile file) {
        if (file == null) return null;
        DocumentFile parent = file.getParentFile();
        if (parent == null) return null;
        return parent.getName();
    }

    /**
     * 获取完整的路径字符串（从根开始）
     *
     * @param file DocumentFile 对象
     * @return 路径字符串，如 "/home/user/file.txt"
     */
    public static String getFullPath(DocumentFile file) {
        if (file == null) return null;

        List<String> segments = new ArrayList<>();
        DocumentFile current = file;

        while (current != null) {
            String name = current.getName();
            if (name != null && !name.isEmpty()) {
                segments.add(0, name);
            }
            current = current.getParentFile();
        }

        return "/" + String.join("/", segments);
    }
}