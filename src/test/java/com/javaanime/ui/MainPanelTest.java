package com.javaanime.ui;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class MainPanelTest {
    @Test void createsSilentSceneAndEnablesPlaybackAndExport() throws Exception {
        var reference = new AtomicReference<MainPanel>();
        SwingUtilities.invokeAndWait(() -> {
            Theme.install(); var panel = new MainPanel(false); reference.set(panel);
            find(panel, JCheckBox.class).getFirst().setSelected(false);
            button(panel, "Generate animation").doClick();
        });
        try {
            awaitReady(reference.get());
            SwingUtilities.invokeAndWait(() -> {
                var panel = reference.get();
                assertTrue(button(panel, "Export MP4").isEnabled());
                assertTrue(button(panel, "Restart").isEnabled());
                var playback = find(panel, JButton.class).stream().filter(b -> b.getText().equals("Pause") || b.getText().equals("Play")).findFirst().orElseThrow();
                assertTrue(playback.isEnabled()); playback.doClick(); assertEquals("Play", playback.getText());
                button(panel, "Restart").doClick(); assertEquals("Pause", playback.getText());
            });
        } finally { SwingUtilities.invokeAndWait(reference.get()::shutdown); }
    }

    @Test void unsupportedActionWaitsForManualChoice() throws Exception {
        var reference = new AtomicReference<MainPanel>();
        SwingUtilities.invokeAndWait(() -> {
            Theme.install(); var panel = new MainPanel(false); reference.set(panel);
            find(panel, JCheckBox.class).getFirst().setSelected(false);
            find(panel, JTextArea.class).getFirst().setText("A person swims at the beach.");
            button(panel, "Generate animation").doClick();
            assertFalse(button(panel, "Export MP4").isEnabled());
            var actions = find(panel, JComboBox.class).stream().filter(combo -> combo.getItemCount() > 0 && "Choose an action...".equals(combo.getItemAt(0))).findFirst().orElseThrow();
            actions.setSelectedIndex(1); button(panel, "Generate animation").doClick();
        });
        try { awaitReady(reference.get()); }
        finally { SwingUtilities.invokeAndWait(reference.get()::shutdown); }
    }

    @Test void rejectsOversizedPasteAndKeepsGenerateVisibleAtSmallWindowSize() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Theme.install(); var panel = new MainPanel(false);
            try {
                JTextArea input = find(panel, JTextArea.class).getFirst(); input.setText("a".repeat(500));
                input.append("extra"); assertEquals(500, input.getText().length());
                panel.setSize(1080, 760); layout(panel);
                Point buttonPosition = SwingUtilities.convertPoint(button(panel, "Generate animation"), 0, 0, panel);
                assertTrue(buttonPosition.y >= 0 && buttonPosition.y + button(panel, "Generate animation").getHeight() < panel.getHeight());
                assertTrue(input.getHeight() >= 80);
            } finally { panel.shutdown(); }
        });
    }

    private static void awaitReady(MainPanel panel) throws Exception {
        long deadline = System.nanoTime() + 10_000_000_000L;
        var ready = new AtomicReference<>(false);
        while (!ready.get() && System.nanoTime() < deadline) {
            SwingUtilities.invokeAndWait(() -> ready.set(button(panel, "Export MP4").isEnabled()));
            if (!ready.get()) Thread.sleep(25);
        }
        assertTrue(ready.get(), "Generation did not enable export");
    }
    private static JButton button(Container parent, String text) { return find(parent, JButton.class).stream().filter(b -> b.getText().equals(text)).findFirst().orElseThrow(); }
    private static <T> List<T> find(Container parent, Class<T> type) {
        var result = new ArrayList<T>();
        for (Component component : parent.getComponents()) {
            if (type.isInstance(component)) result.add(type.cast(component));
            if (component instanceof Container child) result.addAll(find(child, type));
        }
        return result;
    }
    private static void layout(Container parent) { parent.doLayout(); for (Component component : parent.getComponents()) if (component instanceof Container child) layout(child); }
}
