package xyz.sl.coderemote.core.vfs;

import android.content.Context;
import android.net.Uri;

// todo: network action should be moved into Resource
import com.jcraft.jsch.SftpException;

import java.io.IOException;

import xyz.sl.coderemote.core.remote.SshClient;
import xyz.sl.coderemote.core.remote.SshManager;
import xyz.sl.coderemote.core.vfs.cache.SftpCacheTranslate;

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
    private final SftpCacheTranslate sftpCacheTranslate;

    public SftpResourceProvider(
            Context context,
            SshManager sshManager,
            LocalResourceProvider localResourceProvider
    ) {
        this.sshManager = sshManager;
        this.localResourceProvider = localResourceProvider;
        this.sftpCacheTranslate = new SftpCacheTranslate(context);
    }

    @Override
    public boolean supports(Uri uri) {
        return "sftp".equals(uri.getScheme());
    }

    @Override
    public Resource resolve(Uri uri) throws IOException {
        String connectionId = uri.getAuthority();
        String remotePath = uri.getPath();

        if (connectionId == null) {
            throw new IOException("Missing SFTP connection ID");
        }

        SshClient connection = sshManager.getConnection(connectionId);

        // parameter uri:
        // such as baseDir = sftp://[auth]/C:/Users/jocker/Desktop
        // such as file    = sftp://[auth]/C:/Users/jocker/Desktop/a.txt

        // 映射到本地文件
        // local cache uri:
        // such as baseDir = file:///[cacheDir]/ssh/[connectId]/Desktop
        // such as file    = file:///[cacheDir]/ssh/[connectId]/Desktop/a.txt
        Uri cacheResourceUri = sftpCacheTranslate.getCacheResourceUri(
                connectionId,
                connection.getBaseDir(),
                remotePath
        );

        try {
            return new SftpResource(
                    (LocalResource) localResourceProvider.resolve(cacheResourceUri),
                    connection,
                    uri,
                    connection.getSftpChannel().stat(remotePath)  // /C:/Users/jocker/Desktop
            );
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }
}
