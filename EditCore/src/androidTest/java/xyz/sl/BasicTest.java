package xyz.sl;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

public class BasicTest {
    @Test
    public void testUri() throws URISyntaxException {
        URI uri = this.getClass().getClassLoader().getResource("text-file.txt").toURI();
        System.out.println(uri);

        // [sout] jar:file:/data/app/xyz.sl.editcore.test-OcHk9YyGtoR47GjlAz0bLA==/base.apk!/text-file.txt
    }
}
