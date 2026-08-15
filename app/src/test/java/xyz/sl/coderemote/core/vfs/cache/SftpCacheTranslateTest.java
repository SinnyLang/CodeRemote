package xyz.sl.coderemote.core.vfs.cache;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SftpCacheTranslateTest {

    @Test
    public void getCacheResourceUri() {
        // Desktop/a.txt
        String base = "/C%3A/Users/jocker/Desktop";
        String path = "/C%3A/Users/jocker/Desktop/a.txt";
        String relativePath = SftpCacheTranslate.calcRelativePath(base, path);
        System.out.println(relativePath);
        assertEquals("Desktop/a.txt", relativePath);
    }
}