package xyz.sl.coderemote;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import xyz.sl.coderemote.utils.SshClient;

public class SSHConnectTest {
    String hostLinux = BuildConfig.SSH_LINUX_HOST;
    int portLinux = Integer.parseInt(BuildConfig.SSH_LINUX_PORT);
    String nameLinux = BuildConfig.SSH_LINUX_USER;
    String passwordLinux = BuildConfig.SSH_LINUX_PASSWORD;

    String hostWin = BuildConfig.SSH_WIN_HOST;
    int portWin = Integer.parseInt(BuildConfig.SSH_WIN_PORT);
    String nameWin = BuildConfig.SSH_WIN_USER;
    String passwordWin = BuildConfig.SSH_WIN_PASSWORD;

    SshClient sshClientLinux;
    SshClient sshClientWin;

    @Before
    public void connect() throws Exception {
        sshClientLinux = new SshClient();
        sshClientLinux.connect(hostLinux, portLinux, nameLinux, passwordLinux);
        assertTrue(sshClientLinux.isConnected());

        sshClientWin = new SshClient();
        sshClientWin.connect(hostWin, portWin, nameWin, passwordWin);
        assertTrue(sshClientWin.isConnected());
    }

    @After
    public void disconnect(){
        sshClientLinux.disconnect();
        assertFalse(sshClientLinux.isConnected());

        sshClientWin.disconnect();
        assertFalse(sshClientWin.isConnected());
    }

    @Test
    public void testRemoteFile(){
        System.out.println("======== Test Remote File =========");
        String files1 = String.valueOf(sshClientWin.listFiles(sshClientWin.userPath()));
        System.out.println(files1);

        System.out.println("======== Test Remote File =========");
        String files2 = String.valueOf(sshClientLinux.listFiles(sshClientLinux.userPath()));
        System.out.println(files2);
    }

    @Test
    public void testLinuxIOStream(){
        System.out.println("======== Test I/O =========");
        try {
            InputStream inputStream = sshClientLinux.getChannelShellInputStream();
            OutputStream outputStream = sshClientLinux.getChannelShellOutputStream();
            readOutput(inputStream);
            // Send commands
            sendCommand(outputStream, "ls -lh");

            readOutput(inputStream);
            sendCommand(outputStream, "bash");
            // Read the output
            readOutput(inputStream);
            sendCommand(outputStream, "whoami");

            // Read the output
            readOutput(inputStream);
            sendCommand(outputStream, "pwd");

            // Read the output
            readOutput(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testWinIOStream(){
        System.out.println("======== Test I/O =========");
        try {
            InputStream inputStream = sshClientWin.getChannelShellInputStream();
            OutputStream outputStream = sshClientWin.getChannelShellOutputStream();
            readOutput(inputStream);
            // Send commands
            sendCommand(outputStream, "dir");

            readOutput(inputStream);
            sendCommand(outputStream, "powershell");
            // Read the output
            readOutput(inputStream);
            sendCommand(outputStream, "whoami");

            // Read the output
            readOutput(inputStream);
            sendCommand(outputStream, "ls");

            readOutput(inputStream);
            sendCommand(outputStream, "pwd");

            // Read the output
            readOutput(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final CountDownLatch waiter = new CountDownLatch(1);
    private static void sendCommand(OutputStream outputStream, String command) throws Exception {
//        outputStream.write((command + "\n").getBytes());
        outputStream.write((command + "\r\n").getBytes());  // CRLF for win | LF for Unix
        outputStream.flush();
        waiter.await(2, TimeUnit.SECONDS);
    }

    private static void readOutput(InputStream inputStream) throws Exception {
        byte[] buffer = new byte[1024];
        int bytesRead;

        while (inputStream.available() > 0 && (bytesRead = inputStream.read(buffer)) > 0) {
            System.out.print(new String(buffer, 0, bytesRead));
        }
    }

}
