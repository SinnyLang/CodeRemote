package xyz.sl.coderemote.core.vfs.cache;

import java.io.IOException;

import xyz.sl.coderemote.core.vfs.Resource;

public interface Cache {
    Resource getCachedResource(Resource remoteResource);
    boolean isCached(Resource remoteResource);
    Resource cache(Resource remoteResource) throws IOException;
}