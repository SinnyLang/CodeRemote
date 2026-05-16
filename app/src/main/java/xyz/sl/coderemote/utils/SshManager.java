package xyz.sl.coderemote.utils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import com.jcraft.jsch.UserInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Properties;
import java.util.stream.Stream;

public class SshManager {
    private Session session;
    private ChannelSftp channelSftp;
    private ChannelShell channelShell;

    /**
     * 连接 SSH（密码认证）
     */
    public boolean connect(String host, int port, String username, String password) {
        try {
            JSch jsch = new JSch();

            // 创建会话
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            // 配置连接属性
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no"); // 仅测试环境使用！
            session.setConfig(config);

            // 设置超时（毫秒）
            session.connect(30_000);

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(30_000);

            channelShell = (ChannelShell) session.openChannel("shell");
            channelShell.setPty(true);
            channelShell.connect(30_000);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            session = null;
            channelSftp = null;
            channelShell = null;
            return false;
        }
    }

    /**
     * 连接 SSH（密码认证 - 推荐方式）
     */
    public boolean connectWithUserInfo(String host, int port, String username, String password) {
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);

            // 设置用户信息交互
            session.setUserInfo(new UserInfo() {
                @Override
                public String getPassphrase() {
                    return null;
                }

                @Override
                public String getPassword() {
                    return password;
                }

                @Override
                public boolean promptPassword(String message) {
                    return true;
                }

                @Override
                public boolean promptPassphrase(String message) {
                    return false;
                }

                @Override
                public boolean promptYesNo(String message) {
                    return true;
                }

                @Override
                public void showMessage(String message) {
                    System.out.println("SSH Message: " + message);
                }
            });

            session.connect(30_000);

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(30_000);

            channelShell = (ChannelShell) session.openChannel("shell");
            channelShell.setPty(true);
            channelShell.connect(30_000);


            return true;
        } catch (Exception e) {
            e.printStackTrace();
            session = null;
            channelSftp = null;
            channelShell = null;
            return false;
        }
    }

    /**
     * 连接 SSH（私钥认证）
     */
    public boolean connectWithPrivateKey(String host, int port, String username, String privateKeyPath) {
        try {
            JSch jsch = new JSch();

            // 添加私钥
            jsch.addIdentity(privateKeyPath);

            session = jsch.getSession(username, host, port);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect(30000);

            Channel channel = session.openChannel("sftp");
            channel.connect(30000);
            channelSftp = (ChannelSftp) channel;

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 下载文件
     */
    public boolean downloadFile(String remotePath, String localPath) {
        try {
            channelSftp.get(remotePath, localPath);
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
            channelSftp.get(remotePath, localPath, monitor);
            return true;
        } catch (SftpException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 上传文件
     */
    public boolean uploadFile(String localPath, String remotePath) {
        try {
            channelSftp.put(localPath, remotePath);
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
            channelSftp.put(localPath, remotePath, monitor);
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
    public String listFiles(String remotePath) {
        try {
            java.util.Vector<ChannelSftp.LsEntry> files = channelSftp.ls(remotePath);
            StringBuilder result = new StringBuilder();
            for (ChannelSftp.LsEntry entry : files) {
                result.append(entry.getFilename()).append("\n");
            }
            return result.toString();
        } catch (SftpException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 用户目录
     */
    public String userPath() {
        try {
            return channelSftp.pwd();
        } catch (SftpException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 创建目录
     */
    public boolean mkdir(String remotePath) {
        try {
            channelSftp.mkdir(remotePath);
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
            channelSftp.rm(remotePath);
            return true;
        } catch (SftpException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 执行 SSH 命令
     */
    public String execCommand(String command) {
        ChannelExec channelExec = null;
        try {
            channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(command);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(channelExec.getInputStream())
            );

            channelExec.connect(30000);

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
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
     * 断开连接
     */
    public void disconnect() {
        if (channelSftp != null && channelSftp.isConnected()) {
            channelSftp.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return session != null && session.isConnected() &&
                channelSftp != null && channelSftp.isConnected();
    }

    public ChannelShell getChannelShell(){
        if (!isConnected()){
            throw new NullPointerException("Cannot get Channel Shell on null ssh session");
        }
        return channelShell;
    }

    public InputStream getChannelShellInputStream() throws IOException {
        return channelShell.getInputStream();
    }

    public OutputStream getChannelShellOutputStream() throws IOException {
        return channelShell.getOutputStream();
    }
}
