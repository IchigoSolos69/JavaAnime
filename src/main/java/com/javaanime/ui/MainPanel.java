package com.javaanime.ui;

import com.javaanime.model.*;
import com.javaanime.model.SceneSpec.*;
import com.javaanime.model.SceneSpec.Action;
import com.javaanime.parser.PromptParser;
import com.javaanime.render.SceneRenderer;
import com.javaanime.service.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;

public final class MainPanel extends JPanel {
    private final JTextArea prompt = new JTextArea("A person walks through a park in the rain.");
    private final JLabel count = Theme.label("0 / 500", 12, Theme.MUTED, false);
    private final JComboBox<Object> action = new JComboBox<>();
    private final JComboBox<Setting> setting = new JComboBox<>(Setting.values());
    private final JComboBox<Lighting> lighting = new JComboBox<>(Lighting.values());
    private final JComboBox<Weather> weather = new JComboBox<>(Weather.values());
    private final JCheckBox narration = new JCheckBox("Read my text aloud", true);
    private final JComboBox<SpeechService.Voice> voices = new JComboBox<>();
    private final JComboBox<String> examples = new JComboBox<>(new String[]{"Choose an example...", "A person walks through a park in the rain.", "A girl dances on the beach at night.", "A man runs through a city street.", "A person waves in a sunny park.", "A boy jumps on the beach."});
    private final JLabel voiceHint = Theme.label("Checking Windows voices...", 11, Theme.MUTED, false);
    private final JLabel interpretation = Theme.label(" ", 11, Theme.MUTED, false);
    private final JLabel status = Theme.label("Ready to create", 12, Theme.MUTED, false);
    private final JLabel clipInfo = Theme.label("720p   /   24 fps   /   MP4", 12, Theme.MUTED, false);
    private final JLabel time = Theme.label("0:00 / 0:00", 12, Theme.MUTED, false);
    private final JButton generate = Theme.button("Generate animation", true);
    private final JButton export = Theme.button("Export MP4", true);
    private final JButton play = Theme.button("Play", false);
    private final JButton restart = Theme.button("Restart", false);
    private final JButton cancel = Theme.button("Cancel", false);
    private final JSlider scrubber = new JSlider(0, 1000, 0);
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final PreviewCanvas canvas = new PreviewCanvas();
    private final PromptParser parser = new PromptParser();
    private final SpeechService speech = new SpeechService();
    private final Timer timer = new Timer(1000 / AnimationClip.FPS, event -> tick());
    private final Timer analyzeTimer = new Timer(350, event -> analyze());
    private Cancellation activeCancellation;
    private Cancellation voiceCancellation;
    private AnimationClip clip;
    private PreviewPlayer player;
    private boolean busy, updatingSlider, scrubbing, resumeAfterScrub, closed;
    private String analyzedText = "";

