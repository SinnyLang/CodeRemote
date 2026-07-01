package xyz.sl.coderemote.utils;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;

public class EditFileManger {
    // EditFileManger 的生命周期与应用相同
    private Context context;

    public EditFileManger(Context context) {
        // TODO : 需要判断 context 是否能转换为 ApplicationContext 类型，
        //  如果不能则可能出现内存泄漏
        this.context = context;
    }

    public CacheEditFile getCacheFileEmpty() throws IOException {
        return CacheEditFile.empty(context);
    }

    public CacheEditFile getCacheFile(Uri uri) throws IOException {
        return CacheEditFile.fromUri(context, uri);
    }
}
