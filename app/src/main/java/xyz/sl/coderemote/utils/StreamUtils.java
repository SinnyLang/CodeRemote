package xyz.sl.coderemote.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class StreamUtils {

    private static final int BUFFER_SIZE = 8 * 1024; // 8KB，Android 实践值

    private StreamUtils() {}

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] bytes = new byte[BUFFER_SIZE];

        int len = 0;
        while ((len = in.read(bytes))!=-1){
            out.write(bytes, 0, len);
        }
        out.flush();
    }

    public static void writeStringToFile(String text, File file) throws IOException {
        try (OutputStream out = new FileOutputStream(file, false)){
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static String readStringFromFile(File file) throws IOException {
        try (
                InputStream in = new FileInputStream(file);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
        ){
            byte[] buffer = new byte[8192];
            int len = 0;
            while ((len = in.read(buffer))!=-1){
                out.write(buffer, 0, len);
            }

            return out.toString("UTF-8");
        }
    }
}

