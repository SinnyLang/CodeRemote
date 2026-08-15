package xyz.sl.coderemote.core.vfs;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import xyz.sl.coderemote.utils.DocumentFileUtils;

public class LocalResource implements DirectoryResource {
    private final Context context;
    private final DocumentFile documentFile;

    public LocalResource(Context context, DocumentFile documentFile) {
        this.context = context;
        this.documentFile = documentFile;
    }

    public static LocalResource emptyLocalResource(){
        return new LocalResource(new Application(), DocumentFile.fromFile(new File("")));
    }

    @Override
    public Uri getUri() {
        return documentFile.getUri();
    }

    @Override
    public String getName() {
        String name = documentFile.getName();
        return name != null ? name : "";
    }

    @Override
    public String getPath() {
        return documentFile.getUri().toString();
    }

    @Override
    public ResourceType getType() {
        return documentFile.isDirectory() ? ResourceType.DIRECTORY : ResourceType.FILE;
    }

    @Override
    public long getSize() {
        return documentFile.isFile() ? documentFile.length() : 0;
    }

    @Override
    public long getLastModified() {
        return documentFile.lastModified();
    }

    @Override
    public boolean exists() {
        return documentFile.exists();
    }

    @Override
    public boolean delete() {
        return documentFile.delete();
    }

    @Override
    public Resource rename(String newName) {
        if (!documentFile.renameTo(newName))
            throw new RuntimeException("Failed to rename: " + getName());

        return new LocalResource(context, documentFile);
    }

    @Override
    public InputStream openInputStream() throws IOException {
        InputStream inputStream =
                context.getContentResolver().openInputStream(getUri());
        if (inputStream == null) {
            throw new IOException("Cannot open input stream: " + getUri());
        }
        return inputStream;
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        OutputStream outputStream =
                context.getContentResolver().openOutputStream(getUri());
        if (outputStream == null) {
            throw new IOException("Cannot open output stream: " + getUri());
        }
        return outputStream;
    }

    @Override
    public boolean isDirectory() {
        return documentFile.isDirectory();
    }

    @Override
    public List<Resource> list() {
        DocumentFile[] files = documentFile.listFiles();
        List<Resource> result = new ArrayList<>();

        for (DocumentFile file : files) {
            result.add(new LocalResource(context, file));
        }
        return result;
    }

    @Override
    public Resource createFile(String name) {
        DocumentFile file =
                documentFile.createFile("application/octet-stream", name);
        if (file == null) {
            throw new RuntimeException("Failed to create file: " + name);
        }
        return new LocalResource(context, file);
    }

    @Override
    public Resource createDirectory(String name) {
        DocumentFile directory =
                documentFile.createDirectory(name);
        if (directory == null) {
            throw new RuntimeException("Failed to create directory: " + name);
        }
        return new LocalResource(context, directory);
    }

    @Override
    public Resource ensureParentDirectory(){
        DocumentFile parentDirectory = DocumentFileUtils.ensureParentDirectory(context, documentFile);
        if (parentDirectory == null)
            throw new RuntimeException("can not ensure parent directory: "+documentFile.getUri());

        return new LocalResource(context, parentDirectory);
    }


    protected LocalResource resolve(String name){
        Uri child = getUri().buildUpon().appendPath(name).build();

        DocumentFile file = null;
        if ("file".equals(child.getScheme())) {
            String path = child.getPath();
            if (path != null){
                file = DocumentFile.fromFile(new File(path));
            }
        }

        if ("content".equals(child.getScheme()))
            file = DocumentFile.fromTreeUri(context, child);


        if (file == null) {
            throw new RuntimeException("Invalid URI: " + child);
        }

        LocalResource localResource = new LocalResource(context, file);
        return localResource;
    }
}
