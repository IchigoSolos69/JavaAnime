package com.javaanime;

import com.javaanime.ui.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public final class Main {
    private Main() {}
    public static void main(String[] args) throws Exception {
        if (args.length > 0) { Diagnostics.run(args); return; }
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            var panel = new MainPanel(true);
            var window = new JFrame("JavaAnime — Offline animation studio");
            window.setIconImage(AppIcon.image());
            window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            window.setContentPane(panel); window.setMinimumSize(new Dimension(1080, 760)); window.setSize(1250, 860);
            window.setLocationRelativeTo(null);
            window.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) { panel.shutdown(); window.dispose(); }
            });
            window.setVisible(true);
        });
    }
}
