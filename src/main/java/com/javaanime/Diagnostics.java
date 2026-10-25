package com.javaanime;

import com.javaanime.model.*;
import com.javaanime.model.SceneSpec.*;
import com.javaanime.model.SceneSpec.Action;
import com.javaanime.render.SceneRenderer;
import com.javaanime.service.*;
import com.javaanime.ui.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.ByteArrayOutputStream;

/** Explicit development checks; the normal application always opens the studio. */
public final class Diagnostics {
    private Diagnostics() {}
    public static void run(String[] args) throws Exception {
        if (args.length != 2 || !java.util.Set.of("--render-check", "--smoke-test", "--speech-check").contains(args[0])) {
            throw new IllegalArgumentException("Usage: --render-check <folder>, --smoke-test <folder>, or --speech-check <folder>");
        }
        Path output = Path.of(args[1]).toAbsolutePath(); Files.createDirectories(output);
        var renderer = new SceneRenderer();
        if (args[0].equals("--render-check")) {
            var png = new ByteArrayOutputStream(); ImageIO.write(AppIcon.image(), "png", png);
            byte[] bytes = png.toByteArray(); var icon = ByteBuffer.allocate(22 + bytes.length).order(ByteOrder.LITTLE_ENDIAN);
            icon.putShort((short) 0).putShort((short) 1).putShort((short) 1);
            icon.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0).putShort((short) 1).putShort((short) 32).putInt(bytes.length).putInt(22).put(bytes);
            Files.write(output.resolve("JavaAnime.ico"), icon.array());
            var sheet = new BufferedImage(1280, 3 * 384, BufferedImage.TYPE_INT_RGB);
            var g = sheet.createGraphics(); g.setColor(Theme.BACKGROUND); g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
            int i = 0;
            for (Action action : Action.values()) {
                var scene = new SceneSpec(action, Setting.values()[i % 3], i % 2 == 0 ? Lighting.DAY : Lighting.NIGHT, i == 0 ? Weather.RAIN : Weather.CLEAR);
                int x = i % 2 * 640, y = i / 2 * 384;
                g.drawImage(renderer.frame(scene, .63, 640, 360), x, y, null);
                g.setColor(Theme.TEXT); g.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                g.drawString(action + " / " + scene.setting() + " / " + scene.lighting(), x + 14, y + 378); i++;
            }
            g.dispose(); ImageIO.write(sheet, "png", output.resolve("scene-check.png").toFile());
            SwingUtilities.invokeAndWait(() -> {
                Theme.install(); var panel = new MainPanel(false); panel.setSize(1250, 860); layout(panel);
                var image = new BufferedImage(1250, 860, BufferedImage.TYPE_INT_RGB); var graphics = image.createGraphics(); graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); panel.printAll(graphics); graphics.dispose();
                try { ImageIO.write(image, "png", output.resolve("studio-check.png").toFile()); } catch (Exception e) { throw new RuntimeException(e); }
                panel.shutdown();
            });
        } else {
            var cancellation = new Cancellation(); var speech = new SpeechService();
            var voices = args[0].equals("--speech-check") ? speech.voices(cancellation) : java.util.List.<SpeechService.Voice>of();
            if (args[0].equals("--speech-check") && voices.isEmpty()) throw new IllegalStateException("No installed English Windows speech voices.");
            if (!voices.isEmpty()) System.out.println("Available voices: " + voices);
            try (var clip = new ClipGenerator(speech).generate("A person walks through a park in the rain.",
                    new SceneSpec(Action.WALKING, Setting.PARK, Lighting.DAY, Weather.RAIN), !voices.isEmpty(), voices.isEmpty() ? null : voices.getFirst(), cancellation)) {
                if (clip.narration() != null) Files.copy(clip.narration(), output.resolve("narration.wav"), StandardCopyOption.REPLACE_EXISTING);
                new VideoExporter(ToolPaths.ffmpeg(), renderer).export(clip, output.resolve(voices.isEmpty() ? "sample-silent.mp4" : "sample-narrated.mp4"), cancellation, ignored -> {});
                System.out.println("Generated " + clip.frameCount() + " frames, " + clip.durationSeconds() + " seconds.");
            }
        }
        System.out.println("Checks saved to " + output);
    }
    private static void layout(Container parent) { parent.doLayout(); for (Component child : parent.getComponents()) if (child instanceof Container container) layout(container); }
}
