package xyz.sl.editcore;

public interface Editor {
    int getLength();
    String getText(); // 取得全部文本（用于保存、显示）
    String getText(int from, int to); // 片段

    void insert(int pos, String text); // 插入
    void delete(int pos, int length); // 删除
    void cut(int from, int to); // 剪切（将选区放入剪贴板并删除）
    void paste(int pos); // 从剪贴板粘贴到 pos

    boolean canUndo();
    boolean canRedo();
    void undo();
    void redo();

    void beginCompoundChange(); // 可选：将多次操作组成一个复合操作（用于复杂粘贴/批量替换）
    void endCompoundChange();
}
