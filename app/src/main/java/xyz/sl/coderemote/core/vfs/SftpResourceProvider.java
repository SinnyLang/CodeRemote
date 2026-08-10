package xyz.sl.coderemote.core.vfs;

import android.net.Uri;

import java.io.IOException;

public class SftpResourceProvider implements ResourceProvider {

//    private final SftpConnectionManager connectionManager;
//    public SftpResourceProvider(SftpConnectionManager connectionManager) {
//        this.connectionManager = connectionManager;
//    }

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

//        SftpConnection connection = connectionManager.getConnection(connectionId);
        String path = uri.getPath();

//        return connection.resolve(path);
        return null;
    }
}
