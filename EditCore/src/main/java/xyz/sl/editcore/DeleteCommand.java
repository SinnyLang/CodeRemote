package xyz.sl.editcore;

// DeleteCommand
public class DeleteCommand implements EditCommand {
    private final PieceTable pt;
    private final int pos;
    private final int len;
    private String deleted;
    public DeleteCommand(PieceTable pt, int pos, int len) { this.pt = pt; this.pos = pos; this.len = len; }
    @Override public void execute() { deleted = pt.delete(pos, len); }
    @Override public void undo() { pt.insert(pos, deleted); }
}