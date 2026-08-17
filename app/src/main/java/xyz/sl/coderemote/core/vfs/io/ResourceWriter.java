package xyz.sl.coderemote.core.vfs.io;

import java.io.IOException;

import xyz.sl.coderemote.core.vfs.Resource;

public interface ResourceWriter<T> {
    void write(Resource resource, T data) throws IOException;
}
