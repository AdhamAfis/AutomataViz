package com.dfavisualizer.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import com.dfavisualizer.ui.MainFrame;
import com.dfavisualizer.ui.UiTheme;

public class ThemeChooserDialog extends JDialog {
    private final JTextField search = new JTextField();
    private final JComboBox<String> filter = new JComboBox<>(new String[] {"All themes", "Core themes", "Light themes", "Dark themes"});
    private final DefaultListModel<UiTheme.Theme> model = new DefaultListModel<>();
    private final JList<UiTheme.Theme> themes = new JList<>(model);
    private final JLabel summary = new JLabel();

    public ThemeChooserDialog(MainFrame app) {
        super(app.getFrame(), "Themes", false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(380, 540);
        setMinimumSize(new java.awt.Dimension(320, 360));
        setLocationRelativeTo(app.getFrame());
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setContentPane(content);
        JPanel header = new JPanel(new BorderLayout(6, 6));
        header.add(new JLabel("Select a theme to preview it in the workspace."), BorderLayout.NORTH);
        search.putClientProperty("JTextField.placeholderText", "Search themes");
        search.getAccessibleContext().setAccessibleName("Search themes");
        search.setName("themes.search");
        filter.getAccessibleContext().setAccessibleName("Theme category");
        header.add(search, BorderLayout.CENTER); header.add(filter, BorderLayout.SOUTH);
        content.add(header, BorderLayout.NORTH);
        themes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        themes.getAccessibleContext().setAccessibleName("Available themes");
        themes.setName("themes.list");
        content.add(new JScrollPane(themes), BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(6, 6));
        footer.add(summary, BorderLayout.NORTH);
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.add(close); footer.add(buttons, BorderLayout.SOUTH);
        content.add(footer, BorderLayout.SOUTH);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { reload(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { reload(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { reload(); }
        });
        filter.addActionListener(e -> reload());
        themes.addListSelectionListener(e -> {
            UiTheme.Theme selected = themes.getSelectedValue();
            if (e.getValueIsAdjusting() || selected == null || selected.className.equals(UIManager.getLookAndFeel().getClass().getName())) return;
            try {
                app.applyTheme(selected);
                summary.setText(selected.name + " · " + (selected.dark ? "dark" : "light"));
                try { UiTheme.remember(selected); }
                catch (java.util.prefs.BackingStoreException | RuntimeException failure) {
                    summary.setText("Applied; could not save theme for next launch.");
                    summary.setToolTipText(failure.getMessage());
                }
            } catch (RuntimeException failure) {
                javax.swing.JOptionPane.showMessageDialog(this, failure.getMessage(), "Unable to change theme", javax.swing.JOptionPane.ERROR_MESSAGE);
                reload();
            }
        });
        reload();
    }

    private void reload() {
        String query = search.getText().toLowerCase(Locale.ROOT).trim();
        String current = UIManager.getLookAndFeel().getClass().getName();
        model.clear();
        UiTheme.Theme selected = null;
        for (UiTheme.Theme theme : UiTheme.THEMES) {
            int mode = filter.getSelectedIndex();
            if (!theme.name.toLowerCase(Locale.ROOT).contains(query) || mode == 1 && !theme.core
                    || mode == 2 && theme.dark || mode == 3 && !theme.dark) continue;
            model.addElement(theme);
            if (theme.className.equals(current)) selected = theme;
        }
        themes.setSelectedValue(selected, true);
        summary.setText(model.size() + " themes" + (selected == null ? "" : " · " + selected.name));
    }
}
