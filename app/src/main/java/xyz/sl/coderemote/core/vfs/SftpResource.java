package xyz.sl.coderemote.core.vfs;

import android.net.Uri;
import android.util.Log;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import xyz.sl.coderemote.core.remote.SshClient;
import xyz.sl.coderemote.core.vfs.cache.Cache;

/**
 * 表示远程主机和本地存在的文件，一个 SftpResource 同时指向这两个文件资源
 * 当本地 Resource 做出更改时，将相应的操作同步至远程主机
 */
public class SftpResource implements DirectoryResource, Cache {
    private final static String tag = "SftpResource";

    // remote uri:
    // such as baseDir = sftp://[auth]/C:/Users/jocker/Desktop
    // such as file    = sftp://[auth]/C:/Users/jocker/Desktop/a.txt

    // 映射到本地文件
    // local cache uri:
    // such as baseDir = file:///[cacheDir]/ssh/[connectId]/Desktop
    // such as file    = file:///[cacheDir]/ssh/[connectId]/Desktop/a.txt

    private final LocalResource localMappedResource;    // 本地缓存文件，与远程文件相对应
    private final Uri remoteUri;                        // 标识SftpResource的唯一Uri
    private final SshClient connection;                 // 远程连接
    private final SftpATTRS attrs;                      // 文件属性

    private final AtomicBoolean isCaching = new AtomicBoolean(false);

    public SftpResource(
            LocalResource localMappedResource,
            SshClient connection,
            Uri remoteUri,
            SftpATTRS attrs
    ) {
        this.localMappedResource = localMappedResource;
        this.connection = connection;
        this.remoteUri = remoteUri;
        this.attrs = attrs;
    }

    @Override
    public Uri getUri() {
        return remoteUri;
    }

    @Override
    public String getName() {
        return localMappedResource.getName();
    }

    //  /C:/Users/jocker/Desktop
    //  /C:/Users/jocker/Desktop/a.txt
    @Override
    public String getPath() {
        return remoteUri.getPath();
    }

    @Override
    public ResourceType getType() {
        return attrs.isDir() ? ResourceType.DIRECTORY : ResourceType.FILE;
    }

    @Override
    public long getSize() {
        return attrs.getSize();
    }

    @Override
    public long getLastModified() {
        return attrs.getMTime() * 1000L;
    }

    @Override
    public boolean exists() {
        try {
            connection.getSftpChannel().stat(remoteUri.getPath());
            return true;
        } catch (SftpException e) {
            return false;
        }
    }

