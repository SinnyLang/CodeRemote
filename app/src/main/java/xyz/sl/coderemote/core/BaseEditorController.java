package xyz.sl.coderemote.core;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.mammb.code.piecetable.Pos;
import com.mammb.code.piecetable.TextEdit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import xyz.sl.coderemote.utils.CacheEditFile;

public class BaseEditorController implements EditorController{
    private final Cursor cursor;
    private Selection selection;
    private final TextEdit textEdit;
    private final Path textPath;
    private final CacheEditFile cache;

    /**
     * Mapper cursor position between UI Offset Model and Cursor Row Column Model.
     */
    private final PositionMapper cursorPositionMapper;


    // ------------- BaseEditorController -----------------
    public BaseEditorController(Context context, Uri uri) throws IOException {
        cache = CacheEditFile.fromUri(context, uri);
        textPath = cache.getCacheFile().toPath();

        cursor = new Cursor(0, 0);
        textEdit = TextEdit.of(textPath);

        cursorPositionMapper = setPositionMapper();
    }

    public BaseEditorController(Context context, String text) throws IOException {
        cache = CacheEditFile.empty(context);
        textPath = cache.getCacheFile().toPath();

        cursor = new Cursor(0, 0);
        textEdit = TextEdit.of(textPath);
        cursorPositionMapper = setPositionMapper();

        insertText(text);
    }


    // ------------- PositionMapper -----------------

    @NonNull
    private PositionMapper setPositionMapper() {
        final PositionMapper cursorPositionMapper;
        List<Integer> lineStartOffsets = new LinkedList<>();
        int textLength = 0;
        for (int i = 0; i < textEdit.rows(); i++) {
            lineStartOffsets.add(textLength);
            textLength += textEdit.getText(i).length();
        }
        cursorPositionMapper = new PositionMapper(lineStartOffsets);
        return cursorPositionMapper;
    }
    public PositionMapper getCursorPositionMapper() {
        return cursorPositionMapper;
    }

    public void dispatch(EditorAction action) {
        if (action instanceof EditorAction.Insert act)
            insertText(act.text());
        if (action instanceof EditorAction.Backspace act)
            backspace(act.len());
        if (action instanceof EditorAction.Delete act)
            delete(act.len());
        if (action instanceof EditorAction.MoveCursor act)
            moveCursor(act.direction());
        if (action instanceof EditorAction.Jump act)
            jumpTo(act.row(), act.column());
        if (action instanceof EditorAction.Undo)
            undo();
        if (action instanceof EditorAction.Redo)
            redo();
        if (action instanceof EditorAction.Flush)
            flush();
    }

    @Override
    public void insertText(String text) {
        Pos pos = textEdit.insert(cursor.row, cursor.col, text);
        updateCursorPositionMapper();
        setCursor(pos.row(), pos.col());
        Log.d("Controller-InsertText", getText().toString());
    }

    @Override
    public void delete(int len) {
        textEdit.delete(cursor.row, cursor.col, len);
        updateCursorPositionMapper();
        Log.d("Controller-DeleteText", getText().toString());
    }

    /**
     * After update text, the position of every line start will change.
     * To get last position, call this method after update text.
     */
    private void updateCursorPositionMapper(){
        List<Integer> lineStartOffsets = new LinkedList<>();
        int textLength = 0;
        for (int i = 0; i < textEdit.rows(); i++) {
            lineStartOffsets.add(textLength);
            textLength += textEdit.getText(i).length();
        }
        cursorPositionMapper.setLineStartOffsets(lineStartOffsets);
    }

    @Override
    public void backspace(int len) {
        Pos pos = textEdit.backspace(cursor.row, cursor.col, len);
        updateCursorPositionMapper();
        setCursor(pos.row(), pos.col());
    }

    @Override
    public void moveCursor(Direction direction) {
        setCursor(direction.row(), direction.col());
    }

    @Override
    public void jumpTo(int row, int col) {
        setCursor(row, col);
    }

    @Override
    public void redo() {
        List<Pos> posList = textEdit.redo();
        try {
            Pos pos = posList.get(0);
            setCursor(pos.row(), pos.col());
        } catch (IndexOutOfBoundsException e) {
            Log.d("BaseEditorController", "没有redo记录");
        }
    }

    @Override
    public void undo() {
        List<Pos> posList = textEdit.undo();
        try {
            Pos pos = posList.get(0);
            setCursor(pos.row(), pos.col());
        } catch (IndexOutOfBoundsException e) {
            Log.d("BaseEditorController", "没有undo记录");
        }
    }

    @Override
    public void flush() {
        textEdit.flush();
    }



    public Cursor getCursor() {
        return cursor;
    }

    public void setCursor(int row, int col) {
        this.cursor.col = col;
        this.cursor.row = row;
    }

    @Override
    public int getCursorOffset() {
        return cursorPositionMapper.toOffset(cursor);
    }

    @Override
    public void setCursorOffset(int offset) {
        Cursor cursor = cursorPositionMapper.fromOffset(offset);
        this.cursor.col = cursor.col;
        this.cursor.row = cursor.row;
    }


    public StringBuilder getText(){
        StringBuilder s = new StringBuilder();
        for (int row = 0; row < textEdit.rows(); row++) {
            s.append(textEdit.getText(row));
        }
        return s;
    }

    public TextEdit getTextEdit() {
        return textEdit;
    }
}
