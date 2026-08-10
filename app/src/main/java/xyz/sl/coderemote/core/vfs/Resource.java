package xyz.sl.coderemote.core.vfs;

import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Resource {
    Uri getUri();
    String getName();
    String getPath();
    ResourceType getType();
    long getSize();
    long getLastModified();
    boolean exists();
    boolean delete();
    Resource rename(String newName) throws IOException;
    InputStream openInputStream() throws IOException;
    OutputStream openOutputStream() throws IOException;
    boolean isDirectory();
}

