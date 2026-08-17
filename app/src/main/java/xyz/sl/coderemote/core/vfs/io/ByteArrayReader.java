package xyz.sl.coderemote.core.vfs.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import xyz.sl.coderemote.core.vfs.Resource;


/**
 * ByteArrayReader 的作用是：将整个资源作为 byte[] 读取。它本质上是“全部读出”。<br>
 * 如果“不想一次全部读出”，就不应该使用 ByteArrayReader。
 */
public class ByteArrayReader implements ResourceReader<byte[]> {
    private static final String tag = "ByteArrayReader";
    private final long maxSize;

    public ByteArrayReader() {
        this(10 * 1024 * 1024); // 默认限制 10MB
    }

    public ByteArrayReader(long maxSize) {
        if (maxSize <= 0) throw new IllegalArgumentException("maxSize must be positive");
        this.maxSize = maxSize;
    }

    @Override
    public byte[] read(Resource resource) throws IOException {
        if (resource == null) {
            throw new IllegalArgumentException("resource is null");
        }
        if (resource.isDirectory()) {
            throw new IOException("Cannot read from a directory: " + resource.getUri());
        }

        // 预分配容量（如果有大小信息）
        long size = resource.getSize();
        try (
                InputStream inputStream = resource.openInputStream();
                ByteArrayOutputStream outputStream = (size > 0 && size <= Integer.MAX_VALUE) ?
                     new ByteArrayOutputStream((int) size) :
                     new ByteArrayOutputStream();
        ) {
            if (inputStream == null) {
                throw new IOException("openInputStream() returned null for: " + resource.getUri());
            }

            byte[] buffer = new byte[8192];
            int len;
            long total = 0;
            while ((len = inputStream.read(buffer)) != -1) {
                total += len;
                if (total > maxSize) {
                    throw new IOException("File size exceeds limit: " + maxSize + " bytes");
                }
                outputStream.write(buffer, 0, len);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            // 可选：记录日志（但使用java.util.logging）
            // Logger.getLogger(TAG).log(Level.WARNING, "Read failed", e);
            throw e; // 直接抛出
        }
    }


}
