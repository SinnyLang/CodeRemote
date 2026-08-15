package xyz.sl.coderemote.core.vfs.cache;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 负责提取缓存 SFTP 文件的 Uri。
 * 1. 获取缓存根目录。
 * 2. 获取某个sftp连接的缓存根目录。
 * 3. 负责转换文件、目录 Uri: 远程真实Uri <--> 本地缓存Uri
 */
public class SftpCacheTranslate {
    private final static String tag = "SFTP-cache-translate";

    private final File root;

    /**
     * 根目录。
     * file://[auth]/xxx/ssh
     */
    public SftpCacheTranslate(Context context) {
        root = new File(context.getExternalCacheDir(), "ssh");
    }

    /**
     * 获取指定 SSH 连接的缓存根目录。
     * file://[auth]/xxx/ssh/[connectionId]
     */
    public Uri getConnectionRootUri(String connectionId) {
        File file = new File(root, connectionId);
        return file.exists() ? Uri.fromFile(file) : null;
    }

    private File getConnectionRootFile(String connectionId) {
        File file = new File(root, connectionId);
        return file.exists() ? file : null;
    }


    public Uri createConnectionRoot(String connectionId){
        File connectionDir = new File(root, connectionId);
        if (!connectionDir.exists()) {
            if (!connectionDir.mkdirs()) {
                return null;
            }
        }
        return Uri.fromFile(connectionDir);
    }

    /**
     * 将远程相对路径映射为本地缓存文件。相对于工作目录。
     * <p>
     * 例如：
     * <p>
     * connectionId:
     * 0b6a9...a6808
     * <p>
     * relativePath: Desktop/a.txt
     * <p>
     * 返回：
     * [cacheDir]/ssh/0b6a9...a6808/Desktop/a.txt
     */
    public Uri getCacheResourceUri(String connectionId, String relativePath) {
        String _relativePath = relativePath;
        if (_relativePath.startsWith("/")) {
            _relativePath = relativePath.substring(1);
        }

        Uri connectionRootUri = getConnectionRootUri(connectionId);
        if (connectionRootUri == null){
            connectionRootUri = createConnectionRoot(connectionId);
        }
        return connectionRootUri.buildUpon()
                // expected file://[auth]/xxx/ssh/[connectionId]/_relativePath
                .appendPath(_relativePath)
                .build();
    }

    private File getCacheResourceFile(String connectionId, String relativePath){
        File connectionRootFile = getConnectionRootFile(connectionId);
        if (connectionRootFile == null)
            return null;

        File file = new File(connectionRootFile, relativePath);
        Log.d(tag, "getCacheResourceFile "+file.getAbsolutePath());
        return file;
    }

    /**
     * 将远程路径映射为本地缓存文件。
     * <p>
     * 例如：
     * <p>
     * connectionId:
     * 0b6a9...a6808
     * <p>
     * remoteWorkdir: /C:/Users/jocker/Desktop
     * <p>
     * remotePath:    /C:/Users/jocker/Desktop/a.txt
     * <p>
     * 返回：
     * file:///xxx/ssh/0b6a9...a6808/Desktop/a.txt
     */
    public Uri getCacheResourceUri(String connectedId, String remoteWorkdir,
                                   String remotePath){
        String relativePath = calcRelativePath(remoteWorkdir, remotePath);
        return getCacheResourceUri(connectedId, relativePath);
    }

    private File getCacheResourceFile(String connectedId, String remoteWorkdir,
                                   String remotePath){
        String relativePath = calcRelativePath(remoteWorkdir, remotePath);
        return getCacheResourceFile(connectedId, relativePath);
    }

    /** 计算文件与工作目录父目录的相对路径，例如：
     * @param remoteWorkdir /C:/Users/jocker/Desktop
     * @param remotePath /C:/Users/jocker/Desktop/a.txt
     * @return Desktop/a.txt
     */
    public static String calcRelativePath(String remoteWorkdir, String remotePath) {
        // 标准化路径
        Path workDir = Paths.get(remoteWorkdir).normalize();
        Path file = Paths.get(remotePath).normalize();
        // 计算相对路径
        Path relativePath = workDir.relativize(file);
        String result = relativePath.toString();

        // 相对路径出现 ".." 表示出现了目录越级，越到了工作目录之外，抛出异常
        if(result.contains(".."))
            throw new RuntimeException("目录越级：" +
                    "remoteWorkdir=" + remoteWorkdir +
                    ", but remotePath=" + remotePath
            );

        // 如果相对路径不为空，返回：工作目录名称+相对路径
        if (!result.isEmpty()) {
            // 统一使用正斜杠
            String workdirName = workDir.getFileName().toString();
            return workdirName + "/" + result.replace('\\', '/');
        }

        // 如果相对路径为空（同一文件），返回文件名
        return file.getFileName().toString();
    }
}
