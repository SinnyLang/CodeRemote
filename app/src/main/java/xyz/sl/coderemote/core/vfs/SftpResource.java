package xyz.sl.coderemote.core.vfs;

import android.net.Uri;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import xyz.sl.coderemote.core.remote.SshClient;

/**
 * 表示远程主机和本地存在的文件，一个 SftpResource 同时指向这两个文件资源
 * 当本地 Resource 做出更改时，将相应的操作同步至远程主机
 */
public class SftpResource implements DirectoryResource {
    private final LocalResource localMappedResource;
    private final SshClient connection;
    private final String path;
    private final SftpATTRS attrs;

    public SftpResource(LocalResource localMappedResource,SshClient connection,
                        String path, SftpATTRS attrs) {
        this.localMappedResource = localMappedResource;
        this.connection = connection;
        this.path = path;
        this.attrs = attrs;
    }

    @Override
    public Uri getUri() {
        return Uri.parse("sftp://" + path);
    }

    @Override
    public String getName() {
        int index = path.lastIndexOf('/');
        if (index < 0) {
            return path;
        }
        return path.substring(index + 1);
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public ResourceType getType() {
        return attrs.isDir() ? ResourceType.DIRECTORY : ResourceType.FILE;
    }

    @Override
    public long getSize() {
        return attrs.getSize();
    }

    @Override
    public long getLastModified() {
        return attrs.getMTime() * 1000L;
    }

    @Override
    public boolean exists() {
        try {
            connection.getSftpChannel().stat(path);
            return true;
        } catch (SftpException e) {
            return false;
        }
    }

    @Override
    public boolean delete() throws RuntimeException {
        try {
            if (attrs.isDir()) {
                connection.getSftpChannel().rmdir(path);
            } else {
                connection.getSftpChannel().rm(path);
            }
            return true;
        } catch (SftpException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Resource rename(String newName) throws IOException {
        String parent = path.substring(0, path.lastIndexOf('/'));
        String newPath = parent + "/" + newName;

        try {
            connection.getSftpChannel().rename(path, newPath);
            return resolve(newPath);
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public InputStream openInputStream() throws IOException {
        try {
            return connection.getSftpChannel().get(path);
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }
    @Override
    public OutputStream openOutputStream() throws IOException {
        try {
            return connection.getSftpChannel().put(path);
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean isDirectory() {
        return attrs.isDir();
    }

    @Override
    public List<Resource> list() throws IOException {
        List<Resource> result = new ArrayList<>();

        try {
            Vector<ChannelSftp.LsEntry> entries = connection.getSftpChannel().ls(path);

            for (ChannelSftp.LsEntry entry : entries) {
                String name = entry.getFilename();
                if (".".equals(name) || "..".equals(name)) continue;

                String childPath = path.endsWith("/") ? path + name : path + "/" + name;

                result.add(
                        new SftpResource(connection, childPath, entry.getAttrs())
                );
            }

            return result;
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    private SftpResource resolve(String path) throws IOException {
        try {
            SftpATTRS attrs = connection.getSftpChannel().stat(path);
            return new SftpResource(connection, path, attrs);
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Resource createFile(String name) throws IOException {
        String newPath = path + "/" + name;
        try {
            OutputStream output = connection.getSftpChannel().put(newPath);
            output.close();

            return resolve(newPath);
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Resource createDirectory(String name) throws IOException {
        String newPath = path + "/" + name;
        try {
            connection.getSftpChannel().mkdir(newPath);

            return resolve(newPath);
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }
}
