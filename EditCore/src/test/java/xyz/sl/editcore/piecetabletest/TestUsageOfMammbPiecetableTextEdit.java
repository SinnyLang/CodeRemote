package xyz.sl.editcore.piecetabletest;


import static org.junit.Assert.*;

import com.mammb.code.piecetable.Pos;
import com.mammb.code.piecetable.TextEdit;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class TestUsageOfMammbPiecetableTextEdit {

    String content = """
                    介绍
                    图形处理器（Graphics Processing Unit，GPU）是一种主要用于处理图形和影像的特殊处理器。在计算机图形、深度学习、科学计算等领域，GPU 的并行计算能力被广泛应用。因此，对于使用 GPU 加速的任务来说，检测 GPU 是否可用是非常重要的一步。
                    a
                    本文将介绍如何使用 Python 检测 GPU 是否可用。首先，我们会了解如何检测系统中是否安装了 GPU。然后，我们会介绍如何使用 Python 库来检测和管理 GPU 的使用情况。
                    bc
                    1. 检测系统中的 GPU
                    在使用 GPU 加速之前，我们需要首先检测系统中是否安装了 GPU。可以通过以下几种方式来进行检测：
                    
                    1.1 查看硬件信息
                    在 Windows 系统中，可以通过以下步骤来查看硬件信息：
                    1. 打开「开始」菜单，搜索并点击「设备管理器」。
                    2. 展开「显示适配器」，如果能看到 NVIDIA、AMD 或 Intel 的图形处理器，则表示系统中安装了 GPU。
                    
                    
                    """;
    private TextEdit fileEditor;

    @Before
    public void generateTextFile() throws IOException {
        File file = File.createTempFile("piecrtable_test", ".txt");
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(content);
        }
        System.out.println("Temp file path: " + file.getPath());
        fileEditor = TextEdit.of(file.toPath());
    }

    @Test
    public void testBasicInsertDeleteUndoRedo(){
        TextEdit textEdit = TextEdit.of();
        textEdit.insert(0, 0, "A 这是");
        textEdit.insert(0, 4, " 123");
        textEdit.delete(0, 5, 2);
        assertEquals("A 这是 3", textEdit.getText(0));

        textEdit.undo();
        assertEquals("A 这是 123", textEdit.getText(0));

        textEdit.redo();
        assertEquals("A 这是 3", textEdit.getText(0));
    }

    @Test
    public void testFileWrite() throws IOException {
        File savingFile = File.createTempFile("piecetable_test_save",".txt");
        fileEditor.save(savingFile.toPath());

    }

    @Test
    public void testBasicMethodInTextEditor(){
        // list rows
        System.out.println("========list-rows====");
        for (int row = 0; row < fileEditor.rows(); row++) {
            System.out.print(fileEditor.getText(row));
        }

        System.out.println("========get-text=====");
        System.out.print(fileEditor.getText(0, 1));
        System.out.println(">>>");
        System.out.print(fileEditor.getText(2, 3));
        System.out.println(">>>");
        System.out.print(fileEditor.getText(Pos.of(2, 1), Pos.of(3, 10)));
        System.out.println(">>>");
        List<String> texts = fileEditor.getTexts(Pos.of(2, 0), Pos.of(6, 2));
        System.out.println(texts);

        System.out.println("========insert======");
        fileEditor.insert(0, 1, "🤣🤣🤣");
        System.out.print(fileEditor.getText(0));
        System.out.println(">>>");
        fileEditor.insert(List.of(Pos.of(1,1), Pos.of(2,0), Pos.of(3, 1)), "一一一");
        for (int row = 0; row < fileEditor.rows(); row++) {
            System.out.print(fileEditor.getText(row));
        }

        System.out.println("========undo=======");
        System.out.print(">>undo1>>");
        List<Pos> undo = fileEditor.undo();
        System.out.println(undo);
        for (int row = 0; row < fileEditor.rows(); row++) {
            System.out.print(fileEditor.getText(row));
        }
        System.out.print(">>undo2>>");
        undo = fileEditor.undo();
        System.out.println(undo);
        for (int row = 0; row < fileEditor.rows(); row++) {
            System.out.print(fileEditor.getText(row));
        }
        System.out.print(">>undo3>>");
        undo = fileEditor.undo();
        System.out.println(undo);
        for (int row = 0; row < fileEditor.rows(); row++) {
            System.out.print(fileEditor.getText(row));
        }
        System.out.print(">>undo4>>");
        undo = fileEditor.undo();
        System.out.println(undo);
        for (int row = 0; row < fileEditor.rows(); row++) {
            System.out.print(fileEditor.getText(row));
        }

        System.out.println("========redo=======");
        System.out.print(">>redo1>>");
        List<Pos> redo = fileEditor.redo();
        System.out.println(redo);
        for (int row = 0; row < fileEditor.rows(); row++) {
            System.out.print(fileEditor.getText(row));
        }
        System.out.print(">>redo2>>");
        redo = fileEditor.redo();
        System.out.println(redo);
        for (int row = 0; row < fileEditor.rows(); row++) {
            System.out.print(fileEditor.getText(row));
        }
        System.out.print(">>redo3>>");
        redo = fileEditor.redo();
        System.out.println(redo);
        for (int row = 0; row < fileEditor.rows(); row++) {
            System.out.print(fileEditor.getText(row));
        }
        System.out.print(">>redo4>>");
        redo = fileEditor.redo();
        System.out.println(redo);
        for (int row = 0; row < fileEditor.rows(); row++) {
            System.out.print(fileEditor.getText(row));
        }


    }
}




























