package xyz.sl.editcore;

// EditorImpl
public class SimpleEditor implements Editor {
    private final PieceTable pt;
    private final UndoManager um = new UndoManager();
    private final Clipboard clipboard;
    private CompositeCommand currentCompound = null;

    public SimpleEditor(String initial, Clipboard clipboard) {
        this.pt = new PieceTable(initial == null ? "" : initial);
        this.clipboard = clipboard == null ? new InMemoryClipboard() : clipboard;
    }

    private void execCmd(EditCommand c) {
        if (currentCompound != null) {
            currentCompound.add(c);
            c.execute();
        } else {
            um.executeCommand(c);
        }
    }

    @Override public int getLength() { return pt.length(); }
    @Override public String getText() { return pt.getText(); }
    @Override public String getText(int from, int to) { return pt.getText(from, to); }

    @Override
    public void insert(int pos, String text) { execCmd(new InsertCommand(pt, pos, text)); }

    @Override
    public void delete(int pos, int length) { execCmd(new DeleteCommand(pt, pos, length)); }

    @Override
    public void cut(int from, int to) {
        if (from >= to) return;
        CompositeCommand cc = new CompositeCommand();
        String removed = pt.getText(from, to);
        // 1. set clipboard (not an EditCommand because it doesn't modify pieceTable; but to make cut undoable we embed it)
        cc.add(new EditCommand(){
            @Override public void execute(){ clipboard.set(removed); }
            @Override public void undo(){ clipboard.set(""); } // 简单回退策略
        });
        // 2. delete
        cc.add(new DeleteCommand(pt, from, to - from));
        execCmd(cc);
    }

    @Override
    public void paste(int pos) {
        String toPaste = clipboard.get();
        if (toPaste == null || toPaste.isEmpty()) return;
        execCmd(new InsertCommand(pt, pos, toPaste));
    }

    @Override public boolean canUndo() { return um.canUndo() || currentCompound != null; }
    @Override public boolean canRedo() { return um.canRedo(); }
    @Override public void undo() { um.undo(); }
    @Override public void redo() { um.redo(); }

    @Override
    public void beginCompoundChange() { currentCompound = new CompositeCommand(); }

    @Override
    public void endCompoundChange() {
        if (currentCompound == null) return;
        // 将 compound 作为一个命令提交到 UndoManager
        CompositeCommand cc = currentCompound;
        currentCompound = null;
        um.executeCommand(cc);
    }
}
