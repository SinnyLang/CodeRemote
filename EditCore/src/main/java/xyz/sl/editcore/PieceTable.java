package xyz.sl.editcore;

// PieceTable.java (简化)
import java.util.*;

public class PieceTable {
    private final String original; // 原始只读文本
    private final StringBuilder addBuffer = new StringBuilder(); // 新增内容拼接区

    // Piece 表示：来自 original 或 addBuffer 的片段
    static class Piece {
        final boolean fromAdd; // true -> addBuffer, false -> original
        final int offset; // 在对应 buffer 中的起始位置
        int length; // 片段长度
        Piece(boolean fromAdd, int offset, int length) {
            this.fromAdd = fromAdd; this.offset = offset; this.length = length;
        }
    }
    private final LinkedList<Piece> pieces = new LinkedList<>();

    public PieceTable(String original) {
        this.original = original == null ? "" : original;
        if (this.original.length() > 0) pieces.add(new Piece(false, 0, this.original.length()));
    }

    public int length() {
        return pieces.stream().mapToInt(p -> p.length).sum();
    }

    // 将字符串追加到 addBuffer，返回 offset
    private int appendToAddBuffer(String s) {
        int off = addBuffer.length();
        addBuffer.append(s);
        return off;
    }

    // 获取全文
    public String getText() {
        StringBuilder sb = new StringBuilder(length());
        for (Piece p : pieces) {
            if (p.length <= 0) continue;
            if (p.fromAdd) sb.append(addBuffer, p.offset, p.offset + p.length);
            else sb.append(original, p.offset, p.offset + p.length);
        }
        return sb.toString();
    }

    // 获取片段 [from, to)
    public String getText(int from, int to) {
        if (from < 0) from = 0;
        if (to > length()) to = length();
        if (from >= to) return "";
        StringBuilder sb = new StringBuilder(to - from);
        int pos = 0;
        for (Piece p : pieces) {
            if (p.length <= 0) continue;
            int segStart = pos;
            int segEnd = pos + p.length;
            if (segEnd <= from) { pos = segEnd; continue; }
            if (segStart >= to) break;
            int localFrom = Math.max(0, from - segStart);
            int localTo = Math.min(p.length, to - segStart);
            if (p.fromAdd) sb.append(addBuffer, p.offset + localFrom, p.offset + localTo);
            else sb.append(original, p.offset + localFrom, p.offset + localTo);
            pos = segEnd;
        }
        return sb.toString();
    }

    // insert at pos
    public void insert(int pos, String s) {
        if (s == null || s.isEmpty()) return;
        if (pos < 0) pos = 0;
        if (pos > length()) pos = length();
        int addOff = appendToAddBuffer(s);
        // 找到 pieces 中的插入点，可能需要拆分一个 piece
        int cur = 0;
        ListIterator<Piece> it = pieces.listIterator();
        while (it.hasNext()) {
            Piece p = it.next();
            if (cur + p.length >= pos) {
                // 插在这个 piece 内或其前面
                int inner = pos - cur;
                if (inner == 0) {
                    // 插在 p 之前
                    it.previous();
                    it.add(new Piece(true, addOff, s.length()));
                } else if (inner == p.length) {
                    // 插在 p 之后
                    it.add(new Piece(true, addOff, s.length()));
                } else {
                    // 需要拆分 p 为 left + right，在中间插入
                    it.remove();
                    Piece left = new Piece(p.fromAdd, p.offset, inner);
                    Piece right = new Piece(p.fromAdd, p.offset + inner, p.length - inner);
                    it.add(left);
                    it.add(new Piece(true, addOff, s.length()));
                    it.add(right);
                }
                return;
            }
            cur += p.length;
        }
        // 到末尾
        pieces.add(new Piece(true, addOff, s.length()));
    }

    // delete [pos, pos+len)
    public String delete(int pos, int len) {
        if (len <= 0) return "";
        if (pos < 0) pos = 0;
        if (pos >= length()) return "";
        int to = Math.min(length(), pos + len);
        String removed = getText(pos, to);
        // 遍历并修剪 pieces
        int cur = 0;
        ListIterator<Piece> it = pieces.listIterator();
        while (it.hasNext()) {
            Piece p = it.next();
            int segStart = cur;
            int segEnd = cur + p.length;
            if (segEnd <= pos) { cur = segEnd; continue; }
            if (segStart >= to) break;
            // overlap exists
            int delFrom = Math.max(segStart, pos);
            int delTo = Math.min(segEnd, to);
            int localFrom = delFrom - segStart;
            int localTo = delTo - segStart;
            it.remove();
            if (localFrom > 0) {
                it.add(new Piece(p.fromAdd, p.offset, localFrom));
            }
            if (localTo < p.length) {
                it.add(new Piece(p.fromAdd, p.offset + localTo, p.length - localTo));
            }
            cur = segEnd;
        }
        return removed;
    }
}
