package xyz.sl.coderemote.core.vfs;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import java.io.IOException;

public class LocalResourceProvider implements ResourceProvider {

    private final Context context;

    public LocalResourceProvider(Context context) {
        this.context = context;
    }

    @Override
    public boolean supports(Uri uri) {
        return "file".equals(uri.getScheme()) || "content".equals(uri.getScheme());
    }

    @Override
    public Resource resolve(Uri uri) throws IOException {
        DocumentFile file = DocumentFile.fromTreeUri(context, uri);
        if (file == null) {
            throw new IOException("Invalid URI: " + uri);
        }
        return new LocalResource(context, file);
    }
}
