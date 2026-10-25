package com.javaanime.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public final class Theme {
    public static final Color BACKGROUND = new Color(14, 23, 30);
    public static final Color CARD = new Color(22, 34, 43);
    public static final Color FIELD = new Color(31, 46, 57);
    public static final Color TEXT = new Color(232, 241, 238);
    public static final Color MUTED = new Color(151, 173, 178);
    public static final Color ACCENT = new Color(109, 224, 186);
    private Theme() {}
    public static void install() {
        var font = new Font("Segoe UI", Font.PLAIN, 13);
        for (Object key : UIManager.getDefaults().keySet().toArray()) if (key.toString().endsWith(".font")) UIManager.put(key, font);
        UIManager.put("Panel.background", CARD);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("CheckBox.background", CARD);
        UIManager.put("CheckBox.foreground", TEXT);
        UIManager.put("ComboBox.background", FIELD);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.selectionBackground", new Color(41, 82, 76));
        UIManager.put("ComboBox.selectionForeground", TEXT);
        UIManager.put("OptionPane.background", CARD);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("TextField.background", FIELD);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("List.background", FIELD);
        UIManager.put("List.foreground", TEXT);
        UIManager.put("Slider.background", CARD);
        UIManager.put("ProgressBar.foreground", ACCENT);
        UIManager.put("ProgressBar.background", FIELD);
        UIManager.put("ToolTip.background", FIELD);
        UIManager.put("ToolTip.foreground", TEXT);
    }
    public static JLabel label(String text, int size, Color color, boolean bold) {
        var label = new JLabel(text);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color); return label;
    }
    public static Border padding(int amount) { return new EmptyBorder(amount, amount, amount, amount); }
    public static JPanel card(LayoutManager layout) {
        var panel = new JPanel(layout); panel.setBackground(CARD); panel.setBorder(padding(22)); return panel;
    }
    public static JButton button(String text, boolean primary) {
        var button = new JButton(text) {
            @Override protected void paintComponent(Graphics graphics) {
                var g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = primary ? ACCENT : FIELD;
                if (!isEnabled()) fill = primary ? new Color(53, 91, 81) : new Color(28, 40, 49);
                else if (getModel().isRollover()) fill = fill.brighter();
                g.setColor(fill); g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); g.dispose();
                super.paintComponent(graphics);
            }
        };
        button.setForeground(primary ? BACKGROUND : TEXT); button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBorder(new EmptyBorder(11, 16, 11, 16)); button.setContentAreaFilled(false); button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return button;
    }
}
