package xyz.sl.coderemote.core.vfs;

import android.net.Uri;

// todo: network action should be moved into Resource
import com.jcraft.jsch.SftpException;

import java.io.IOException;

import xyz.sl.coderemote.core.remote.SshClient;
import xyz.sl.coderemote.core.remote.SshManager;

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
    public SftpResourceProvider(SshManager sshManager) {
        this.sshManager = sshManager;
    }

    @Override
    public boolean supports(Uri uri) {
        return "sftp".equals(uri.getScheme());
    }

    @Override
    public Resource resolve(Uri uri) throws IOException {
        String connectionId = uri.getHost();

        if (connectionId == null) {
            throw new IOException("Missing SFTP connection ID");
        }

        SshClient connection = sshManager.getConnection(connectionId);
        String path = uri.getPath();

        try {
            return new SftpResource(connection, path, connection.getSftpChannel().stat(path));
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }
}
