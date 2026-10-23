package com.dfavisualizer.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultEditorKit;

/** Checks platform editing shortcuts without modifying the system clipboard. */
public class TextShortcutCheck {
    public static void main(String[] args) throws Exception {
        UiTheme.install();
        SwingUtilities.invokeAndWait(() -> {
            JTextField field = new JTextField("replace this");
            int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
            int[] keys = {KeyEvent.VK_X, KeyEvent.VK_C, KeyEvent.VK_V, KeyEvent.VK_A};
            String[] actions = {DefaultEditorKit.cutAction, DefaultEditorKit.copyAction,
                    DefaultEditorKit.pasteAction, DefaultEditorKit.selectAllAction};
            for (int i = 0; i < keys.length; i++) {
                Object action = field.getInputMap().get(KeyStroke.getKeyStroke(keys[i], mask));
                assert actions[i].equals(action) : "Missing platform shortcut: " + actions[i] + " (bound to " + action + ")";
                assert field.getActionMap().get(action) != null : "Shortcut has no editor action";
            }
            field.selectAll();
            String expression = "[a-z]+@[a-z]+\\.[a-z]+";
            assert field.getTransferHandler().importData(field, new StringSelection(expression));
            assert expression.equals(field.getText()) : "Pasting alters the expression";
        });
        System.out.println("Text editing shortcut and paste checks passed");
    }
}
