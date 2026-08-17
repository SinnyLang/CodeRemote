package xyz.sl.coderemote.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import xyz.sl.coderemote.core.FileNode;

/**
 * BaseFileManager 提供对本地文件的列出、读、写、增、删、重命名操作
 */
public abstract class BaseFileManager {
    protected final Context context;
    protected final File cacheDir;
    protected final File fileDir;

    public BaseFileManager(Context context) {
        this.context = context.getApplicationContext();
        this.cacheDir = new File(context.getExternalCacheDir(), "file_cache");
        this.fileDir = new File(context.getExternalFilesDir(null), "remote_projects");
        // 预览时，初始化BaseFileManager中创建文件夹会导致预览失效
//        if (!cacheDir.exists()) {
//            cacheDir.mkdirs();
//        }
//        if (!fileDir.exists()) {
//            fileDir.mkdirs();
//        }
    }

    public CacheEditFile getCacheFileEmpty() throws IOException {
        return CacheEditFile.empty(context);
    }

    public CacheEditFile getCacheFile(Uri uri) throws IOException {
        return CacheEditFile.fromUri(context, uri);
    }

    /**
     * 从 Uri 中获取文件名
     */
    @Deprecated
    public String getFileNameFromUri(Uri uri){
        String result = "UnKnownFile";

        if (uri == null){
            return "";
        }

        // 1. 处理 file:// 协议
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            String path = uri.getPath();
            if (path != null) {
                result = new File(path).getName();
            }
        }

