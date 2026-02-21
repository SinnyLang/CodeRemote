package xyz.sl.coderemote.core;

import java.util.List;

public class PositionMapper {
    public int toOffset(Cursor cursor) {
        return lineStartOffsets.get(cursor.row) + cursor.col;
    }

    public Cursor fromOffset(int offset){
        int size = lineStartOffsets.size();

        for (int i = 0; i < size; i++) {
            if (lineStartOffsets.get(i) > offset ) {
                return new Cursor(i-1, offset - lineStartOffsets.get(i-1));
            }
        }

        return new Cursor(size - 1 , offset - lineStartOffsets.get(size - 1));
    }

    private List<Integer> lineStartOffsets;

    public PositionMapper(List<Integer> lineStartOffsets){
        this.lineStartOffsets = lineStartOffsets;
    }

    public void setLineStartOffsets(List<Integer> lineStartOffsets) {
        this.lineStartOffsets = lineStartOffsets;
    }
}
