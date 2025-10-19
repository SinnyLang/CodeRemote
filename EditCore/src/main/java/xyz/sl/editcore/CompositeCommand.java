package xyz.sl.editcore;

// CompositeCommand 用于将多个操作打包（例如 cut = delete + clipboard set）
import java.util.*;
public class CompositeCommand implements EditCommand {
    private final List<EditCommand> parts = new ArrayList<>();
    public void add(EditCommand c) { parts.add(c); }
    @Override public void execute() { for (EditCommand c : parts) c.execute(); }
    @Override public void undo() {
        for (int i = parts.size() - 1; i >= 0; i--) parts.get(i).undo();
    }
}
