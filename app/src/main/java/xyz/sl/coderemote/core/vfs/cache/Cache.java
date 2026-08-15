package xyz.sl.coderemote.core.vfs.cache;

import android.net.Uri;

import java.io.IOException;

public interface Cache {
    boolean isCached();
    Uri cache() throws IOException;
    boolean isCacheValid();
}