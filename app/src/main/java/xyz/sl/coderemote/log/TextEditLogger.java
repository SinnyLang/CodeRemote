package xyz.sl.coderemote.log;

import android.util.Log;

import com.mammb.code.piecetable.log.Logger;

public class TextEditLogger implements Logger {
    private final String tag;

    public TextEditLogger(String tag) {
        this.tag = tag;
    }

    @Override
    public void debug(String msg) {
        Log.d(tag, msg);
    }

    @Override
    public void info(String msg) {
        Log.i(tag, msg);
    }

    @Override
    public void warn(String msg) {
        Log.w(tag, msg);
    }

    @Override
    public void error(String msg, Throwable t) {
        Log.e(tag, msg, t);
    }
}