    public MainPanel(boolean discoverVoices) {
        setLayout(new BorderLayout(0, 22)); setBackground(Theme.BACKGROUND); setBorder(Theme.padding(28));
        action.addItem("Choose an action..."); for (Action item : Action.values()) action.addItem(item);
        add(header(), BorderLayout.NORTH);
        var body = new JPanel(new BorderLayout(24, 0)); body.setOpaque(false);
        var left = controls(); left.setPreferredSize(new Dimension(365, left.getPreferredSize().height));
        var leftScroll = new JScrollPane(left); leftScroll.setBorder(null); leftScroll.getVerticalScrollBar().setUnitIncrement(20);
        leftScroll.setPreferredSize(new Dimension(382, 620)); leftScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        var leftColumn = new JPanel(new BorderLayout()); leftColumn.setBackground(Theme.CARD);
        leftColumn.setPreferredSize(new Dimension(382, 620)); leftColumn.add(leftScroll, BorderLayout.CENTER);
        var create = Theme.card(new BorderLayout(0, 8)); create.setBorder(BorderFactory.createEmptyBorder(12, 22, 20, 22));
        create.add(generate, BorderLayout.CENTER);
        create.add(Theme.label("One character, one action. Built-in cartoons.", 11, Theme.MUTED, false), BorderLayout.SOUTH);
        leftColumn.add(create, BorderLayout.SOUTH);
        body.add(leftColumn, BorderLayout.WEST); body.add(preview(), BorderLayout.CENTER); add(body, BorderLayout.CENTER);
        var footer = new JPanel(new BorderLayout(12, 0)); footer.setOpaque(false);
        footer.add(status, BorderLayout.CENTER);
        var tasks = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); tasks.setOpaque(false);
        progress.setPreferredSize(new Dimension(140, 8)); progress.setVisible(false); cancel.setVisible(false);
        tasks.add(progress); tasks.add(cancel); footer.add(tasks, BorderLayout.EAST); add(footer, BorderLayout.SOUTH);
        analyzeTimer.setRepeats(false);
        prompt.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
            private void changed() { count.setText(prompt.getText().length() + " / 500"); analyzeTimer.restart(); }
        });
        ((AbstractDocument) prompt.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void insertString(FilterBypass fb, int offset, String text, AttributeSet attributes) throws BadLocationException { replace(fb, offset, 0, text, attributes); }
            @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attributes) throws BadLocationException {
                text = text == null ? "" : text;
                if (fb.getDocument().getLength() - length + text.length() <= PromptParser.MAX_LENGTH) super.replace(fb, offset, length, text, attributes);
                else status.setText("Use up to 500 characters. Short descriptions work best.");
            }
        });
        count.setText(prompt.getText().length() + " / 500");
        generate.addActionListener(e -> generate()); export.addActionListener(e -> export());
        play.addActionListener(e -> { if (player == null) return; if (player.isPlaying()) player.pause(); else player.play(); tick(); });
        restart.addActionListener(e -> { if (player != null) { player.restart(); tick(); } });
        cancel.addActionListener(e -> { if (activeCancellation != null) { activeCancellation.cancel(); cancel.setEnabled(false); status.setText("Cancelling..."); } });
        narration.addActionListener(e -> voices.setEnabled(narration.isSelected() && !busy && voices.getItemCount() > 0));
        scrubber.addChangeListener(e -> scrub());
        setBusy(false); analyze(); timer.start();
        if (discoverVoices) discoverVoices(); else voiceHint.setText("Uses an installed English Windows voice");
    }

    private JPanel header() {
        var header = new JPanel(new BorderLayout()); header.setOpaque(false);
        var identity = new JPanel(); identity.setOpaque(false); identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.add(Theme.label("JavaAnime", 29, Theme.TEXT, true)); identity.add(Box.createVerticalStrut(4));
        identity.add(Theme.label("Turn a little story into a moving scene.", 14, Theme.MUTED, false)); header.add(identity, BorderLayout.CENTER);
        var badge = Theme.label("  OFFLINE STUDIO  ", 11, Theme.ACCENT, true); badge.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(45, 83, 74)), Theme.padding(9)));
        var right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 5)); right.setOpaque(false); right.add(badge); header.add(right, BorderLayout.EAST); return header;
    }

    private JPanel controls() {
        var panel = Theme.card(new GridBagLayout());
        var constraints = new GridBagConstraints(); constraints.gridx = 0; constraints.gridy = 0; constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL; constraints.anchor = GridBagConstraints.NORTHWEST;
        row(panel, constraints, Theme.label("01  DESCRIBE YOUR SCENE", 11, Theme.ACCENT, true), 12);
        prompt.setFont(new Font("Segoe UI", Font.PLAIN, 15)); prompt.setForeground(Theme.TEXT); prompt.setBackground(Theme.FIELD);
        prompt.setCaretColor(Theme.ACCENT); prompt.setLineWrap(true); prompt.setWrapStyleWord(true); prompt.setBorder(Theme.padding(13)); prompt.setRows(5);
        var scroll = new JScrollPane(prompt); scroll.setBorder(BorderFactory.createLineBorder(new Color(47, 66, 77))); scroll.setPreferredSize(new Dimension(300, 110)); scroll.setMinimumSize(new Dimension(300, 110));
        row(panel, constraints, scroll, 5); row(panel, constraints, count, 9);
        row(panel, constraints, Theme.label("Try a starting point", 12, Theme.MUTED, false), 7);
        examples.setToolTipText("Pick a supported example to fill the description.");
        examples.addActionListener(e -> { if (examples.getSelectedIndex() > 0) { prompt.setText((String) examples.getSelectedItem()); analyze(); } });
        examples.setPrototypeDisplayValue("A person walks in a park."); row(panel, constraints, examples, 19);
        row(panel, constraints, Theme.label("02  MAKE IT YOURS", 11, Theme.ACCENT, true), 10);
        row(panel, constraints, field("Action", action), 9); row(panel, constraints, field("Setting", setting), 9);
        var atmosphere = new JPanel(new GridLayout(1, 2, 10, 0)); atmosphere.setOpaque(false);
        atmosphere.add(field("Lighting", lighting)); atmosphere.add(field("Weather", weather)); row(panel, constraints, atmosphere, 7);
        interpretation.setPreferredSize(new Dimension(300, 35)); row(panel, constraints, interpretation, 14);
        narration.setFocusPainted(false); row(panel, constraints, narration, 5);
        voices.setPrototypeDisplayValue(new SpeechService.Voice("Microsoft English", "en-US")); row(panel, constraints, voices, 4);
        voiceHint.setPreferredSize(new Dimension(300, 29)); row(panel, constraints, voiceHint, 17);
        constraints.weighty = 1; panel.add(Box.createVerticalGlue(), constraints); return panel;
    }

    private JPanel preview() {
        var panel = Theme.card(new BorderLayout(0, 18));
        var title = new JPanel(new BorderLayout()); title.setOpaque(false);
        title.add(Theme.label("03  WATCH IT COME TO LIFE", 11, Theme.ACCENT, true), BorderLayout.WEST);
        title.add(Theme.label("16:9", 11, Theme.MUTED, false), BorderLayout.EAST); panel.add(title, BorderLayout.NORTH);
        panel.add(canvas, BorderLayout.CENTER);
        var bottom = new JPanel(); bottom.setOpaque(false); bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        var playback = new JPanel(new BorderLayout(12, 0)); playback.setOpaque(false);
        var buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); buttons.setOpaque(false); buttons.add(play); buttons.add(restart);
        playback.add(buttons, BorderLayout.WEST); playback.add(time, BorderLayout.EAST); bottom.add(playback); bottom.add(Box.createVerticalStrut(8));
        scrubber.setOpaque(false); bottom.add(scrubber); bottom.add(Box.createVerticalStrut(18));
        var output = new JPanel(new BorderLayout()); output.setOpaque(false); output.add(clipInfo, BorderLayout.WEST); output.add(export, BorderLayout.EAST);
        bottom.add(output); panel.add(bottom, BorderLayout.SOUTH); return panel;
    }

    private static JPanel field(String title, JComponent control) {
        var panel = new JPanel(new BorderLayout(0, 5)); panel.setOpaque(false);
        panel.add(Theme.label(title, 12, Theme.MUTED, false), BorderLayout.NORTH);
        control.setPreferredSize(new Dimension(140, 30)); panel.add(control, BorderLayout.CENTER); return panel;
    }
    private static void row(JPanel panel, GridBagConstraints constraints, Component component, int gap) {
        constraints.insets = new Insets(0, 0, gap, 0); panel.add(component, constraints); constraints.gridy++;
    }

    private void analyze() {
        analyzeTimer.stop(); String text = prompt.getText();
        if (text.equals(analyzedText)) return;
        analyzedText = text;
        if (text.isBlank()) { action.setSelectedIndex(0); interpretation.setText("Describe one character and one action."); return; }
        try {
            var result = parser.parse(text);
            action.setSelectedItem(result.needsActionSelection() ? "Choose an action..." : result.actions().getFirst());
            setting.setSelectedItem(result.setting()); lighting.setSelectedItem(result.lighting()); weather.setSelectedItem(result.weather());
            interpretation.setText("<html>" + result.message() + "</html>");
        } catch (IllegalArgumentException e) { interpretation.setText(e.getMessage()); }
    }

    private void discoverVoices() {
        voiceCancellation = new Cancellation();
        new SwingWorker<List<SpeechService.Voice>, Void>() {
            @Override protected List<SpeechService.Voice> doInBackground() throws Exception { return speech.voices(voiceCancellation); }
            @Override protected void done() {
                if (closed) return;
                try {
                    for (var voice : get()) voices.addItem(voice);
                    voiceHint.setText(voices.getItemCount() > 0 ? "<html>Windows voices work offline. Quality varies by voice.</html>" : "<html>No English voice found. Add one in Windows Settings, or turn narration off.</html>");
                } catch (Exception e) {
                    voiceHint.setText("<html>Windows speech is unavailable. Turn narration off to create a silent clip.</html>");
                    voiceHint.setToolTipText(message(e));
                }
                voices.setEnabled(narration.isSelected() && !busy && voices.getItemCount() > 0);
            }
        }.execute();
    }

    private void generate() {
        if (busy) return; analyze();
        try { parser.parse(prompt.getText()); } catch (IllegalArgumentException e) { error(e.getMessage()); return; }
        if (!(action.getSelectedItem() instanceof Action chosen)) { status.setText("Choose an action before generating this scene."); action.requestFocusInWindow(); return; }
        if (narration.isSelected() && voices.getSelectedItem() == null) { error("An English Windows voice is required for narration. Turn narration off, or install an English speech voice in Windows Settings and reopen the app."); return; }
        if (player != null) player.pause();
        var scene = new SceneSpec(chosen, (Setting) setting.getSelectedItem(), (Lighting) lighting.getSelectedItem(), (Weather) weather.getSelectedItem());
        String text = prompt.getText().strip(); boolean narrated = narration.isSelected(); var voice = (SpeechService.Voice) voices.getSelectedItem();
        var cancellation = new Cancellation(); activeCancellation = cancellation;
        setBusy(true); progress.setIndeterminate(true); status.setText(narrated ? "Creating your scene and narration..." : "Creating your scene...");
        new SwingWorker<AnimationClip, Void>() {
            @Override protected AnimationClip doInBackground() throws Exception { return new ClipGenerator(speech).generate(text, scene, narrated, voice, cancellation); }
            @Override protected void done() {
                try {
                    AnimationClip created = get();
                    if (closed || cancellation.isCancelled()) { created.close(); return; }
                    releaseClip(); clip = created;
                    try { player = new PreviewPlayer(clip); }
                    catch (IOException e) {
                        player = new PreviewPlayer(new AnimationClip(clip.prompt(), clip.scene(), clip.durationSeconds(), null, null));
                        JOptionPane.showMessageDialog(MainPanel.this, e.getMessage(), "Audio preview unavailable", JOptionPane.INFORMATION_MESSAGE);
                    }
                    canvas.showScene(clip.scene(), 0); player.play();
                    clipInfo.setText(String.format(java.util.Locale.ROOT, "720p   /   24 fps   /   %.1f seconds%s", clip.durationSeconds(), clip.narration() == null ? "" : "   /   Narrated"));
                    status.setText("Your scene is ready. Preview it, or export an MP4.");
                } catch (Exception e) {
                    if (!closed && (cancellation.isCancelled() || rootCause(e) instanceof CancellationException)) status.setText("Generation cancelled.");
                    else if (!closed) error(message(e));
                } finally { activeCancellation = null; if (!closed) { setBusy(false); tick(); } else releaseClip(); }
            }
        }.execute();
    }

    private void export() {
        if (clip == null || busy) return;
        Path encoder;
        try { encoder = ToolPaths.ffmpeg(); } catch (IOException e) { error(e.getMessage()); return; }
        var chooser = new JFileChooser(); chooser.setDialogTitle("Save your animation"); chooser.setFileFilter(new FileNameExtensionFilter("MP4 video", "mp4"));
        chooser.setSelectedFile(new java.io.File("JavaAnime.mp4"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path destination = chooser.getSelectedFile().toPath();
        if (!destination.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".mp4")) destination = destination.resolveSibling(destination.getFileName() + ".mp4");
        if (Files.exists(destination) && JOptionPane.showConfirmDialog(this, "Replace " + destination.getFileName() + "?", "Replace video", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        if (player != null) player.pause();
        final Path output = destination; final AnimationClip source = clip;
        var cancellation = new Cancellation(); activeCancellation = cancellation;
        setBusy(true); progress.setIndeterminate(false); progress.setValue(0); status.setText("Exporting your MP4...");
        new SwingWorker<Void, Integer>() {
            @Override protected Void doInBackground() throws Exception {
                new VideoExporter(encoder, new SceneRenderer()).export(source, output, cancellation, value -> { if (value % 2 == 0) publish(value); }); return null;
            }
            @Override protected void process(List<Integer> updates) { if (!closed) progress.setValue(updates.getLast()); }
            @Override protected void done() {
                try { get(); if (!closed) { status.setText("Saved " + output.getFileName()); JOptionPane.showMessageDialog(MainPanel.this, "Your animation is saved to:\n" + output, "MP4 exported", JOptionPane.INFORMATION_MESSAGE); } }
                catch (Exception e) { if (!closed) { if (cancellation.isCancelled() || rootCause(e) instanceof CancellationException) status.setText("Export cancelled. Your existing file was kept."); else error(message(e)); } }
                finally { activeCancellation = null; if (!closed) { setBusy(false); tick(); } else releaseClip(); }
            }
        }.execute();
    }

    private void scrub() {
        if (updatingSlider || player == null) return;
        if (scrubber.getValueIsAdjusting() && !scrubbing) { scrubbing = true; resumeAfterScrub = player.isPlaying(); player.pause(); }
        player.seek(scrubber.getValue() / 1000.0 * clip.durationSeconds());
        if (!scrubber.getValueIsAdjusting() && scrubbing) { scrubbing = false; if (resumeAfterScrub) player.play(); }
        tick();
    }
    private void tick() {
        if (player == null || clip == null) return;
        double position = player.position();
        if (player.isFinished() && player.isPlaying()) player.pause();
        canvas.showScene(clip.scene(), position);
        play.setText(player.isPlaying() ? "Pause" : "Play");
        time.setText(formatTime(position) + " / " + formatTime(clip.durationSeconds()));
        if (!scrubbing) { updatingSlider = true; scrubber.setValue((int) (position / clip.durationSeconds() * 1000)); updatingSlider = false; }
    }
    private static String formatTime(double seconds) { int s = (int) seconds; return (s / 60) + ":" + String.format("%02d", s % 60); }
    private void setBusy(boolean value) {
        busy = value; generate.setEnabled(!value); export.setEnabled(!value && clip != null);
        play.setEnabled(!value && player != null); restart.setEnabled(!value && player != null); scrubber.setEnabled(!value && player != null);
        prompt.setEditable(!value); examples.setEnabled(!value); action.setEnabled(!value); setting.setEnabled(!value); lighting.setEnabled(!value); weather.setEnabled(!value);
        narration.setEnabled(!value); voices.setEnabled(!value && narration.isSelected() && voices.getItemCount() > 0);
        cancel.setVisible(value); cancel.setEnabled(value); progress.setVisible(value); progress.setIndeterminate(false);
    }
    private void error(String text) { status.setText("Something needs attention."); JOptionPane.showMessageDialog(this, text, "JavaAnime", JOptionPane.WARNING_MESSAGE); }
    private static Throwable rootCause(Throwable e) { while (e.getCause() != null) e = e.getCause(); return e; }
    private static String message(Throwable e) { Throwable root = rootCause(e); String text = root.getMessage(); return text == null ? root.toString() : text; }
    private void releaseClip() {
        if (player != null) { player.close(); player = null; }
        if (clip != null) { try { clip.close(); } catch (IOException ignored) { /* Temporary files can be removed by Windows later. */ } clip = null; }
    }
    public void shutdown() {
        closed = true; timer.stop(); analyzeTimer.stop();
        if (voiceCancellation != null) voiceCancellation.cancel();
        if (activeCancellation != null) activeCancellation.cancel();
        else releaseClip();
        if (player != null) player.pause();
    }
}
