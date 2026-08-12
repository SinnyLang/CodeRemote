package xyz.sl.coderemote

import android.net.Uri
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConstructAndroidUriTest {
    @Test
    fun constructUri() {
        // Construction of this cannot be correctly resolved,
        // such as host, port ...
        val uri = Uri.Builder()
                .scheme("sftp")
                .authority("jocker")
                .path("localhost:22/home")
                .query("pwd")
                .fragment("0")
                .build();
        Assert.assertEquals(
            "sftp://jocker%40localhost%3A22/home/abc/abc.txt?pwd#0",
            uri.toString()
        )

        // Construction of this can be correctly resolved,
        // such as host, port ...
        val uri1 = Uri.Builder()
            .scheme("sftp")
            .encodedAuthority("jocker@localhost:22")
            .encodedPath("/home/abc/abc.txt")
            .encodedQuery("pwd")
            .encodedFragment("0")
            .build()

        Assert.assertEquals(
            "sftp://jocker@localhost:22/home/abc/abc.txt?pwd#0",
            uri1.toString()
        )
    }
}