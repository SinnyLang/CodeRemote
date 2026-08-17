package xyz.sl.coderemote.core.vfs.io;

import java.io.IOException;

import xyz.sl.coderemote.core.vfs.Resource;

public interface ResourceReader<T> {
    T read(Resource resource) throws IOException;
}
