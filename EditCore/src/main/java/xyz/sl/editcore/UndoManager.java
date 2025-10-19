package xyz.sl.editcore;

import java.util.ArrayDeque;
import java.util.Deque;

// UndoManager
public class UndoManager {
    private final Deque<EditCommand> undoStack = new ArrayDeque<>();
    private final Deque<EditCommand> redoStack = new ArrayDeque<>();
    public void executeCommand(EditCommand c) {
        c.execute();
        undoStack.push(c);
        redoStack.clear();
    }
    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }
    public void undo() {
        if (!canUndo()) return;
        EditCommand c = undoStack.pop();
        c.undo();
        redoStack.push(c);
    }
    public void redo() {
        if (!canRedo()) return;
        EditCommand c = redoStack.pop();
        c.execute();
        undoStack.push(c);
    }
}
