package xyz.sl.coderemote.core.vfs;

import android.app.Application;
import android.content.Context;
import android.net.Uri;

// todo: network action should be moved into Resource
import com.jcraft.jsch.SftpException;

import java.io.IOException;

import xyz.sl.coderemote.core.remote.SshClient;
import xyz.sl.coderemote.core.remote.SshManager;
import xyz.sl.coderemote.core.vfs.cache.Cache;
import xyz.sl.coderemote.core.vfs.cache.SftpCache;

/** SFTP uri maybe define:
 *
 * <p>
 *  origin uri:
 *  [sftp]://[jocker]@[localhost]:[22][/home/abc/abc.txt]?{query}#{fragment}
 * </p>
 * <p>
 *  connection id uri:
 *  sftp://[connection-id][path]?{query}#{fragment}
 * </p>
 *
 */
public class SftpResourceProvider implements ResourceProvider {

    private final SshManager sshManager;
    private final LocalResourceProvider localResourceProvider;
    private final SftpCache sftpCache;
    public SftpResourceProvider(
            Context context,
            SshManager sshManager,
            LocalResourceProvider localResourceProvider
    ) {
        this.sshManager = sshManager;
        this.localResourceProvider = localResourceProvider;
        this.sftpCache = new SftpCache(context);
    }

    @Override
    public boolean supports(Uri uri) {
        return "sftp".equals(uri.getScheme());
    }

    @Override
    public Resource resolve(Uri uri) throws IOException {
        String connectionId = uri.getAuthority();

        if (connectionId == null) {
            throw new IOException("Missing SFTP connection ID");
        }

        SshClient connection = sshManager.getConnection(connectionId);
        String path = uri.getPath();

        // 映射到本地文件
        Uri cachedResource = sftpCache.getCachedResource(connectionId, uri.getPath());

        try {
            return new SftpResource(
                    (LocalResource) localResourceProvider.resolve(cachedResource),
                    connection,
                    path,
                    connection.getSftpChannel().stat(path)
            );
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }
}
