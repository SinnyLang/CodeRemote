package xyz.sl.coderemote.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class CacheEditFile {

    private final Context context;
    private final Uri sourceUri;      // 可能为 null
    private final File cacheFile;

    private CacheEditFile(Context context, Uri sourceUri, File cacheFile) {
        this.context = context.getApplicationContext();
        this.sourceUri = sourceUri;
        this.cacheFile = cacheFile;
        Log.i("CacheEditFile", "sourceUri="+((sourceUri == null ? null : sourceUri.toString())));
        Log.i("CacheEditFile", "cacheFile="+cacheFile.getPath());
    }

    /** 从 Uri 创建可编辑缓存 */
    public static CacheEditFile fromUri(Context context, Uri uri) throws IOException {
        File cache = createTempCache(context);
        copyUriToFile(context, uri, cache);
        return new CacheEditFile(context, uri, cache);
    }

    /** 仅创建一个空缓存文件 */
    public static CacheEditFile empty(Context context) throws IOException {
        File cache = createTempCache(context);
        return new CacheEditFile(context, null, cache);
    }

    public File getCacheFile() {
        return cacheFile;
    }

    /** 提交修改：写回原 Uri */
    public void commit() throws IOException {
        if (sourceUri == null) {
            throw new IllegalStateException("No source Uri to commit to");
        }
        copyFileToUri(context, cacheFile, sourceUri);
        cleanup();
    }

    /** 保存为新的 Uri（比如 SAF 选择的目标） */
    public void saveAs(Uri targetUri) throws IOException {
        copyFileToUri(context, cacheFile, targetUri);
    }

    /** 清理缓存 */
    public void cleanup() {
        cacheFile.delete();
    }

    // ================= helpers =================

    private static File createTempCache(Context context) throws IOException {
        return File.createTempFile(
                "edit_", ".tmp", context.getCacheDir()
        );
    }

    private static void copyUriToFile(Context context, Uri uri, File target)
            throws IOException {

        ContentResolver resolver = context.getContentResolver();

        try (InputStream in = resolver.openInputStream(uri);
             OutputStream out = new FileOutputStream(target, false)) {

            if (in == null) {
                throw new FileNotFoundException("Cannot open InputStream for " + uri);
            }

            StreamUtils.copy(in, out);
        }
    }

    private static void copyUriToUri(Context context, Uri from, Uri to)
            throws IOException {

        ContentResolver resolver = context.getContentResolver();

        try (InputStream in = resolver.openInputStream(from);
             OutputStream out = resolver.openOutputStream(to, "wt")) {

            if (in == null || out == null) {
                throw new FileNotFoundException("Cannot open stream");
            }

            StreamUtils.copy(in, out);
        }
    }

    private static void copyFileToUri(Context context, File source, Uri uri)
            throws IOException {

        ContentResolver resolver = context.getContentResolver();

        try (InputStream in = new FileInputStream(source);
             OutputStream out = resolver.openOutputStream(uri, "wt")) {

            if (out == null) {
                throw new FileNotFoundException("Cannot open OutputStream for " + uri);
            }

            StreamUtils.copy(in, out);
        }
    }

}

