package com.javaanime.model;

import java.io.IOException;
import java.nio.file.*;

public record AnimationClip(String prompt, SceneSpec scene, double durationSeconds, Path narration,
                            Path workingDirectory) implements AutoCloseable {
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;
    public static final int FPS = 24;

    public AnimationClip {
        if (!Double.isFinite(durationSeconds) || durationSeconds < 5) throw new IllegalArgumentException("Invalid clip duration");
    }
    public int frameCount() { return (int) Math.ceil(durationSeconds * FPS); }
    public static double durationFor(double speechSeconds) {
        if (!Double.isFinite(speechSeconds) || speechSeconds < 0) throw new IllegalArgumentException("Invalid speech duration");
        return Math.ceil(Math.max(5, speechSeconds + 0.5) * FPS) / FPS;
    }
    @Override public void close() throws IOException {
        if (workingDirectory == null || !Files.exists(workingDirectory)) return;
        try (var paths = Files.walk(workingDirectory)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
