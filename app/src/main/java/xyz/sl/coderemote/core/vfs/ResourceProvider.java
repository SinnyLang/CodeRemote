package xyz.sl.coderemote.core.vfs;

import android.net.Uri;

import java.io.IOException;

public interface ResourceProvider {
    boolean supports(Uri uri);
    Resource resolve(Uri uri) throws IOException;
}
