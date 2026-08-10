package xyz.sl.coderemote.core.vfs;

import java.io.IOException;
import java.util.List;

public interface DirectoryResource extends Resource {
    List<Resource> list() throws IOException;
    Resource createFile(String name) throws IOException;
    Resource createDirectory(String name) throws IOException;
}
