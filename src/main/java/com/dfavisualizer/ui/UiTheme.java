package com.dfavisualizer.ui;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractButton;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.text.DefaultEditorKit;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.plaf.UIResource;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.LineBorder;
import com.formdev.flatlaf.ui.FlatLineBorder;

public final class UiTheme {
    public static Color BACKGROUND = new Color(0x3C3F41);
    public static Color BORDER = new Color(0x626669);
    public static Color TEXT = new Color(0xDDDDDD);
    public static Color MUTED = new Color(0xB0B4BA);
    public static Color ACCENT = new Color(0x4B6EAF);
    public static String CANVAS_COLOR = "#303335";
    public static Color CANVAS = Color.decode(CANVAS_COLOR);
    public static String DFA_FILL = "#344559";
    public static String NFA_FILL = "#443F59";
    public static String START_FILL = "#29473C";
    public static String ACCEPT_FILL = "#514631";
    public static String DEAD_FILL = "#513638";
    public static String STATE_TEXT = "#E2E6EB";
    public static String STATE_STROKE = "#9CA7B3";
    public static String START_STROKE = "#83C792";
    public static String DEAD_STROKE = "#EE9B98";
    public static String EDGE = "#B6BEC7";
    public static String LOOP = "#B4A8F0";
    public static String EPSILON = "#EE9B98";
    public static String ACTIVE_STATE = "#8CB4FF";
    public static String ACTIVE_EDGE = "#F2BD71";

    public static String INPUT_BACKGROUND = "#365880";
    public static String INPUT_FOREGROUND = "#F1F4F8";
    public static Color SOURCE_HIGHLIGHT = new Color(92, 76, 37);
    public static final List<Theme> THEMES = createThemes();

    private UiTheme() {}

    public static final class Theme {
        public final String name, className;
        public final boolean dark, core;
        private Theme(String name, String className, boolean dark, boolean core) {
            this.name = name; this.className = className; this.dark = dark; this.core = core;
        }
        @Override public String toString() { return name; }
    }

    private static List<Theme> createThemes() {
        List<Theme> themes = new ArrayList<>();
        themes.add(new Theme("Flat Light", "com.formdev.flatlaf.FlatLightLaf", false, true));
        themes.add(new Theme("Flat Dark", FlatDarkLaf.class.getName(), true, true));
        themes.add(new Theme("Flat IntelliJ", "com.formdev.flatlaf.FlatIntelliJLaf", false, true));
        themes.add(new Theme("Flat Darcula", "com.formdev.flatlaf.FlatDarculaLaf", true, true));
        themes.add(new Theme("Flat macOS Light", "com.formdev.flatlaf.themes.FlatMacLightLaf", false, true));
        themes.add(new Theme("Flat macOS Dark", "com.formdev.flatlaf.themes.FlatMacDarkLaf", true, true));
        for (FlatAllIJThemes.FlatIJLookAndFeelInfo info : FlatAllIJThemes.INFOS)
            themes.add(new Theme(info.getName(), info.getClassName(), info.isDark(), false));
        return Collections.unmodifiableList(themes);
    }

    public static void installPreferred() {
        try {
            String saved = preferences().get("theme", FlatDarkLaf.class.getName());
            Theme theme = THEMES.stream().filter(t -> t.className.equals(saved)).findFirst().orElse(THEMES.get(1));
            configure(theme);
        } catch (RuntimeException e) {
            System.err.println("Saved theme unavailable; using Flat Dark: " + e.getMessage());
            install();
        }
    }

    private static java.util.prefs.Preferences preferences() {
        return java.util.prefs.Preferences.userNodeForPackage(UiTheme.class);
    }

    public static void remember(Theme theme) throws java.util.prefs.BackingStoreException {
        preferences().put("theme", theme.className);
        preferences().flush();
    }

    public static String hex(Color color) { return String.format("#%06X", color.getRGB() & 0xFFFFFF); }

