package xyz.sl.coderemote.core.remote;

import com.jcraft.jsch.*;
import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * 统一的SSH客户端
 * 支持密码认证和私钥认证
 * 提供SFTP文件操作和Shell交互功能
 */
public class SshClient {

    private final SshConnectionConfig config;
    private Session session;
    private ChannelSftp sftpChannel;
    private ChannelShell shellChannel;

    // ==================== 构造函数 ====================

    public SshClient(SshConnectionConfig config) {
        this.config = config;
    }

    public SshClient(String id, String name, String host, int port, String username,
                     SshConnectionConfig.AuthType authType,
                     String password, String privateKeyPath, String passphrase) {

        this.config = new SshConnectionConfig(id, name, host, port, username,
                authType, password, privateKeyPath, passphrase);
    }

    /**
     * 便捷构造器 - 使用密码认证
     */
    public SshClient(String host, int port, String username, String password) {
        String id = UUID.randomUUID().toString();
        this.config = new SshConnectionConfig(id, id.substring(0, 5),
                host, port, username,
                SshConnectionConfig.AuthType.PASSWORD,
                password,
                null,
                null
        );
    }

    public SshConnectionConfig getSshConnectionConfig(){
        return config;
    }

    // ==================== 连接管理 ====================

    /**
     * 连接到SSH服务器
     */
    public synchronized void connect() throws IOException {
        if (isConnected()) return;

        try {
            JSch jsch = new JSch();

            // 加载私钥（如果使用私钥认证）
            if (config.getAuthType() == SshConnectionConfig.AuthType.PRIVATE_KEY) {
                if (config.getPrivateKeyPath() == null || config.getPrivateKeyPath().isEmpty()) {
                    throw new IOException("Private key path is required for private key authentication");
                }
                if (config.getPassphrase() != null && !config.getPassphrase().isEmpty()) {
                    jsch.addIdentity(config.getPrivateKeyPath(), config.getPassphrase());
                } else {
                    jsch.addIdentity(config.getPrivateKeyPath());
                }
            }

            // 创建会话
            session = jsch.getSession(
                    config.getUsername(),
                    config.getHost(),
                    config.getPort()
            );

            // 设置密码（如果使用密码认证）
            if (config.getAuthType() == SshConnectionConfig.AuthType.PASSWORD) {
                if (config.getPassword() == null) {
                    throw new IOException("Password is required for password authentication");
                }
                session.setPassword(config.getPassword());
            }

            // 配置会话属性
            Properties sessionConfig = new Properties();
            // 注意：生产环境应该实现HostKeyRepository，这里仅用于开发测试
            sessionConfig.put("StrictHostKeyChecking", "no");
            session.setConfig(sessionConfig);

            // 设置超时
            int timeout = 30000;
            session.connect(timeout);

            // 打开SFTP通道
            Channel channel = session.openChannel("sftp");
            channel.connect(timeout);
            sftpChannel = (ChannelSftp) channel;

            // 如果需要Shell功能，延迟初始化
            // 用户可以通过 getShellChannel() 方法按需创建

        } catch (JSchException e) {
            disconnect();
            throw new IOException("SSH connection failed: " + e.getMessage(), e);
        }
    }

