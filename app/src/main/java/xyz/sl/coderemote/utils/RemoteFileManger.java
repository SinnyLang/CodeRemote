package xyz.sl.coderemote.utils;

import android.content.Context;
import android.net.Uri;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import xyz.sl.coderemote.ui.FileNode;

public class RemoteFileManger extends BaseFileManager{
    private SshManager sshManager;
    private String workdir;

    public RemoteFileManger(Context context, SshManager sshManager) {
        super(context);
        this.sshManager = sshManager;
        this.workdir = sshManager.userPath();
    }

    public RemoteFileManger(Context context, SshManager sshManager, String workdir){
        super(context);
        this.sshManager = sshManager;
        this.workdir = workdir;
    }

    @Override
    public @NotNull List<? extends @NotNull FileNode> scanDirectory(@NotNull Uri rootUri) {
//        sshManager.listFiles()
        return null;
    }
}
