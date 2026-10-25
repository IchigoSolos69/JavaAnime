package com.javaanime.ui;

import com.javaanime.model.AnimationClip;
import javax.sound.sampled.*;
import java.io.IOException;

public final class PreviewPlayer implements AutoCloseable {
    private final PlaybackTimeline timeline;
    private Clip audio;
    public PreviewPlayer(AnimationClip clip) throws IOException {
        timeline = new PlaybackTimeline(clip.durationSeconds());
        if (clip.narration() != null) {
            try (var input = AudioSystem.getAudioInputStream(clip.narration().toFile())) {
                audio = AudioSystem.getClip(); audio.open(input);
            } catch (UnsupportedAudioFileException | LineUnavailableException | IllegalArgumentException e) {
                if (audio != null) audio.close();
                throw new IOException("The audio output device is unavailable. The generated narration can still be exported.", e);
            }
        }
    }
    public double position() {
        if (audio != null && audio.isRunning()) timeline.seek(audio.getMicrosecondPosition() / 1e6);
        return timeline.position();
    }
    public void play() {
        timeline.play();
        if (audio != null) {
            audio.setMicrosecondPosition(Math.min((long) (timeline.position() * 1e6), audio.getMicrosecondLength()));
            if (audio.getMicrosecondPosition() < audio.getMicrosecondLength()) audio.start();
        }
    }
    public void pause() { position(); timeline.pause(); if (audio != null) audio.stop(); }
    public void seek(double seconds) {
        boolean playing = isPlaying(); pause(); timeline.seek(seconds); if (playing && !timeline.isFinished()) play();
    }
    public void restart() { pause(); timeline.seek(0); play(); }
    public boolean isPlaying() { return timeline.isPlaying(); }
    public boolean isFinished() { return timeline.isFinished(); }
    @Override public void close() { pause(); if (audio != null) audio.close(); }
}
