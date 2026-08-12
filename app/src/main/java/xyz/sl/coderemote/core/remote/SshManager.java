package xyz.sl.coderemote.core.remote;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SshManager {

    private final SshConnectionStore store;
    private final Map<String, SshClient> connections = new ConcurrentHashMap<>();

    public SshManager(SshConnectionStore store) {
        this.store = store;
    }

    public SshClient getConnection(String connectionId) throws IOException {
        SshClient connection = connections.get(connectionId);

        if (connection != null && connection.isConnected()) {
            return connection;
        }
        return connect(connectionId);
    }

    public synchronized SshClient connect(String connectionId) throws IOException {
        SshClient existing = connections.get(connectionId);
        if (existing != null && existing.isConnected()) {
            return existing;
        }

        SshConnectionConfig config = store.get(connectionId);

        if (config == null) {
            throw new IOException("Unknown SSH connection: " + connectionId);
        }

        SshClient connection = new SshClient(config);
        connection.connect();

        connections.put(connectionId, connection);
        return connection;
    }

    public void disconnect(String connectionId) {
        SshClient connection = connections.remove(connectionId);

        if (connection != null) {
            connection.disconnect();
        }
    }

    public void disconnectAll() {
        for (SshClient connection : connections.values()) {
            connection.disconnect();
        }

        connections.clear();
    }

    public void addConnection(SshClient client){
        SshClient client1 = connections.put(client.getSshConnectionConfig().getId(), client);
    }
}
