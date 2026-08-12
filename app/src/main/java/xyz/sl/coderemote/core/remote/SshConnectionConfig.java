package xyz.sl.coderemote.core.remote;

public class SshConnectionConfig {

    private final String id;
    private final String name;

    private final String host;
    private final int port;

    private final String username;

    private final AuthType authType;

    private final String password;
    private final String privateKeyPath;
    private final String passphrase;

    public SshConnectionConfig(
            String id,
            String name,
            String host,
            int port,
            String username,
            AuthType authType,
            String password,
            String privateKeyPath,
            String passphrase
    ) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.port = port;
        this.username = username;
        this.authType = authType;
        this.password = password;
        this.privateKeyPath = privateKeyPath;
        this.passphrase = passphrase;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public String getPassword() {
        return password;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public enum AuthType {
        PASSWORD,
        PRIVATE_KEY
    }
}
