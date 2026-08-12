package xyz.sl.coderemote.core.vfs.cache;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import xyz.sl.coderemote.core.vfs.LocalResource;
import xyz.sl.coderemote.core.vfs.Resource;

public class SftpCache implements Cache{

    private final DocumentFile root;

    public SftpCache(Context context) {
        root = DocumentFile.fromFile(new File(context.getExternalCacheDir(), "ssh"));
    }

    /**
     * 获取指定 SSH 连接的缓存根目录。
     * file://[auth]/xxx/ssh/[connectionId]
     */
    public Uri getConnectionRoot(String connectionId) {
        return root.findFile(connectionId).getUri();
    }

    public Uri createConnectionRoot(String connectionId){
        return root.createDirectory(connectionId).getUri();
    }

    /**
     * 将远程路径映射为本地缓存文件。
     * <p>
     * 例如：
     * <p>
     * connectionId:
     * 0b6a95dc-eb27-485e-a105-7401553a6808
     * <p>
     * remotePath:
     * /C:/Users/jocker/Desktop/a.txt
     * <p>
     * 返回：
     * [cacheDir]/ssh/0b6a95dc-eb27-485e-a105-7401553a6808/C:/Users/jocker/Desktop/a.txt
     */
    public Uri getCachedResource(String connectionId, String remotePath) {
        String relativePath = remotePath;

        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        return getConnectionRoot(connectionId).buildUpon()
                .appendPath(remotePath)     // expected file://[auth]/xxx/ssh/[connectionId]/remotePath
                .build();
    }

    /**
     * 获取远程目录对应的本地缓存目录。
     */
    public Uri getDirectory(String connectionId, String remotePath) {
        return getCachedResource(connectionId, remotePath);
    }

    /**
     * 创建缓存目录。
     */
    public File ensureDirectory(String connectionId, String remotePath) {
        Uri directoryUri = getDirectory(connectionId, remotePath);
        File directory = new File(URI.create(directoryUri.toString()));

        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Cannot create cache directory: " + directory);
        }

        return directory;
    }

    public Resource getLocalMapResource(Uri uri){
        String connectionId = uri.getAuthority();
        String remotePath = uri.getPath();
        return LocalResource.emptyLocalResource();
    }

    @Override
    public Resource getCachedResource(Resource remoteResource) {
        return null;
    }

    @Override
    public boolean isCached(Resource remoteResource) {
        return false;
    }

    @Override
    public Resource cache(Resource remoteResource) throws IOException {
        return null;
    }
}
