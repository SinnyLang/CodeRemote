package xyz.sl.editcore;

// InsertCommand
public class InsertCommand implements EditCommand {
    private final PieceTable pt;
    private final int pos;
    private final String text;
    public InsertCommand(PieceTable pt, int pos, String text) { this.pt = pt; this.pos = pos; this.text = text; }
    @Override public void execute() { pt.insert(pos, text); }
    @Override public void undo() { pt.delete(pos, text.length()); }
}
