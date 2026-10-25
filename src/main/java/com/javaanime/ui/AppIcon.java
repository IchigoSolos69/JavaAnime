package com.javaanime.ui;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

public final class AppIcon {
    private AppIcon() {}
    public static BufferedImage image() {
        var image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB); var g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Theme.BACKGROUND); g.fillRoundRect(0, 0, 256, 256, 56, 56);
        g.setColor(Theme.ACCENT); g.fillRoundRect(39, 69, 178, 133, 23, 23);
        g.setColor(new Color(47, 109, 102)); g.fillRoundRect(39, 47, 178, 42, 13, 13);
        g.setColor(Theme.ACCENT);
        for (int x = 57; x < 206; x += 47) { var stripe = new Path2D.Double(); stripe.moveTo(x, 47); stripe.lineTo(x + 23, 47); stripe.lineTo(x + 7, 83); stripe.lineTo(x - 16, 83); stripe.closePath(); g.fill(stripe); }
        g.setColor(Theme.BACKGROUND); var play = new Path2D.Double(); play.moveTo(111, 111); play.lineTo(111, 174); play.lineTo(165, 143); play.closePath(); g.fill(play);
        g.dispose(); return image;
    }
}
