package xyz.sl.coderemote.core;

public sealed interface EditorAction permits
        EditorAction.Insert,
        EditorAction.Backspace,
        EditorAction.Delete,
        EditorAction.MoveCursor,
        EditorAction.Jump,
        EditorAction.Undo,
        EditorAction.Redo,
        EditorAction.Flush
{
    record Insert(String text) implements EditorAction {}
    record Backspace(int len) implements EditorAction {}
    record Delete(int len) implements EditorAction {}
    record MoveCursor(Direction direction, boolean selecting) implements EditorAction {}
    record Jump(int row, int column) implements EditorAction {}
    record Undo() implements EditorAction {}
    record Redo() implements EditorAction {}
    record Flush() implements EditorAction{}
}