    /**
     * 断开连接
     */
    public synchronized void disconnect() {
        if (sftpChannel != null) {
            if (sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            sftpChannel = null;
        }

        if (shellChannel != null) {
            if (shellChannel.isConnected()) {
                shellChannel.disconnect();
            }
            shellChannel = null;
        }

        if (session != null) {
            if (session.isConnected()) {
                session.disconnect();
            }
            session = null;
        }
    }

    /**
     * 检查是否已连接，session连接且打开sftp通道则为连接
     */
    public boolean isConnected() {
        return session != null
                && session.isConnected()
                && sftpChannel != null
                && sftpChannel.isConnected();
    }

    // ==================== SFTP 操作 ====================

    /**
     * 获取SFTP通道（内部使用）
     */
    public ChannelSftp getSftpChannel() {
        if (!isConnected()) {
            throw new IllegalStateException("SSH client is not connected");
        }
        return sftpChannel;
    }

    /**
     * 上传文件
     */
    public boolean uploadFile(String localPath, String remotePath) {
        try {
            getSftpChannel().put(localPath, remotePath);
            return true;
        } catch (SftpException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 上传文件（带进度监控）
     */
    public boolean uploadFileWithProgress(String localPath, String remotePath, SftpProgressMonitor monitor) {
        try {
            getSftpChannel().put(localPath, remotePath, monitor);
            return true;
        } catch (SftpException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 下载文件
     */
    public boolean downloadFile(String remotePath, String localPath) {
        try {
            getSftpChannel().get(remotePath, localPath);
            return true;
        } catch (SftpException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 下载文件（带进度监控）
     */
    public boolean downloadFileWithProgress(String remotePath, String localPath, SftpProgressMonitor monitor) {
        try {
            getSftpChannel().get(remotePath, localPath, monitor);
            return true;
        } catch (SftpException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 列出目录内容
     */
    @SuppressWarnings("unchecked")
    public List<String> listFiles(String remotePath) {
        try {
            java.util.Vector<ChannelSftp.LsEntry> files = getSftpChannel().ls(remotePath);
            List<String> fileList = new LinkedList<>();
            for (ChannelSftp.LsEntry entry : files) {
                String filename = entry.getFilename();
                // 过滤掉 . 和 ..
                if (!".".equals(filename) && !"..".equals(filename)) {
                    fileList.add(filename);
                }
            }
            return fileList;
        } catch (SftpException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * 列出目录中的文件夹
     */
    @SuppressWarnings("unchecked")
    public List<String> listDirectories(String remotePath) {
        try {
            java.util.Vector<ChannelSftp.LsEntry> files = getSftpChannel().ls(remotePath);
            List<String> dirList = new LinkedList<>();
            for (ChannelSftp.LsEntry entry : files) {
                String filename = entry.getFilename();
                if (!".".equals(filename) && !"..".equals(filename) && entry.getAttrs().isDir()) {
                    dirList.add(filename);
                }
            }
            return dirList;
        } catch (SftpException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * 获取当前工作目录
     */
    public String getCurrentDirectory() {
        try {
            return getSftpChannel().pwd();
        } catch (SftpException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 切换目录
     */
    public boolean changeDirectory(String remotePath) {
        try {
            getSftpChannel().cd(remotePath);
            return true;
        } catch (SftpException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 创建目录
     */
    public boolean mkdir(String remotePath) {
        try {
            getSftpChannel().mkdir(remotePath);
            return true;
        } catch (SftpException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除文件
     */
    public boolean deleteFile(String remotePath) {
        try {
            getSftpChannel().rm(remotePath);
            return true;
        } catch (SftpException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除目录（递归）
     */
    @SuppressWarnings("unchecked")
    public boolean deleteDirectory(String remotePath) {
        try {
            java.util.Vector<ChannelSftp.LsEntry> files = getSftpChannel().ls(remotePath);
            for (ChannelSftp.LsEntry entry : files) {
                String filename = entry.getFilename();
                if (".".equals(filename) || "..".equals(filename)) {
                    continue;
                }
                String fullPath = remotePath + "/" + filename;
                if (entry.getAttrs().isDir()) {
                    deleteDirectory(fullPath);
                } else {
                    getSftpChannel().rm(fullPath);
                }
            }
            getSftpChannel().rmdir(remotePath);
            return true;
        } catch (SftpException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 重命名/移动文件或目录
     */
    public boolean rename(String oldPath, String newPath) {
        try {
            getSftpChannel().rename(oldPath, newPath);
            return true;
        } catch (SftpException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==================== Shell 操作 ====================

    /**
     * 获取Shell通道（延迟初始化）
     */
    public synchronized ChannelShell getShellChannel() throws IOException {
        if (!isConnected()) {
            throw new IllegalStateException("SSH client is not connected");
        }

        if (shellChannel == null || !shellChannel.isConnected()) {
            try {
                shellChannel = (ChannelShell) session.openChannel("shell");
                shellChannel.setPty(true);
                int timeout = 30000;
                shellChannel.connect(timeout);
            } catch (JSchException e) {
                throw new IOException("Failed to open shell channel", e);
            }
        }
        return shellChannel;
    }

    /**
     * 获取Shell输入流
     */
    public InputStream getShellInputStream() throws IOException {
        return getShellChannel().getInputStream();
    }

    /**
     * 获取Shell输出流
     */
    public OutputStream getShellOutputStream() throws IOException {
        return getShellChannel().getOutputStream();
    }

    // ==================== 命令执行 ====================

    /**
     * 执行SSH命令（非交互式）
     */
    public String execCommand(String command) {
        ChannelExec channelExec = null;
        try {
            if (!isConnected()) {
                throw new IllegalStateException("SSH client is not connected");
            }

            channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(command);

            // 设置输入输出流
            InputStream inputStream = channelExec.getInputStream();
            InputStream errorStream = channelExec.getErrStream();

            channelExec.connect(30000);

            // 读取输出
            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();

            // 使用线程读取错误流
            Thread errorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line).append("\n");
                    }
                } catch (IOException e) {
                    // 忽略
                }
            });
            errorThread.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            errorThread.join(1000);

            // 如果有错误输出，抛异常或合并
            if (error.length() > 0) {
                // 可以选择合并错误输出或抛出异常
                output.append("[ERROR]\n").append(error);
            }

            return output.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (channelExec != null && channelExec.isConnected()) {
                channelExec.disconnect();
            }
        }
    }

    /**
     * 执行命令并返回退出码
     */
    public CommandResult execCommandWithResult(String command) {
        ChannelExec channelExec = null;
        try {
            if (!isConnected()) {
                throw new IllegalStateException("SSH client is not connected");
            }

            channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(command);

            InputStream inputStream = channelExec.getInputStream();
            InputStream errorStream = channelExec.getErrStream();

            channelExec.connect(30000);

            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();

            Thread errorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line).append("\n");
                    }
                } catch (IOException e) {
                    // 忽略
                }
            });
            errorThread.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            errorThread.join(1000);

            // 等待通道关闭以获取退出码
            while (!channelExec.isClosed()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            int exitCode = channelExec.getExitStatus();
            return new CommandResult(output.toString(), error.toString(), exitCode);

        } catch (Exception e) {
            e.printStackTrace();
            return new CommandResult(null, e.getMessage(), -1);
        } finally {
            if (channelExec != null && channelExec.isConnected()) {
                channelExec.disconnect();
            }
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 获取用户名
     */
    public String getUsername() {
        return session != null ? session.getUserName() : null;
    }

    /**
     * 获取主机地址
     */
    public String getHost() {
        return config.getHost();
    }

    /**
     * 获取端口
     */
    public int getPort() {
        return config.getPort();
    }

    /**
     * 获取配置
     */
    public SshConnectionConfig getConfig() {
        return config;
    }

    public void setBaseDir(String baseDir){
        config.setBaseDir(baseDir);
    }

    public String getBaseDir() {
        return config.getBaseDir();
    }

    // ==================== 内部类 ====================

    /**
     * 命令执行结果
     */
    public static class CommandResult {
        private final String output;
        private final String error;
        private final int exitCode;

        public CommandResult(String output, String error, int exitCode) {
            this.output = output;
            this.error = error;
            this.exitCode = exitCode;
        }

        public String getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }

        public int getExitCode() {
            return exitCode;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }

        @Override
        public String toString() {
            return "CommandResult{" +
                    "exitCode=" + exitCode +
                    ", output='" + output + '\'' +
                    ", error='" + error + '\'' +
                    '}';
        }
    }
}
