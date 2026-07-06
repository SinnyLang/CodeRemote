package xyz.sl.coderemote.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class EditFileManger {
    // EditFileManger 的生命周期与 ApplicationContext 相同
    private Context context;

    public EditFileManger(Context context) {
        // TODO : 需要判断 context 是否能转换为 ApplicationContext 类型，
        //  如果不能则可能出现内存泄漏
        this.context = context;
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
            throw new NullPointerException("uri is null");

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

    /**
     * 逐行读取文本，通过回调处理每一行（适合大文件）
     */
    public static void readLinesFromUri(Context context, Uri uri, OnLineReadListener listener) {
        if (context == null || uri == null || listener == null) {
            return;
        }
        ContentResolver resolver = context.getContentResolver();
        try (InputStream inputStream = resolver.openInputStream(uri)) {
            if (inputStream == null) return;
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                listener.onLineRead(line);
            }
        } catch (IOException | SecurityException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public interface OnLineReadListener {
        void onLineRead(String line);
    }
}
