package xyz.sl.coderemote.core.remote;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SshConnectionStore {

    private final Map<String, SshConnectionConfig> connections
            = new ConcurrentHashMap<>();

    public void add(SshConnectionConfig config) {
        connections.put(config.getId(), config);
    }

    public SshConnectionConfig get(String id) {
        return connections.get(id);
    }

    public void remove(String id) {
        connections.remove(id);
    }

    public List<SshConnectionConfig> getAll() {
        return new ArrayList<>(
                connections.values()
        );
    }
}