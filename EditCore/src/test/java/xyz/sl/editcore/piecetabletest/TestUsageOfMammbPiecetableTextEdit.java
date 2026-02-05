package xyz.sl.editcore.piecetabletest;


import static org.junit.Assert.*;

import com.mammb.code.piecetable.TextEdit;

import org.junit.Test;

public class UsageOfMammbPiecetableTextEdit {


    @Test
    public void testBasicInsertDeleteUndoRedo(){
        TextEdit textEdit = TextEdit.of();
        textEdit.insert(0, 0, "A 这是");
        textEdit.insert(0, 4, " 123");
        textEdit.delete(0, 5, 2);
        assertEquals("A 这是 3", textEdit.getText(0));

        textEdit.undo();
        assertEquals("A 这是 123", textEdit.getText(0));

        textEdit.redo();
        assertEquals("A 这是 3", textEdit.getText(0));
    }
}