    @Override
    public boolean delete() throws RuntimeException {
        try {
            if (attrs.isDir()) {
                connection.getSftpChannel().rmdir(remoteUri.getPath());
            } else {
                connection.getSftpChannel().rm(remoteUri.getPath());
            }
            return true;
        } catch (SftpException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Resource rename(String newName) throws IOException {
        // /C:/Users/jocker/Desktop/a.txt
        String path = getPath();

        // /C:/Users/jocker/Desktop
        String parentPath = path.substring(0, path.lastIndexOf('/'));

        // /C:/Users/jocker/Desktop/newName.txt
        String newPath = parentPath + "/" + newName;

        try {
            connection.getSftpChannel().rename(getPath(), newPath); //oldPath newPath
            return resolve(newPath);
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public InputStream openInputStream() throws IOException {
        try {
            return connection.getSftpChannel().get(getPath());
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }
    @Override
    public OutputStream openOutputStream() throws IOException {
        try {
            return connection.getSftpChannel().put(getPath());
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean isDirectory() {
        return attrs.isDir();
    }

    @Override
    public List<Resource> list() throws IOException {
        List<Resource> result = new ArrayList<>();

        try {
            Vector<ChannelSftp.LsEntry> entries = connection.getSftpChannel().ls(getPath());
            Log.d(tag, "列出子文件");
            for (ChannelSftp.LsEntry entry : entries) {
                String name = entry.getFilename();
                if (".".equals(name) || "..".equals(name)) continue;

                Uri childUri = remoteUri.buildUpon().appendPath(name).build();

                result.add(
                        new SftpResource(
                                localMappedResource.resolve(name),
                                connection,
                                childUri,
                                entry.getAttrs()
                        )
                );
                Log.d(tag, "列出子文件："+name);
            }

            return result;
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    protected SftpResource resolve(String name) throws IOException {
        try {
            SftpATTRS attrs = connection.getSftpChannel().stat(getPath()+"/"+name);
            return new SftpResource(
                    localMappedResource.resolve(name),
                    connection,
                    remoteUri.buildUpon().appendPath(name).build(),
                    attrs
            );
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Resource createFile(String name) throws IOException {
        try {
            OutputStream output = connection.getSftpChannel().put(name);
            output.close();

            return resolve(name);
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Resource createDirectory(String name) throws IOException {
        String newPath = getPath() + "/" + name;
        try {
            connection.getSftpChannel().mkdir(newPath);

            return resolve(newPath);
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Resource ensureParentDirectory() {
        localMappedResource.ensureParentDirectory();
        // /C:/Users/jocker/Desktop/a.txt
        String path = getPath();

        // /C:/Users/jocker/Desktop
        String parentPath = path.substring(0, path.lastIndexOf('/'));

        try {
            SftpATTRS attrs = connection.getSftpChannel().stat(parentPath);
            return new SftpResource(
                    (LocalResource) localMappedResource.ensureParentDirectory(),
                    connection,
                    remoteUri.buildUpon().path(parentPath).build(),
                    attrs
            );
        } catch (SftpException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 判断缓存是否有效（检查远程文件是否更新）
     */
    @Override
    public boolean isCacheValid() {
        if (!isCached()) {
            return false;
        }

        // 比较文件大小和修改时间
        long localSize = localMappedResource.getSize();
        long localModified = localMappedResource.getLastModified();

        // 如果远程文件大小或修改时间不同，缓存无效
        return getSize() == localSize
                && Math.abs(getLastModified() - localModified) < 2000; // 允许 2 秒误差
    }

    /**
     * 判断本地映射的缓存文件是否存在
     */
    @Override
    public boolean isCached() {
        return localMappedResource.exists();
    }

    /**
     * 将远程主机的文件缓存到本地映射文件中。
     * @return
     * @throws IOException
     */
    @Override
    public Uri cache() throws IOException {
        // 缓存有效则返回缓存
        if (isCacheValid())
            return localMappedResource.getUri();

        // 并发写入则抛出异常
        if (!isCaching.compareAndSet(false, true))
            throw new IOException("Cache operation already in progress");

        // 保证父目录存在
        localMappedResource.ensureParentDirectory();

        try (
                InputStream inputStream = openInputStream();
                OutputStream outputStream = localMappedResource.openOutputStream()
        ) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;

                // 可选：进度回调
                if (totalBytes % (1024 * 1024) == 0) {
                    // 每 1MB 记录一次进度
                    notifyProgress(totalBytes);
                }
            }

            outputStream.flush();
        }

        return localMappedResource.getUri();
    }

    /**
     * 创建临时文件
     */
    private File createTempFile() throws IOException {
        String prefix = "cache_";
        String suffix = ".tmp";
        return File.createTempFile(prefix, suffix);
    }

    /**
     * 下载到临时文件
     */
    private void downloadToFile(File targetFile) throws IOException {
        try (InputStream inputStream = openInputStream();
             OutputStream outputStream = new FileOutputStream(targetFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;

                // 可选：进度回调
                if (totalBytes % (1024 * 1024) == 0) {
                    // 每 1MB 记录一次进度
                    notifyProgress(totalBytes);
                }
            }

            outputStream.flush();
        }
    }

    /**
     * 进度监听器（可选）
     */
    public interface ProgressListener {
        void onProgress(long bytesDownloaded, long totalBytes);
    }

    private ProgressListener progressListener;

    public void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }

    private void notifyProgress(long bytesDownloaded) {
        if (progressListener != null) {
            long total = getSize();
            progressListener.onProgress(bytesDownloaded, total);
        }
    }

    /**
     * 异步缓存（不阻塞主线程）
     */
//    public void cacheAsync(CacheCallback callback) {
//        new Thread(() -> {
//            try {
//                Uri uri = cache();
//                if (callback != null) {
//                    callback.onSuccess(uri);
//                }
//            } catch (IOException e) {
//                if (callback != null) {
//                    callback.onError(e);
//                }
//            }
//        }).start();
//    }
}
