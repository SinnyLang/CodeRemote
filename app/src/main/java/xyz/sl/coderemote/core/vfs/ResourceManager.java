package xyz.sl.coderemote.core.vfs;

import android.net.Uri;

import java.io.IOException;
import java.util.List;

public class ResourceManager {

    private final List<ResourceProvider> providers;

    public ResourceManager(List<ResourceProvider> providers) {
        this.providers = providers;
    }

    public Resource resolve(Uri uri) throws IOException {
        for (ResourceProvider provider : providers) {
            if (provider.supports(uri)) {
                return provider.resolve(uri);
            }
        }
        throw new IOException("Unsupported URI scheme: " + uri.getScheme());
    }
}
