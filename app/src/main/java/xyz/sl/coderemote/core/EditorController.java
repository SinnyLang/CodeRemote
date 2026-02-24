package xyz.sl.coderemote.core;

public interface EditorController {

    void insertText(String text);
    void backspace(int len);
    void delete(int len);
    void moveCursor(Direction direction);
    void jumpTo(int row, int col);

    void redo();
    void undo();
    void flush();

    StringBuilder getText();
    Cursor getCursor();
    PositionMapper getCursorPositionMapper();
    int getCursorOffset();
    void setCursorOffset(int offset);
    void dispatch(EditorAction action);

    int getTextRows();
}