    private static void updatePalette() {
        boolean dark = FlatLaf.isLafDark();
        BACKGROUND = new Color(UIManager.getColor("Panel.background").getRGB());
        BORDER = new Color(UIManager.getColor("Component.borderColor").getRGB());
        TEXT = new Color(UIManager.getColor("Label.foreground").getRGB());
        MUTED = Color.decode(dark ? "#B0B4BA" : "#5C6470");
        ACCENT = new Color(UIManager.getColor("Component.accentColor").getRGB());
        CANVAS_COLOR = dark ? "#303335" : "#FFFFFF";
        CANVAS = Color.decode(CANVAS_COLOR);
        DFA_FILL = dark ? "#344559" : "#E6F2FF";
        NFA_FILL = dark ? "#443F59" : "#F2E6FF";
        START_FILL = dark ? "#29473C" : "#E6FFCC";
        ACCEPT_FILL = dark ? "#514631" : "#FFEBCC";
        DEAD_FILL = dark ? "#513638" : "#FFCCCC";
        STATE_TEXT = dark ? "#E2E6EB" : "#1E293B";
        STATE_STROKE = dark ? "#9CA7B3" : "#64748B";
        START_STROKE = dark ? "#83C792" : "#15803D";
        DEAD_STROKE = dark ? "#EE9B98" : "#B91C1C";
        EDGE = dark ? "#B6BEC7" : "#475569";
        LOOP = dark ? "#B4A8F0" : "#4F46E5";
        EPSILON = DEAD_STROKE;
        ACTIVE_STATE = dark ? "#8CB4FF" : "#2556C4";
        ACTIVE_EDGE = dark ? "#F2BD71" : "#B45309";
        INPUT_BACKGROUND = dark ? "#365880" : "#DBEAFE";
        INPUT_FOREGROUND = dark ? "#F1F4F8" : "#1E3A8A";
        SOURCE_HIGHLIGHT = dark ? new Color(92, 76, 37) : new Color(253, 230, 138);
    }

    private static String[] palette() {
        return new String[] {hex(BACKGROUND), hex(BORDER), hex(TEXT), hex(MUTED), hex(ACCENT), CANVAS_COLOR,
                DFA_FILL, NFA_FILL, START_FILL, ACCEPT_FILL, DEAD_FILL, STATE_TEXT, STATE_STROKE,
                START_STROKE, DEAD_STROKE, EDGE, LOOP, ACTIVE_STATE, ACTIVE_EDGE};
    }

    /** Recolor existing cells and components so user layout, input, and playback position survive. */
    public static void apply(Theme theme) {
        String[] before = palette();
        Color oldBackground = BACKGROUND, oldCanvas = CANVAS;
        configure(theme);
        String[] after = palette();
        Map<String, String> colors = new LinkedHashMap<>();
        for (int i = 0; i < before.length; i++) colors.put(before[i], after[i]);
        for (Window window : Window.getWindows()) if (window.isDisplayable()) recolor(window, colors, oldBackground, oldCanvas);
        FlatLaf.updateUI();
        for (Window window : Window.getWindows()) if (window.isDisplayable()) refreshHighlights(window);
    }

