package xyz.sl.editcore;

import org.junit.Test;

public class SimpleEditorTest {
    @Test
    public void testEditor(){
        Editor editor = new SimpleEditor("Hello world", new InMemoryClipboard());
        editor.insert(5, ", dear");     // "Hello, dear world"
        editor.delete(0, 6);          // remove "Hello,"
        editor.cut(0, 4);                         // cut " dear"
        editor.paste(0);                     // paste to beginning
        System.out.println(editor.getText());
        if (editor.canUndo()) editor.undo();
        if (editor.canRedo()) editor.redo();
    }
}