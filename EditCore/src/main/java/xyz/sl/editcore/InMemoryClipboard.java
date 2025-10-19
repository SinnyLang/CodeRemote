package xyz.sl.editcore;

public class InMemoryClipboard implements Clipboard {
    private String content = "";
    @Override public void set(String s) { content = s == null ? "" : s; }
    @Override public String get() { return content; }
}