    public static String recolor(String style, Map<String, String> colors) {
        if (style == null) return null;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("#[0-9a-fA-F]{6}").matcher(style);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) matcher.appendReplacement(result, colors.getOrDefault(matcher.group().toUpperCase(java.util.Locale.ROOT), matcher.group()));
        matcher.appendTail(result);
        return result.toString();
    }

    private static Color recolor(Color color, Map<String, String> colors) {
        if (color == null || color instanceof UIResource) return color;
        String replacement = colors.get(hex(color));
        return replacement == null ? color : Color.decode(replacement);
    }

    private static void recolor(Component component, Map<String, String> colors, Color oldBackground, Color oldCanvas) {
        Color background = component.getBackground();
        if (!(background instanceof UIResource)) {
            if (component instanceof javax.swing.JLabel && component.isOpaque() && oldCanvas.equals(background)) component.setBackground(CANVAS);
            else if (oldBackground.equals(background)) component.setBackground(BACKGROUND);
            else component.setBackground(recolor(background, colors));
        }
        component.setForeground(recolor(component.getForeground(), colors));
        if (component instanceof JComponent) {
            JComponent jc = (JComponent) component;
            jc.setBorder(recolor(jc.getBorder(), component, colors));
            if (jc instanceof javax.swing.JMenu) recolor(((javax.swing.JMenu) jc).getPopupMenu(), colors, oldBackground, oldCanvas);
            if (jc.getComponentPopupMenu() != null) recolor(jc.getComponentPopupMenu(), colors, oldBackground, oldCanvas);
        }
        if (component instanceof Container)
            for (Component child : ((Container) component).getComponents()) recolor(child, colors, oldBackground, oldCanvas);
        if (component instanceof AutomataGraph) ((AutomataGraph) component).applyPalette(colors);

    }

    private static void refreshHighlights(Component component) {
        if (component instanceof com.dfavisualizer.ui.components.ExpressionGuidePanel)
            ((com.dfavisualizer.ui.components.ExpressionGuidePanel) component).refreshHighlight();
        if (component instanceof Container)
            for (Component child : ((Container) component).getComponents()) refreshHighlights(child);
    }

    private static Border recolor(Border border, Component owner, Map<String, String> colors) {
        if (border instanceof FlatLineBorder) {
            FlatLineBorder flat = (FlatLineBorder) border;
            return new FlatLineBorder(flat.getUnscaledBorderInsets(), recolor(flat.getLineColor(), colors), flat.getLineThickness(), flat.getArc());
        }
        if (border == null || border instanceof UIResource) return border;
        if (border instanceof CompoundBorder) {
            CompoundBorder compound = (CompoundBorder) border;
            return new CompoundBorder(recolor(compound.getOutsideBorder(), owner, colors), recolor(compound.getInsideBorder(), owner, colors));
        }
        if (border instanceof MatteBorder) {
            MatteBorder matte = (MatteBorder) border;
            if (matte.getMatteColor() != null) return new MatteBorder(matte.getBorderInsets(), recolor(matte.getMatteColor(), colors));
        }
        if (border instanceof LineBorder) {
            LineBorder line = (LineBorder) border;
            return new LineBorder(recolor(line.getLineColor(), colors), line.getThickness(), line.getRoundedCorners());
        }
        return border;
    }

    public static void install() { configure(THEMES.get(1)); }

    private static void configure(Theme theme) {
        try {
            UIManager.setLookAndFeel(theme.className);
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
            throw new IllegalArgumentException("Unable to load theme " + theme.name, e);
        }
        updatePalette();
        // Keep native Flat Dark component styling; override only workspace density and scrollbar contrast.
        UIManager.put("Button.minimumWidth", 0);
        UIManager.put("TabbedPane.tabHeight", 30);
        UIManager.put("TabbedPane.tabInsets", new Insets(4, 14, 4, 14));
        UIManager.put("SplitPane.background", BACKGROUND);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("ScrollBar.thumb", MUTED);
        UIManager.put("ScrollBar.hoverThumbColor", FlatLaf.isLafDark() ? new Color(0xC4C8CE) : new Color(0x424A56));
        UIManager.put("ScrollBar.pressedThumbColor", TEXT);
        // Retain platform editing shortcuts even if another look and feel changes the defaults.
        InputMap textKeys = (InputMap) UIManager.get("TextField.focusInputMap");
        int shortcut = GraphicsEnvironment.isHeadless() ? InputEvent.CTRL_DOWN_MASK
                : Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        textKeys.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, shortcut), DefaultEditorKit.cutAction);
        textKeys.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcut), DefaultEditorKit.copyAction);
        textKeys.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcut), DefaultEditorKit.pasteAction);
        textKeys.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, shortcut), DefaultEditorKit.selectAllAction);
    }

    public static Border surfaceBorder(int top, int left, int bottom, int right) {
        return new FlatLineBorder(new Insets(top, left, bottom, right), BORDER, 1, 0);
    }

    public static void quietButton(AbstractButton button) {
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setForeground(MUTED);
        button.setMargin(new Insets(4, 8, 4, 8));
    }
}
