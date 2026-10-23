package com.dfavisualizer.ui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.dfavisualizer.ui.UiTheme;
import com.dfavisualizer.controller.AnimationController;

public class StatusPanel extends JPanel {
    private JTextArea statusArea;
    private AnimationController playback;
    private final JPanel playbackControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
    private final JLabel inputRibbon = new JLabel();
    private final JButton pauseButton = new JButton("Pause");
    private final JButton backButton = new JButton("Back");
    private final JCheckBox focusMode = new JCheckBox("Focus graph", true);
    private final JButton nextButton = new JButton("Next Step");
    private final JButton restartButton = new JButton("Restart");
    private final JComboBox<String> speedSelector = new JComboBox<>(new String[] {"0.5×", "1×", "2×"});
    
    public StatusPanel() {
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        setPreferredSize(new Dimension(0, 100));
        setMinimumSize(new Dimension(0, 72));
        JLabel heading = new JLabel("Simulation & results");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 12f));
        JPanel header = new JPanel(new BorderLayout(8, 4));
        header.add(heading, BorderLayout.WEST);
        pauseButton.setPreferredSize(new Dimension(80, pauseButton.getPreferredSize().height));
        UiTheme.quietButton(backButton);
        UiTheme.quietButton(restartButton);
        UiTheme.quietButton(nextButton);
        UiTheme.quietButton(pauseButton);
        pauseButton.addActionListener(e -> playback.togglePause());
        backButton.addActionListener(e -> playback.stepBack());
        focusMode.setToolTipText("Fade unrelated transitions; uncheck for the full graph");
        focusMode.addActionListener(e -> playback.setFocusMode(focusMode.isSelected()));
        nextButton.addActionListener(e -> playback.stepForward());
        restartButton.addActionListener(e -> playback.restart());
        focusMode.setForeground(UiTheme.MUTED);
        focusMode.setFont(focusMode.getFont().deriveFont(12f));
        speedSelector.setSelectedIndex(1);
        speedSelector.getAccessibleContext().setAccessibleName("Playback speed");
        speedSelector.addActionListener(e -> {
            if (playback != null) playback.setSpeed(new double[] {0.5, 1, 2}[speedSelector.getSelectedIndex()]);
        });
        playbackControls.add(focusMode);
        playbackControls.add(backButton);
        playbackControls.add(pauseButton);
        playbackControls.add(nextButton);
        playbackControls.add(restartButton);
        playbackControls.add(new JLabel("Speed"));
        playbackControls.add(speedSelector);
        header.add(playbackControls, BorderLayout.EAST);
        inputRibbon.setName("playback.input");
        inputRibbon.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        header.add(inputRibbon, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);
        hidePlayback();
        
        statusArea = new JTextArea("Enter a regular expression above, then choose Visualize DFA.");
        statusArea.setEditable(false);
        statusArea.setFont(statusArea.getFont().deriveFont(13f));
        statusArea.setForeground(UiTheme.TEXT);
        statusArea.setBackground(javax.swing.UIManager.getColor("TextArea.background"));
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setMargin(new Insets(6, 8, 6, 8));
        
        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setBorder(BorderFactory.createEmptyBorder());
        
        add(statusScroll, BorderLayout.CENTER);
    }
    
    public void setStatus(String text) {
        statusArea.setForeground(text.startsWith("ACCEPTED") ? java.awt.Color.decode(UiTheme.START_STROKE)
                : text.startsWith("REJECTED") || text.startsWith("Error:") ? java.awt.Color.decode(UiTheme.DEAD_STROKE) : UiTheme.TEXT);
        statusArea.setText(text);
        statusArea.setCaretPosition(0);
    }
    
    public void appendStatus(String text) {
        statusArea.append(text + "\n");
        statusArea.setCaretPosition(statusArea.getDocument().getLength());
    }
    
    public void clearStatus() {
        statusArea.setText("");
    }
    
    public JTextArea getStatusArea() {
        return statusArea;
    }

    public void bindPlayback(AnimationController controller) { playback = controller; }

    public void showPlayback(String input, int position, boolean paused, boolean finished, boolean canBack) {
        int start = Math.max(0, position - 10);
        int end = Math.min(input.length(), position + 22);
        String consumed = html(input.substring(start, position));
        String active = position < input.length() ? html(input.substring(position, position + 1)) : "";
        String remaining = html(input.substring(Math.min(position + 1, input.length()), end));
        inputRibbon.setText("<html>Input " + position + " / " + input.length() + " &nbsp; "
                + (start > 0 ? "…" : "") + "<span style='color:" + UiTheme.hex(UiTheme.MUTED) + "'>" + consumed + "</span>"
                + "<b style='background-color:" + UiTheme.INPUT_BACKGROUND + ";color:" + UiTheme.INPUT_FOREGROUND + "'>" + active + "</b>" + remaining
                + (end < input.length() ? "…" : "") + (input.isEmpty() ? "ε (empty input)" : "") + "</html>");
        inputRibbon.setToolTipText("<html>" + html(input) + "</html>");
        pauseButton.setText(paused ? "Resume" : "Pause");
        pauseButton.setEnabled(!finished);
        nextButton.setEnabled(!finished);
        backButton.setEnabled(canBack);
        playbackControls.setVisible(true);
        inputRibbon.setVisible(true);
        setPreferredSize(new Dimension(0, 124));
        setMinimumSize(new Dimension(0, 110));
        revalidate();
    }

    public void hidePlayback() {
        playbackControls.setVisible(false);
        inputRibbon.setVisible(false);
        setPreferredSize(new Dimension(0, 100));
        setMinimumSize(new Dimension(0, 72));
        revalidate();
    }

    private static String html(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace(" ", "␣").replace("\n", "↵").replace("\t", "⇥");
    }
} 