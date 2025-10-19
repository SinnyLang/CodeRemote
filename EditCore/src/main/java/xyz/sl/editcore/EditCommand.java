package xyz.sl.editcore;

// Command 模式 + UndoManager（简化）
public interface EditCommand {
    void execute();
    void undo();
}
