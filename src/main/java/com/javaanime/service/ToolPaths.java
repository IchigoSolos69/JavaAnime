package com.javaanime.service;

import java.io.IOException;
import java.nio.file.*;

public final class ToolPaths {
    private ToolPaths() {}
    public static Path ffmpeg() throws IOException {
        String override = System.getProperty("javaanime.ffmpeg");
        if (override != null && Files.isRegularFile(Path.of(override))) return Path.of(override).toAbsolutePath();
        String appDirectory = System.getProperty("javaanime.appDir");
        if (appDirectory != null) {
            Path bundled = Path.of(appDirectory, "tools", "ffmpeg.exe");
            if (Files.isRegularFile(bundled)) return bundled.toAbsolutePath();
        }
        for (String location : new String[]{"tools/ffmpeg.exe", ".tools/ffmpeg/bin/ffmpeg.exe"}) {
            Path candidate = Path.of(location).toAbsolutePath();
            if (Files.isRegularFile(candidate)) return candidate;
        }
        throw new IOException("The MP4 encoder is missing. Use the complete JavaAnime portable folder, or run scripts/build.ps1.");
    }
    public static String powershell() {
        String windows = System.getenv("SystemRoot");
        return windows == null ? "powershell.exe" : Path.of(windows, "System32", "WindowsPowerShell", "v1.0", "powershell.exe").toString();
    }
}
