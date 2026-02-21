package xyz.sl.coderemote.core;

import static org.junit.Assert.*;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.mammb.code.piecetable.TextEdit;
import com.mammb.code.piecetable.log.Loggers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import xyz.sl.coderemote.log.TextEditLogger;
import xyz.sl.coderemote.utils.StreamUtils;

@RunWith(AndroidJUnit4.class)
public class BaseEditorControllerTest {
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Loggers.setFactory(TextEditLogger::new);
    }

    @Test
    public void test_edit_controller_should_be_created() throws IOException {
        // 准备
        String text = "code remote\n欢迎！\n";
        File tmpDir = context.getCacheDir();
        File tmpFile= new File(tmpDir, "tmp.txt");
        StreamUtils.writeStringToFile(text, tmpFile);
        Uri uri = toUri(tmpFile);
        // fromUri
        BaseEditorController editorCntl = new BaseEditorController(context, uri);
        assertEquals("code remote\n", editorCntl.getTextEdit().getText(0));


        // fromText
        BaseEditorController editorCntl_ = new BaseEditorController(context, text);
        assertEquals("code remote\n", editorCntl_.getTextEdit().getText(0));
    }

    @Test
    public void test_edit_controller_operation_should_be_corrected() throws IOException {
        //
        String text = "code remote\r\n欢迎！";
        File tmpFile= new File(context.getCacheDir(), "tmp.txt");
        StreamUtils.writeStringToFile(text, tmpFile);
        Uri uri = toUri(tmpFile);
        // fromUri
        BaseEditorController editorController = new BaseEditorController(context, uri);
        editorController.dispatch(new EditorAction.Jump(1, 3));
        editorController.dispatch(new EditorAction.Insert("\r\n"));
        editorController.dispatch(new EditorAction.Jump(2, 0));
        editorController.dispatch(new EditorAction.Insert("Cr welcome!"));
        editorController.dispatch(new EditorAction.Jump(0,0));
        editorController.dispatch(new EditorAction.Delete(1));
        editorController.dispatch(new EditorAction.Insert("C"));
        editorController.dispatch(new EditorAction.Jump(1, 1));
        editorController.dispatch(new EditorAction.Backspace(1));
        editorController.dispatch(new EditorAction.Insert("热烈"));
        editorController.dispatch(new EditorAction.Jump(1, editorController.getCursor().col+1));
        editorController.dispatch(new EditorAction.Insert("接"));
        editorController.dispatch(new EditorAction.Flush());            // 两次相连的插入会被判定为一次插入
        editorController.dispatch(new EditorAction.Insert("你"));
        editorController.dispatch(new EditorAction.Undo());
        editorController.dispatch(new EditorAction.Undo());
        editorController.dispatch(new EditorAction.Redo());
        editorController.dispatch(new EditorAction.Insert("春节"));

        // expected text :
        // Code remote
        // 热烈迎接春节！
        // Cr welcome!

        TextEdit textEdit = editorController.getTextEdit();
        for (int i = 0; i < textEdit.rows(); i++) {
            Log.d("BaseEditorControllerTest", textEdit.getText(i));
        }
        assertEquals("Code remote\r\n", textEdit.getText(0));
        assertEquals("热烈迎接春节！\r\n", textEdit.getText(1));
        assertEquals("Cr welcome!", textEdit.getText(2));
    }



    // =============
    private Uri toUri(File file){
        return FileProvider.getUriForFile(
                context,
                context.getPackageName()+".fileprovider",
                file
        );
    }
}