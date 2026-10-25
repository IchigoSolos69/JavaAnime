package com.javaanime.ui;

import java.util.function.LongSupplier;

/** Monotonic time, independently testable without a screen or an audio device. */
public final class PlaybackTimeline {
    private final LongSupplier clock;
    private final double duration;
    private double offset;
    private long started;
    private boolean playing;
    public PlaybackTimeline(double duration) { this(duration, System::nanoTime); }
    PlaybackTimeline(double duration, LongSupplier clock) { this.duration = duration; this.clock = clock; }
    public double position() { return Math.min(duration, offset + (playing ? (clock.getAsLong() - started) / 1e9 : 0)); }
    public void play() { if (position() >= duration) { seek(0); playing = false; } if (playing) return; started = clock.getAsLong(); playing = true; }
    public void pause() { offset = position(); playing = false; }
    public void seek(double seconds) { offset = Math.max(0, Math.min(duration, seconds)); started = clock.getAsLong(); }
    public boolean isPlaying() { return playing; }
    public boolean isFinished() { return position() >= duration; }
}
