package xyz.sl.coderemote.core.vfs.io;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import xyz.sl.coderemote.core.vfs.Resource;

public class TextWriter implements ResourceWriter<String>{
    @Override
    public void write(Resource resource, String data) throws IOException {
        write(resource, data, StandardCharsets.UTF_8);
    }

    public void write(Resource resource, String data, Charset charset) throws IOException{
        if (resource == null) {
            throw new IllegalArgumentException("resource is null");
        }
        if (data == null) {
            throw new IllegalArgumentException("output data is null");
        }
        if (charset == null) {
            throw new IllegalArgumentException("charset is null");
        }
        byte[] dataBytes = data.getBytes(charset);

        new ByteArrayWriter().write(resource, dataBytes); // 复用静态方法
    }
}
