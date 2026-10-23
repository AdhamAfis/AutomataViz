package com.dfavisualizer.ui;
import java.awt.*;
import javax.swing.*;
import com.dfavisualizer.algorithm.*;
import com.dfavisualizer.ui.components.*;
public class LearningReliabilityCheck {
    public static void main(String[] args) throws Exception {
        UiTheme.install();
        SwingUtilities.invokeAndWait(() -> {
            NfaLessonPanel lesson = new NfaLessonPanel(new RegexParser().parse("a*"), java.util.Collections.emptyMap());
            lesson.start("aa"); lesson.next(); lesson.next();
            JTextField input = find(lesson, JTextField.class);
            input.setText("b");
            button(lesson, "Focus active states").doClick(0);
            assert !button(lesson, "Next:").isEnabled() : "Focus resurrects a stale trace after input edits";
            assert lesson.explanation().contains("Input edited") : "Focus hides the pending reset instruction";
            button(lesson, "Start / Reset").doClick(0);
            button(lesson, "Next:").doClick(0); button(lesson, "Next:").doClick(0);
            assert lesson.explanation().contains("REJECTED") : "Missing transitions should reject immediately";
            assert !button(lesson, "Next:").isEnabled() : "Rejected trace still advances";
            button(lesson, "Back").doClick(0);
            assert lesson.consumed() == 0 : "Back failed to restore before rejection";
        });
        System.out.println("Learning controls checks passed (editing, focus, reset, rejection, back)");
    }
    static <T> T find(Container parent, Class<T> type) { if(type.isInstance(parent))return type.cast(parent);for(Component c:parent.getComponents())if(c instanceof Container){T r=find((Container)c,type);if(r!=null)return r;}return null; }
    static AbstractButton button(Container parent,String text) { for(Component c:parent.getComponents()){if(c instanceof AbstractButton && ((AbstractButton)c).getText().startsWith(text))return (AbstractButton)c;if(c instanceof Container){AbstractButton r=button((Container)c,text);if(r!=null)return r;}}return null; }
}
