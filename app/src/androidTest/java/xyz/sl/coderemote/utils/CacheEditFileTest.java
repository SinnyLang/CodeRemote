package xyz.sl.coderemote.utils;

import static org.junit.Assert.*;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@RunWith(AndroidJUnit4.class)
public class CacheEditFileTest {
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void test_fromUri_edit_and_commit_should_overwrite_origin() throws Exception {
        // 准备
        String originalText = "Hello Cache Edit!\n你好，缓存编辑。\n";
        File originalFile = new File(context.getCacheDir(), "source.txt");
        writeStringToFile(originalText, originalFile);
        Uri uri = toUri(originalFile);

        // fromUri()
        CacheEditFile cacheEditFile = CacheEditFile.fromUri(context, uri);
        File cacheFile = cacheEditFile.getCacheFile();
        assertTrue(cacheFile.exists());
        assertEquals(originalText, readStringFromFile(cacheFile));

        // 修改缓存文件
        String modifiedText =  "Modified!\n已修改内容";
        writeStringToFile(modifiedText, cacheFile);
        cacheEditFile.commit();
        assertEquals(modifiedText, readStringFromFile(originalFile));

        // 清理缓存
        assertFalse(cacheFile.exists());
    }

    @Test
    public void test_empty_saveAs_should_write_to_target_uri() throws Exception {
        // 创建
        CacheEditFile cacheEditFile = CacheEditFile.empty(context);

        // 写入
        String originalText = "Hello Cache Edit!\n你好，缓存编辑。\n";
        File cacheFile = cacheEditFile.getCacheFile();
        writeStringToFile(originalText, cacheFile);

        // saveAs()
        File targetFile = new File(context.getCacheDir(), "target.txt");
        Uri uri = toUri(targetFile);
        cacheEditFile.saveAs(uri);

        assertEquals(originalText, readStringFromFile(targetFile));

        // 清理缓存
        assertTrue(cacheFile.exists());
        cacheEditFile.cleanup();
        assertFalse(cacheFile.exists());
    }


    // ========

    private Uri toUri(File file){
        return FileProvider.getUriForFile(
                context,
                context.getPackageName()+".fileprovider",
                file
        );
    }

    private void writeStringToFile(String text, File file) throws IOException {
        try (OutputStream out = new FileOutputStream(file, false)){
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String readStringFromFile(File file) throws IOException {
        try (
                InputStream in = new FileInputStream(file);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
        ){
            byte[] buffer = new byte[8192];
            int len = 0;
            while ((len = in.read(buffer))!=-1){
                out.write(buffer, 0, len);
            }

            return out.toString("UTF-8");
        }
    }
}