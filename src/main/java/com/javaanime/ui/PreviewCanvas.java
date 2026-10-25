package com.javaanime.ui;

import com.javaanime.model.SceneSpec;
import com.javaanime.model.SceneSpec.*;
import com.javaanime.model.SceneSpec.Action;
import com.javaanime.render.SceneRenderer;
import javax.swing.*;
import java.awt.*;

public final class PreviewCanvas extends JPanel {
    private final SceneRenderer renderer = new SceneRenderer();
    private SceneSpec scene = new SceneSpec(Action.WALKING, Setting.PARK, Lighting.DAY, Weather.CLEAR);
    private double seconds;
    private boolean generated;
    public PreviewCanvas() { setBackground(Theme.BACKGROUND); setPreferredSize(new Dimension(700, 394)); }
    public void showScene(SceneSpec scene, double seconds) { this.scene = scene; this.seconds = seconds; generated = true; repaint(); }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        var g = (Graphics2D) graphics.create();
        int width = Math.min(getWidth(), (int) (getHeight() * 16.0 / 9));
        int height = width * 9 / 16;
        int x = (getWidth() - width) / 2, y = (getHeight() - height) / 2;
        g.translate(x, y); renderer.render(g, scene, seconds, width, height);
        if (!generated) {
            g.setColor(new Color(14, 23, 30, 130)); g.fillRect(0, 0, width, height);
            g.setColor(Theme.TEXT); g.setFont(new Font("Segoe UI", Font.BOLD, 24));
            String title = "Your words, in motion.";
            g.drawString(title, (width - g.getFontMetrics().stringWidth(title)) / 2, height / 2 - 4);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            String subtitle = "Describe a scene and select Generate animation";
            g.drawString(subtitle, (width - g.getFontMetrics().stringWidth(subtitle)) / 2, height / 2 + 25);
        }
        g.dispose();
    }
}
