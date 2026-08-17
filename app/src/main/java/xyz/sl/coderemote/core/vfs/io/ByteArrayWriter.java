package xyz.sl.coderemote.core.vfs.io;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;

import xyz.sl.coderemote.core.vfs.Resource;

/**
 * ByteArrayWriter 的作用是：将整个资源作为 byte[] 写入。它本质上是“全部写入”。<br>
 * 如果“不想一次全部写入”，就不应该使用 ByteArrayWriter。
 */
public class ByteArrayWriter implements ResourceWriter<byte[]>{
    private static final String tag = "ByteArrayWriter";

    @Override
    public void write(Resource resource, byte[] data) throws IOException {
        if (resource == null) {
            throw new IllegalArgumentException("resource is null");
        }
        if (resource.isDirectory()) {
            throw new IOException("Cannot write to a directory: " + resource.getUri());
        }
        if (data == null) {
            throw new IllegalArgumentException("data is null");
        }

        // 确保父目录存在，此代码会导致将数据写入已删除的文件
        // resource.ensureParentDirectory();

        try (OutputStream outputStream = resource.openOutputStream()) {
            if (outputStream == null) {
                throw new IOException("Failed to open output stream for URI: " + resource.getUri());
            }
            outputStream.write(data);
            outputStream.flush();
        } catch (IOException e) {
//            Log.e(tag, "Write bytes error: " + e.getMessage());
            throw e; // 向上传递
        }
    }
}

