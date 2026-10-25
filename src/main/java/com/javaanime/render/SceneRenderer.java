package com.javaanime.render;

import com.javaanime.model.SceneSpec;
import com.javaanime.model.SceneSpec.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/** All movement is a function of scene + seconds, shared by preview and video export. */
public final class SceneRenderer {
    public BufferedImage frame(SceneSpec scene, double seconds, int width, int height) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = image.createGraphics();
        try { render(g, scene, seconds, width, height); } finally { g.dispose(); }
        return image;
    }

    public void render(Graphics2D target, SceneSpec scene, double seconds, int width, int height) {
        Graphics2D g = (Graphics2D) target.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.scale(width / 1280.0, height / 720.0);
            g.clipRect(0, 0, 1280, 720);
            boolean night = scene.lighting() == Lighting.NIGHT;
            boolean rain = scene.weather() == Weather.RAIN;
            Color top = night ? color(18, 30, 64) : rain ? color(106, 143, 166) : color(121, 199, 218);
            Color bottom = night ? color(62, 77, 113) : rain ? color(180, 207, 210) : color(222, 241, 226);
            g.setPaint(new GradientPaint(0, 0, top, 0, 570, bottom));
            g.fillRect(0, 0, 1280, 720);
            sky(g, seconds, night, rain);
            switch (scene.setting()) {
                case PARK -> park(g, night);
                case CITY -> city(g, night);
                case BEACH -> beach(g, seconds, night);
            }
            double phase = seconds * switch (scene.action()) {
                case WALKING -> 5.4;
                case RUNNING -> 9.5;
                case JUMPING -> 3.5;
                case WAVING -> 5.2;
                case DANCING -> 6.4;
            };
            double x = 640 + switch (scene.action()) {
                case WALKING -> 205 * Math.sin(seconds * .25);
                case RUNNING -> 290 * Math.sin(seconds * .42);
                case DANCING -> 40 * Math.sin(phase / 2);
                default -> 0;
            };
            double jump = scene.action() == Action.JUMPING ? 105 * Math.pow(Math.max(0, Math.sin(phase)), 1.1) : 0;
            g.setColor(new Color(25, 42, 48, 40));
            g.fill(new Ellipse2D.Double(x - 65 + jump * .15, 600, 130 - jump * .3, 22));
            double bob = switch (scene.action()) {
                case WALKING -> 4 * Math.abs(Math.sin(phase));
                case RUNNING -> 12 * Math.abs(Math.sin(phase));
                case DANCING -> 8 * Math.sin(phase * 2);
                default -> 0;
            };
            character(g, x, 605 - jump - bob, phase, scene.action());
            if (rain) rain(g, seconds, night);
        } finally { g.dispose(); }
    }

    private void sky(Graphics2D g, double t, boolean night, boolean rain) {
        if (night) {
            for (int i = 0; i < 45; i++) {
                double x = (i * 173 + 31) % 1280, y = (i * 71 + 18) % 320;
                int alpha = (int) (130 + 65 * Math.sin(t * 1.3 + i));
                g.setColor(new Color(250, 250, 218, alpha));
                g.fill(new Ellipse2D.Double(x, y, i % 4 == 0 ? 4 : 2, i % 4 == 0 ? 4 : 2));
            }
            g.setColor(color(247, 235, 187)); g.fillOval(1010, 72, 74, 74);
            g.setColor(color(26, 39, 74)); g.fillOval(1033, 57, 68, 72);
        } else if (!rain) {
            g.setColor(new Color(255, 229, 158, 80)); g.fillOval(1000, 46, 160, 160);
            g.setColor(color(255, 232, 166)); g.fillOval(1025, 71, 110, 110);
        }
        g.setColor(night ? new Color(156, 172, 193, 45) : new Color(255, 255, 255, rain ? 135 : 180));
        for (int i = 0; i < 6; i++) {
            double x = ((i * 247 + t * (5 + i)) % 1590) - 220;
            double y = 85 + (i * 47 % 155);
            cloud(g, x, y, .7 + i % 3 * .2);
        }
    }

    private void cloud(Graphics2D g, double x, double y, double size) {
        var transform = g.getTransform();
        g.translate(x, y); g.scale(size, size);
        g.fillOval(0, 32, 170, 40); g.fillOval(25, 5, 75, 65); g.fillOval(75, 16, 76, 55);
        g.setTransform(transform);
    }

    private void park(Graphics2D g, boolean night) {
        g.setColor(night ? color(39, 91, 93) : color(123, 188, 143));
        g.fill(new Ellipse2D.Double(-140, 325, 950, 470));
        g.setColor(night ? color(36, 80, 85) : color(99, 173, 132));
        g.fill(new Ellipse2D.Double(480, 340, 1040, 430));
        g.setColor(night ? color(29, 62, 64) : color(88, 158, 108)); g.fillRect(0, 495, 1280, 225);
        tree(g, 145, 480, 1.25, night); tree(g, 350, 500, .75, night);
        tree(g, 1095, 505, 1.1, night); tree(g, 940, 490, .65, night);
        g.setColor(night ? color(102, 112, 108) : color(218, 216, 190));
        var path = new Path2D.Double();
        path.moveTo(0, 555); path.curveTo(370, 520, 930, 562, 1280, 543); path.lineTo(1280, 660); path.curveTo(880, 700, 385, 627, 0, 660); path.closePath(); g.fill(path);
        g.setColor(night ? color(62, 102, 90) : color(145, 191, 124));
        for (int i = 0; i < 36; i++) {
            int x = (i * 139 + 19) % 1280, y = 675 + i % 5 * 8;
            line(g, x, y, x - 3, y - 10, 2); line(g, x, y, x + 5, y - 7, 2);
        }
        bench(g, 105, 548);
    }

    private void tree(Graphics2D g, int x, int base, double size, boolean night) {
        var transform = g.getTransform(); g.translate(x, base); g.scale(size, size);
        g.setColor(night ? color(84, 73, 67) : color(134, 100, 77)); g.fillRoundRect(-12, -135, 24, 142, 8, 8);
        g.setColor(night ? color(26, 87, 76) : color(45, 135, 96)); g.fillOval(-90, -266, 180, 173);
        g.setColor(night ? color(33, 101, 87) : color(63, 153, 105)); g.fillOval(-72, -283, 144, 138);
        g.setColor(night ? color(45, 112, 94) : color(90, 170, 118)); g.fillOval(-55, -270, 87, 78);
        g.setTransform(transform);
    }

    private void bench(Graphics2D g, int x, int y) {
        g.setColor(color(58, 76, 73)); line(g, x + 10, y, x + 10, y - 60, 7); line(g, x + 125, y, x + 125, y - 60, 7);
        g.setColor(color(160, 112, 79)); g.fillRoundRect(x, y - 59, 137, 13, 4, 4); g.fillRoundRect(x, y - 39, 137, 13, 4, 4); g.fillRoundRect(x - 5, y - 20, 147, 13, 4, 4);
    }

    private void city(Graphics2D g, boolean night) {
        for (int i = 0; i < 11; i++) {
            int x = i * 130 - 45, h = 160 + i * 79 % 150;
            g.setColor(night ? color(35 + i % 3 * 6, 48 + i % 3 * 7, 73 + i % 3 * 8) : color(138 + i % 3 * 13, 172 + i % 3 * 9, 180 + i % 3 * 11));
            g.fillRoundRect(x, 485 - h, 113, h + 10, 4, 4);
            for (int row = 0; row < h / 35 - 1; row++) for (int col = 0; col < 3; col++) {
                g.setColor(night ? ((row + col + i) % 3 == 0 ? color(64, 83, 111) : color(238, 202, 128)) : color(217, 236, 229));
                g.fillRoundRect(x + 15 + col * 30, 502 - h + row * 33, 16, 21, 2, 2);
            }
        }
        g.setColor(night ? color(68, 78, 95) : color(181, 192, 196)); g.fillRect(0, 485, 1280, 166);
        g.setColor(night ? color(35, 44, 59) : color(76, 95, 107)); g.fillRect(0, 651, 1280, 69);
        g.setColor(night ? color(151, 139, 101) : color(242, 226, 169));
        for (int x = 40; x < 1280; x += 160) g.fillRoundRect(x, 686, 80, 5, 2, 2);
        g.setColor(night ? color(83, 93, 108) : color(147, 165, 174));
        for (int x = 0; x < 1280; x += 160) line(g, x, 502, x - 25, 644, 1);
        for (int y = 520; y < 650; y += 42) line(g, 0, y, 1280, y, 1);
        for (int x : new int[]{145, 1140}) {
            g.setColor(color(45, 62, 70)); g.fillRoundRect(x, 301, 9, 246, 5, 5); line(g, x + 4, 303, x + 49, 303, 7);
            if (night) { g.setColor(new Color(255, 225, 148, 22)); g.fillOval(x - 5, 283, 125, 175); }
            g.setColor(night ? color(255, 225, 154) : color(241, 236, 210)); g.fillRoundRect(x + 27, 295, 48, 17, 9, 9);
        }
        tree(g, 1030, 531, .6, night);
    }

    private void beach(Graphics2D g, double t, boolean night) {
        g.setColor(night ? color(37, 77, 109) : color(62, 159, 180)); g.fillRect(0, 345, 1280, 210);
        g.setColor(night ? color(60, 113, 134) : color(111, 200, 198)); g.fillRect(0, 420, 1280, 135);
        for (int row = 0; row < 5; row++) {
            g.setColor(night ? new Color(175, 214, 218, 70) : new Color(235, 250, 238, 130));
            var wave = new Path2D.Double();
            for (int x = -10; x <= 1290; x += 8) {
                double y = 378 + row * 32 + Math.sin(x * .012 + t * 1.8 + row) * (3 + row);
                if (x == -10) wave.moveTo(x, y); else wave.lineTo(x, y);
            }
            g.setStroke(new BasicStroke(2 + row * .6f)); g.draw(wave);
        }
        g.setColor(night ? color(164, 152, 124) : color(245, 222, 170));
        var shore = new Path2D.Double(); shore.moveTo(0, 510); shore.curveTo(350, 490, 860, 547, 1280, 498); shore.lineTo(1280, 720); shore.lineTo(0, 720); shore.closePath(); g.fill(shore);
        g.setColor(night ? color(189, 182, 157) : color(252, 239, 206)); g.setStroke(new BasicStroke(9));
        var foam = new Path2D.Double(); foam.moveTo(0, 510); foam.curveTo(350, 490, 860, 547, 1280, 498); g.draw(foam);
        palm(g, 145, 572, night); palm(g, 1125, 550, night);
        g.setColor(night ? color(201, 157, 139) : color(235, 151, 126));
        for (int i = 0; i < 9; i++) g.fillArc(220 + (i * 131 % 920), 639 + i % 3 * 18, 13, 9, 0, 180);
    }

    private void palm(Graphics2D g, int x, int y, boolean night) {
        g.setColor(night ? color(115, 94, 78) : color(175, 131, 88));
        g.setStroke(new BasicStroke(19, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        var trunk = new QuadCurve2D.Double(x, y, x - 30, y - 140, x + 20, y - 235); g.draw(trunk);
        g.setColor(night ? color(36, 101, 83) : color(51, 142, 97));
        for (int i = 0; i < 7; i++) {
            double angle = -Math.PI + i * Math.PI / 6;
            var leaf = new QuadCurve2D.Double(x + 20, y - 235, x + 20 + Math.cos(angle) * 75, y - 275 + Math.sin(angle) * 50, x + 20 + Math.cos(angle) * 130, y - 235 + Math.sin(angle) * 70 + 42);
            g.setStroke(new BasicStroke(13, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(leaf);
        }
    }

    private void character(Graphics2D g, double x, double y, double phase, Action action) {
        var transform = g.getTransform(); g.translate(x, y);
        if (action == Action.DANCING) g.rotate(Math.sin(phase / 2) * .09, 0, -50);
        double stride = switch (action) { case WALKING -> 30; case RUNNING -> 55; case DANCING -> 27; default -> 0; };
        for (int side : new int[]{-1, 1}) {
            double swing = Math.sin(phase + (side == 1 ? Math.PI : 0));
            double footX = side * 18 + stride * swing;
            double footY = action == Action.JUMPING ? -8 : -Math.max(0, -swing) * (action == Action.RUNNING ? 35 : stride * .4);
            g.setColor(side == -1 ? color(36, 52, 80) : color(46, 67, 98));
            double kneeX = side * 19 + stride * swing * .5 + (action == Action.JUMPING ? side * 15 : 0);
            line(g, side * 15, -73, kneeX, -38, 23); line(g, kneeX, -38, footX, footY - 8, 19);
            g.setColor(color(250, 247, 231)); line(g, footX - 5, footY - 3, footX + 14, footY - 3, 13);
            g.setColor(color(231, 115, 97)); line(g, footX - 5, footY - 9, footX + 13, footY - 9, 12);
        }
        // Rounded jacket silhouette, with a lighter inset and zipper.
        g.setColor(color(30, 117, 127));
        var body = new Path2D.Double(); body.moveTo(-28, -163); body.quadTo(-43, -140, -35, -73); body.quadTo(0, -62, 35, -73); body.quadTo(43, -140, 28, -163); body.closePath(); g.fill(body);
        g.setColor(color(43, 147, 151)); g.fillRoundRect(-25, -155, 48, 81, 22, 22);
        g.setColor(color(169, 221, 202)); line(g, 0, -149, 0, -83, 2);
        for (int side : new int[]{-1, 1}) {
            double swing = Math.sin(phase + (side == 1 ? Math.PI : 0));
            double elbowX = side * 45 - swing * stride * .6, elbowY = -112;
            double handX = side * 46 - swing * stride, handY = -83 - Math.max(0, swing) * stride * .35;
            if (action == Action.WAVING && side == 1) { elbowX = 63; elbowY = -173; handX = 78 + Math.sin(phase) * 17; handY = -224 + Math.cos(phase) * 4; }
            if (action == Action.JUMPING) { elbowX = side * 58; elbowY = -177; handX = side * 74; handY = -211; }
            if (action == Action.DANCING) { elbowX = side * 59; elbowY = -138 + 20 * swing; handX = side * 84; handY = -157 + 36 * swing; }
            g.setColor(color(30, 117, 127)); line(g, side * 28, -149, elbowX, elbowY, 19);
            line(g, elbowX, elbowY, handX, handY, 16);
            g.setColor(color(241, 183, 137)); g.fill(new Ellipse2D.Double(handX - 10, handY - 10, 20, 22));
        }
        g.setColor(color(226, 155, 116)); g.fillRoundRect(-14, -183, 28, 30, 8, 8);
        g.setColor(color(241, 185, 143)); g.fillOval(-48, -249, 96, 82);
        g.fillOval(-54, -218, 15, 22); g.fillOval(39, -218, 15, 22);
        g.setColor(color(49, 46, 53));
        var hair = new Path2D.Double(); hair.moveTo(-46, -210); hair.curveTo(-68, -273, 40, -284, 46, -228); hair.lineTo(40, -212); hair.lineTo(31, -238); hair.quadTo(-10, -218, -33, -238); hair.lineTo(-39, -210); hair.closePath(); g.fill(hair);
        g.setColor(color(76, 58, 55));
        boolean blink = ((phase / 5.4) % 4.5) > 4.32;
        if (blink) { line(g, -21, -207, -13, -207, 3); line(g, 13, -207, 21, -207, 3); }
        else { g.fillOval(-21, -211, 7, 10); g.fillOval(14, -211, 7, 10); }
        g.setColor(new Color(231, 134, 118, 160)); g.fillOval(-31, -199, 15, 8); g.fillOval(17, -199, 15, 8);
        g.setColor(color(125, 73, 67)); g.setStroke(new BasicStroke(2.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(-10, -200, 20, 15, 190, 160, Arc2D.OPEN));
        g.setTransform(transform);
    }

    private void rain(Graphics2D g, double t, boolean night) {
        g.setColor(new Color(214, 239, 249, night ? 110 : 150));
        for (int i = 0; i < 165; i++) {
            double x = ((i * 197.0 - t * 95) % 1420 + 1420) % 1420 - 70;
            double y = (i * 113.0 + t * (420 + i % 4 * 40)) % 790 - 40;
            line(g, x, y, x - 7, y + 20, 1.5);
        }
        g.setColor(new Color(209, 233, 238, 65));
        for (int i = 0; i < 14; i++) {
            double fraction = (t * 1.7 + i * .31) % 1;
            g.setStroke(new BasicStroke(1));
            g.draw(new Ellipse2D.Double(80 + (i * 171 % 1120) - fraction * 13, 590 + i % 5 * 17, fraction * 26, fraction * 8));
        }
    }
    private static void line(Graphics2D g, double x1, double y1, double x2, double y2, double width) {
        g.setStroke(new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }
    private static Color color(int r, int g, int b) { return new Color(r, g, b); }
}