        // 2. 处理 content:// 协议
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[]{OpenableColumns.DISPLAY_NAME},  // 只查询需要的列
                    null,
                    null,
                    null
            )) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (SecurityException | IllegalArgumentException e) {
                // 权限问题或非法 URI，返回 null
                result = "";
            }
        }

        return result; // 未知类型
    }

    @Deprecated
    public File getFile(Uri uri){
        return null;
    }



    // ---------- 缓存工具方法 ----------
    protected File getCachedFile(String remotePath) {
        String hash = Integer.toHexString(remotePath.hashCode());
        String fileName = remotePath.substring(remotePath.lastIndexOf('/') + 1);
        return new File(cacheDir, hash + "_" + fileName);
    }

    // 读本地缓存文件
    protected String readTextFromCache(File file) throws IOException {
        return new String(readBytesFromFile(file), StandardCharsets.UTF_8);
    }

    protected void writeTextToCache(File file, String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    protected byte[] readBytesFromFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }
    }

    // ---------- 通用文件属性 ----------
    @Deprecated
    public static class FileAttributes {
        public final long size;
        public final long lastModified;
        public final boolean isDirectory;

        public FileAttributes(long size, long lastModified, boolean isDirectory) {
            this.size = size;
            this.lastModified = lastModified;
            this.isDirectory = isDirectory;
        }
    }

    // ---------- 抽象方法（子类实现） ----------
    @Deprecated
    public FileAttributes getAttributes(String path) throws IOException{
        throw new IOException("implement of this method is planning");
    }
    // 可选：创建、删除、重命名

    //===========list File===================
    @Deprecated
    public @NotNull List<? extends @NotNull FileNode> scanDirectory(@NotNull Uri rootUri) {
        return List.of(UriUtils.INSTANCE.uriToFileNode(context, rootUri));
    }

    @Deprecated
    public @NotNull List<? extends @NotNull FileNode> listDirectory(@NotNull Uri rootUri) {


        return List.of(UriUtils.INSTANCE.uriToFileNode(context, rootUri));
    }

    //===========read========================
    /**
     * 从 Uri 读取文本内容，完全保留原始换行符（CRLF / LF / CR）。
     * 内部复用字节读取逻辑，适用于文本文件。
     *
     * @param uri     文件 Uri
     * @param charset 字符集（默认 UTF-8）
     * @return 文件内容字符串，若出错返回 null
     */
    public String readTextFromUri(Uri uri, Charset charset) {
        try {
            byte[] data = readBytesFromUri(context, uri);
            if (data == null) throw new NullPointerException("result of \"readBytesFromUri(Context, Uri)\" is null");
            return new String(data, charset);
        } catch (Exception e){
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw, true));
            return "文件读取失败\n" + sw.getBuffer().toString();
        }
    }

    // 重载：默认 UTF-8
    public @NotNull String readTextFromUri(Uri uri) {
        return readTextFromUri(uri, StandardCharsets.UTF_8);
    }

    /**
     * 从 Uri 读取字节数组（适用于二进制文件）
     */
    public static byte[] readBytesFromUri(Context context, Uri uri) throws IOException {
        if (context == null)
            throw new RuntimeException("Code Remote EditFileManger is not initial");
        if (uri == null)
            throw new IllegalArgumentException("uri is null");

        ContentResolver resolver = context.getContentResolver();
        try (InputStream inputStream = resolver.openInputStream(uri)) {
            if (inputStream == null) {
                return null;
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            return outputStream.toByteArray();
        } catch (IOException | SecurityException | IllegalArgumentException e) {
            Log.e("EditFileManger", e.getMessage());
            throw e;
        }
    }

    //===========write======================
    /**
     * 将字节数组写入指定的 Uri（覆盖原有内容）。
     *
     * @param context 上下文
     * @param uri     目标 Uri（须有写入权限）
     * @param data    要写入的字节数据
     * @throws IOException 如果写入失败（权限、磁盘错误等）
     */
    public static void writeBytesToUri(Context context, Uri uri, byte[] data) throws IOException {
        if (context == null) {
            throw new RuntimeException("Code Remote EditFileManger is not initial");
        }
        if (uri == null) {
            throw new IllegalArgumentException("uri is null");
        }
        if (data == null) {
            throw new IllegalArgumentException("data is null");
        }

        ContentResolver resolver = context.getContentResolver();
        try (OutputStream outputStream = resolver.openOutputStream(uri)) {
            if (outputStream == null) {
                throw new IOException("Failed to open output stream for URI: " + uri);
            }
            outputStream.write(data);
            outputStream.flush();
        } catch (IOException | SecurityException | IllegalArgumentException e) {
            Log.e("EditFileManger", "Write bytes error: " + e.getMessage());
            throw e; // 向上传递
        }
    }

    /**
     * 将字符串写入指定的 Uri（指定字符集），覆盖原有内容。
     *
     * @param uri     目标 Uri
     * @param text    要写入的文本
     * @param charset 字符集（如 StandardCharsets.UTF_8）
     * @throws IOException 如果写入失败
     */
    public void writeTextToUri(Uri uri, String text, Charset charset) throws IOException {
        if (context == null) {
            throw new RuntimeException("Code Remote EditFileManger is not initial");
        }
        if (uri == null) {
            throw new IllegalArgumentException("uri is null");
        }
        if (text == null) {
            throw new IllegalArgumentException("text is null");
        }
        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }
        byte[] data = text.getBytes(charset);
        writeBytesToUri(context, uri, data); // 复用静态方法
    }

    /**
     * 将字符串写入指定的 Uri（默认 UTF-8）。
     */
    public void writeTextToUri(Uri uri, String text) throws IOException {
        writeTextToUri(uri, text, StandardCharsets.UTF_8);
    }

    //===========create======================
    /**
     * 在指定目录下创建新文件
     * @param parentUri 父目录Uri
     * @param fileName 文件名
     * @return 创建的文件Uri
     * @throws IOException 创建失败时抛出
     */
    @Deprecated
    public Uri createFile(Uri parentUri, String fileName) throws IOException {
        if (context == null) {
            throw new RuntimeException("Code Remote EditFileManger is not initial");
        }
        if (parentUri == null) {
            throw new IllegalArgumentException("Parent Uri cannot be null");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }
        if (!isValidFileName(fileName)) {
            throw new IllegalArgumentException("Invalid file name: " + fileName);
        }

        String scheme = parentUri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            // file:// 协议
            File parentDir = new File(parentUri.getPath());
            if (!parentDir.exists() || !parentDir.isDirectory()) {
                throw new IOException("Parent directory does not exist: " + parentDir.getPath());
            }
            File newFile = new File(parentDir, fileName);
            if (newFile.exists()) {
                throw new IOException("File already exists: " + newFile.getName());
            }
            if (!newFile.createNewFile()) {
                throw new IOException("Failed to create file: " + newFile.getName());
            }
            return Uri.fromFile(newFile);
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            // content:// 协议（使用 DocumentFile）
            DocumentFile parentDoc = DocumentFile.fromTreeUri(context, parentUri);
            if (parentDoc == null || !parentDoc.exists() || !parentDoc.isDirectory()) {
                throw new IOException("Parent document is not a valid directory");
            }
            DocumentFile newDoc = parentDoc.createFile("*/*", fileName);
            if (newDoc == null) {
                throw new IOException("Failed to create file: " + fileName);
            }
            return newDoc.getUri();
        } else {
            throw new IOException("Unsupported URI scheme: " + scheme);
        }
    }

    /**
     * 在指定目录下创建新目录
     * @param parentUri 父目录Uri
     * @param dirName 目录名
     * @return 创建的目录Uri
     * @throws IOException 创建失败时抛出
     */
    @Deprecated
    public Uri createDirectory(Uri parentUri, String dirName) throws IOException {
        if (context == null) {
            throw new RuntimeException("Code Remote EditFileManger is not initial");
        }
        if (parentUri == null) {
            throw new IllegalArgumentException("Parent Uri cannot be null");
        }
        if (dirName == null || dirName.trim().isEmpty()) {
            throw new IllegalArgumentException("Directory name cannot be empty");
        }
        if (!isValidFileName(dirName)) {
            throw new IllegalArgumentException("Invalid directory name: " + dirName);
        }

        String scheme = parentUri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            // file:// 协议
            File parentDir = new File(parentUri.getPath());
            if (!parentDir.exists() || !parentDir.isDirectory()) {
                throw new IOException("Parent directory does not exist: " + parentDir.getPath());
            }
            File newDir = new File(parentDir, dirName);
            if (newDir.exists()) {
                throw new IOException("Directory already exists: " + newDir.getName());
            }
            if (!newDir.mkdir()) {
                throw new IOException("Failed to create directory: " + newDir.getName());
            }
            return Uri.fromFile(newDir);
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            // content:// 协议（使用 DocumentFile）
            DocumentFile parentDoc = DocumentFile.fromTreeUri(context, parentUri);
            if (parentDoc == null || !parentDoc.exists() || !parentDoc.isDirectory()) {
                throw new IOException("Parent document is not a valid directory");
            }
            DocumentFile newDoc = parentDoc.createDirectory(dirName);
            if (newDoc == null) {
                throw new IOException("Failed to create directory: " + dirName);
            }
            return newDoc.getUri();
        } else {
            throw new IOException("Unsupported URI scheme: " + scheme);
        }
    }

    //============== delete ============================================
    /**
     * 删除文件
     * @param uri 文件Uri
     * @throws IOException 删除失败时抛出
     */
    @Deprecated
    public void deleteFile(Uri uri) throws IOException {
        if (context == null) {
            throw new RuntimeException("Code Remote EditFileManger is not initial");
        }
        if (uri == null) {
            throw new IllegalArgumentException("Uri cannot be null");
        }

        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            // file:// 协议
            File file = new File(uri.getPath());
            if (!file.exists()) {
                throw new IOException("File does not exist: " + file.getPath());
            }
            if (!file.delete()) {
                throw new IOException("Failed to delete file: " + file.getName());
            }
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            // content:// 协议（使用 DocumentFile）
            DocumentFile documentFile = DocumentFile.fromTreeUri(context, uri);
            if (documentFile == null || !documentFile.exists()) {
                throw new IOException("Document file not found: " + uri);
            }
            if (!documentFile.delete()) {
                throw new IOException("Failed to delete document: " + uri);
            }
        } else {
            throw new IOException("Unsupported URI scheme: " + scheme);
        }
    }

    /**
     * 删除目录（递归删除）
     * @param uri 目录Uri
     * @throws IOException 删除失败时抛出
     */
    @Deprecated
    public void deleteDirectory(Uri uri) throws IOException {
        if (context == null) {
            throw new RuntimeException("Code Remote EditFileManger is not initial");
        }
        if (uri == null) {
            throw new IllegalArgumentException("Uri cannot be null");
        }

        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            // file:// 协议 - 递归删除
            File dir = new File(uri.getPath());
            if (!dir.exists()) {
                throw new IOException("Directory does not exist: " + dir.getPath());
            }
            if (!dir.isDirectory()) {
                throw new IOException("Not a directory: " + dir.getPath());
            }
            deleteDirectoryRecursive(dir);
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            // content:// 协议（使用 DocumentFile）
            DocumentFile documentFile = DocumentFile.fromTreeUri(context, uri);
            if (documentFile == null || !documentFile.exists()) {
                throw new IOException("Document not found: " + uri);
            }
            if (!documentFile.isDirectory()) {
                throw new IOException("Not a directory: " + uri);
            }
            if (!deleteDocumentRecursive(documentFile)) {
                throw new IOException("Failed to delete directory: " + uri);
            }
        } else {
            throw new IOException("Unsupported URI scheme: " + scheme);
        }
    }

    /**
     * 递归删除目录（针对 file:// 协议）
     */
    private void deleteDirectoryRecursive(File dir) throws IOException {
        if (dir == null || !dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryRecursive(file);
                } else {
                    if (!file.delete()) {
                        throw new IOException("Failed to delete file: " + file.getPath());
                    }
                }
            }
        }
        if (!dir.delete()) {
            throw new IOException("Failed to delete directory: " + dir.getPath());
        }
    }

    /**
     * 递归删除文档目录（针对 content:// 协议）
     */
    private boolean deleteDocumentRecursive(DocumentFile documentFile) {
        if (documentFile == null) {
            return false;
        }
        if (documentFile.isDirectory()) {
            DocumentFile[] children = documentFile.listFiles();
            boolean allDeleted = true;
            for (DocumentFile child : children) {
                if (!deleteDocumentRecursive(child)) {
                    allDeleted = false;
                }
            }
            return allDeleted && documentFile.delete();
        } else {
            return documentFile.delete();
        }
    }

    //============== rename ============================================
    /**
     * 重命名文件
     * @param uri 文件Uri
     * @param newName 新文件名
     * @throws IOException 重命名失败时抛出
     */
    @Deprecated
    public void renameFile(Uri uri, String newName) throws IOException {
        if (context == null) {
            throw new RuntimeException("Code Remote EditFileManger is not initial");
        }
        if (uri == null) {
            throw new IllegalArgumentException("Uri cannot be null");
        }
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("New name cannot be empty");
        }

        // 验证文件名合法性
        if (!isValidFileName(newName)) {
            throw new IllegalArgumentException("Invalid file name: " + newName);
        }

        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            // file:// 协议
            File file = new File(uri.getPath());
            File parent = file.getParentFile();
            if (parent == null) {
                throw new IOException("Cannot get parent directory");
            }
            File newFile = new File(parent, newName);
            if (!file.renameTo(newFile)) {
                throw new IOException("Failed to rename file: " + file.getName() + " -> " + newName);
            }
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            // content:// 协议（使用 DocumentFile）
            DocumentFile documentFile = DocumentFile.fromTreeUri(context, uri);
            if (documentFile == null || !documentFile.exists()) {
                throw new IOException("Document file not found: " + uri);
            }
            if (!documentFile.renameTo(newName)) {
                throw new IOException("Failed to rename document: " + uri);
            }
        } else {
            throw new IOException("Unsupported URI scheme: " + scheme);
        }
    }

    /**
     * 重命名目录
     * @param uri 目录Uri
     * @param newName 新目录名
     * @throws IOException 重命名失败时抛出
     */
    @Deprecated
    public void renameDirectory(Uri uri, String newName) throws IOException {
        // 目录重命名与文件重命名逻辑相同
        renameFile(uri, newName);
    }

    //==============
    /**
     * 验证文件名是否合法
     */
    private boolean isValidFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }
        // Windows/Linux 非法字符：/ \ : * ? " < > |
        // Android 额外禁止的一些字符
        String illegalChars = "[\\\\/:*?\"<>|]";
        return !fileName.matches(".*" + illegalChars + ".*");
    }

    /**
     * 检查目录是否为空
     */
    @Deprecated
    public boolean isDirectoryEmpty(Uri uri) throws IOException {
        if (uri == null) {
            throw new IllegalArgumentException("Uri cannot be null");
        }

        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            File dir = new File(uri.getPath());
            if (!dir.exists() || !dir.isDirectory()) {
                throw new IOException("Not a valid directory: " + uri);
            }
            File[] files = dir.listFiles();
            return files == null || files.length == 0;
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            DocumentFile documentFile = DocumentFile.fromTreeUri(context, uri);
            if (documentFile == null || !documentFile.exists() || !documentFile.isDirectory()) {
                throw new IOException("Not a valid document directory: " + uri);
            }
            DocumentFile[] children = documentFile.listFiles();
            return children == null || children.length == 0;
        } else {
            throw new IOException("Unsupported URI scheme: " + scheme);
        }
    }

}
