package xyz.sl.editcore;

import static org.junit.Assert.*;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

public class BasicTest {
    @Test
    public void testUri() throws URISyntaxException {
        URI uri = this.getClass().getClassLoader().getResource("text-file1.txt").toURI();
        System.out.println(uri);
    }

    @Test
    public void testEmo(){
        String s1 = "1中🤣，🤣🤣";
        System.out.println(s1.length());
        assertEquals(9, s1.length());
    }
}
