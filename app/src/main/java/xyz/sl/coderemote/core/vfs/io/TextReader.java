package xyz.sl.coderemote.core.vfs.io;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import xyz.sl.coderemote.core.vfs.Resource;

public class TextReader implements ResourceReader<String>{
    private static final String tag = "TextReader";

    @Override
    public String read(Resource resource) throws IOException {
        return read(resource, StandardCharsets.UTF_8);
    }

    public String read(Resource resource, Charset charset) throws IOException{
        byte[] data = new ByteArrayReader().read(resource);
        if (data == null)
            throw new NullPointerException("result of \"ByteArrayReader.read(Resource)\" is null");

        return new String(data, charset);
    }

}
