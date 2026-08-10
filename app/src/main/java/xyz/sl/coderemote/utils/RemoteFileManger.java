package xyz.sl.coderemote.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import xyz.sl.coderemote.core.FileNode;
import xyz.sl.coderemote.ui.FileTreeViewModelKt;

public class RemoteFileManger extends BaseFileManager{
    private SshClient sshClient;
    private String workdir;
    private File localMapDir;

    /**
     * 构造函数，默认工作目录是用户主目录
     */
    public RemoteFileManger(Context context, SshClient sshClient) {
        super(context);
        this.sshClient = sshClient;
        this.workdir = sshClient.userPath();
        createMapDirectoryInLocal();
    }

    public RemoteFileManger(Context context, SshClient sshClient, String workdir){
        super(context);
        this.sshClient = sshClient;
        this.workdir = workdir;
        createMapDirectoryInLocal();
    }

    /** 在本地创建一个文件夹，将远程项目根目录映射到本机 */
    private void createMapDirectoryInLocal(){
        // 1.创建远程映射根目录（使用远程路径的哈希值作为目录名，避免特殊字符）
        String remoteHash = Integer.toHexString(workdir.hashCode());
        String remoteName = workdir.replace("/", "_").replace(":", "_");
        if (remoteName.length() > 50) {
            remoteName = remoteName.substring(0, 50) + "_" + remoteHash;
        }

        File mapDir = new File(cacheDir, "remote_cache_" + remoteName);
        if (!mapDir.exists()) {
            boolean created = mapDir.mkdirs();
            if (!created) {
                throw new IllegalStateException("无法创建映射目录: " + mapDir.getAbsolutePath());
            }
        }

        // 2. 保存映射目录路径（用于后续操作）
        this.localMapDir = mapDir;

        // 3. 可选：创建元数据文件记录映射关系
        createMappingMetadata(mapDir);

        Log.d("RemoteFileManger", "远程目录已映射到本地: " + mapDir.getAbsolutePath());
    }

    /** 创建映射元数据文件（可选）
     */
    private void createMappingMetadata(File mapDir) {
        try {
            File metaFile = new File(mapDir, ".remotemeta");
            if (!metaFile.exists()) {
                try (FileWriter writer = new FileWriter(metaFile)) {
                    writer.write("remote_path=" + workdir + "\n");
                    writer.write("created_at=" + System.currentTimeMillis() + "\n");
                    writer.write("ssh_user=" + sshClient.getUser() + "\n");
                }
            }
        } catch (IOException e) {
            Log.w("RemoteFileManger", "无法创建元数据文件", e);
        }
    }

    @Override
    public @NotNull List<? extends @NotNull FileNode> scanDirectory(@NotNull Uri rootUri) {
//        List<String> filesName = sshClient.listFiles(rootUri.getPath());
        return List.of(FileTreeViewModelKt.sampleFiles());
    }
}
