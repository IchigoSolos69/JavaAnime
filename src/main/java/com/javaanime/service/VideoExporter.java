package com.javaanime.service;

import com.javaanime.model.AnimationClip;
import com.javaanime.render.SceneRenderer;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

public final class VideoExporter {
    private final Path encoder;
    private final SceneRenderer renderer;
    public VideoExporter(Path encoder, SceneRenderer renderer) { this.encoder = encoder; this.renderer = renderer; }

    public void export(AnimationClip clip, Path destination, Cancellation cancellation, IntConsumer progress) throws IOException {
        cancellation.check();
        destination = destination.toAbsolutePath();
        Path parent = destination.getParent();
        if (!Files.isDirectory(parent)) throw new IOException("The selected output folder does not exist.");
        if (Files.isDirectory(destination)) throw new IOException("Choose a video filename, not a folder.");
        if (clip.narration() != null && destination.equals(clip.narration().toAbsolutePath())) throw new IOException("Choose another output filename.");
        Path temporary = Files.createTempFile(parent, ".javaanime-", ".mp4");
        Process process = null;
        Thread monitor = null, reader = null;
        var output = new ByteArrayOutputStream();
        Throwable failure = null;
        try {
            List<String> command = new ArrayList<>(List.of(encoder.toString(), "-hide_banner", "-loglevel", "error", "-nostdin", "-y",
                    "-f", "rawvideo", "-pixel_format", "bgr24", "-video_size", AnimationClip.WIDTH + "x" + AnimationClip.HEIGHT,
                    "-framerate", "" + AnimationClip.FPS, "-i", "pipe:0"));
            if (clip.narration() != null) command.addAll(List.of("-i", clip.narration().toString()));
            command.addAll(List.of("-map", "0:v:0", "-c:v", "libx264", "-preset", "veryfast", "-crf", "20", "-pix_fmt", "yuv420p"));
            if (clip.narration() != null) command.addAll(List.of("-map", "1:a:0", "-c:a", "aac", "-b:a", "128k", "-af", "apad"));
            else command.add("-an");
            command.addAll(List.of("-t", String.format(Locale.ROOT, "%.6f", clip.frameCount() / (double) AnimationClip.FPS),
                    "-movflags", "+faststart", temporary.toString()));
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            Process running = process;
            reader = Thread.ofVirtual().start(() -> {
                try (var input = running.getInputStream()) {
                    byte[] buffer = new byte[4096];
                    for (int n; (n = input.read(buffer)) != -1;) synchronized (output) {
                        if (output.size() < 65536) output.write(buffer, 0, Math.min(n, 65536 - output.size()));
                    }
                } catch (IOException ignored) { /* The process may be cancelled. */ }
            });
            long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(10);
            monitor = Thread.ofVirtual().start(() -> {
                while (running.isAlive()) {
                    if (cancellation.isCancelled() || System.nanoTime() > deadline) { running.destroyForcibly(); break; }
                    try { Thread.sleep(100); } catch (InterruptedException e) { break; }
                }
            });
            try (var frames = new BufferedOutputStream(process.getOutputStream(), 1280 * 720 * 3)) {
                for (int frame = 0; frame < clip.frameCount(); frame++) {
                    cancellation.check();
                    var image = renderer.frame(clip.scene(), frame / (double) AnimationClip.FPS, AnimationClip.WIDTH, AnimationClip.HEIGHT);
                    frames.write(((DataBufferByte) image.getRaster().getDataBuffer()).getData());
                    progress.accept((int) ((frame + 1L) * 95 / clip.frameCount()));
                }
            }
            while (!process.waitFor(100, TimeUnit.MILLISECONDS)) cancellation.check();
            cancellation.check();
            reader.join(2000);
            if (process.exitValue() != 0) throw new IOException("Video export failed. " + output.toString(StandardCharsets.UTF_8).strip());
            if (Files.size(temporary) < 100) throw new IOException("The encoder did not produce a video.");
            cancellation.check();
            try { Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING); }
            progress.accept(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); cancellation.cancel();
            var cancelled = new java.util.concurrent.CancellationException("Export cancelled");
            cancelled.initCause(e); failure = cancelled; throw cancelled;
        } catch (IOException | RuntimeException e) {
            if (cancellation.isCancelled() && !(e instanceof java.util.concurrent.CancellationException)) {
                var cancelled = new java.util.concurrent.CancellationException("Export cancelled");
                cancelled.initCause(e); failure = cancelled; throw cancelled;
            }
            failure = e;
            synchronized (output) {
                String details = output.toString(StandardCharsets.UTF_8).strip();
                if (e instanceof IOException && !details.isBlank() && (e.getMessage() == null || !e.getMessage().contains(details))) {
                    var detailed = new IOException("Video export failed. " + details, e); failure = detailed; throw detailed;
                }
            }
            throw e;
        } finally {
            if (monitor != null) monitor.interrupt();
            if (process != null) {
                if (process.isAlive()) process.destroyForcibly();
                try { process.waitFor(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            if (reader != null) try { reader.join(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            try { deleteTemporary(temporary); }
            catch (IOException cleanup) { if (failure != null) failure.addSuppressed(cleanup); else throw cleanup; }
        }
    }

    private static void deleteTemporary(Path path) throws IOException {
        // Windows can briefly retain a handle after process termination or a virus scan.
        IOException last = null;
        boolean interrupted = Thread.interrupted();
        try {
            for (int attempt = 0; attempt < 20; attempt++) {
                try { Files.deleteIfExists(path); return; } catch (IOException e) { last = e; }
                try { Thread.sleep(50); } catch (InterruptedException e) { interrupted = true; }
            }
            throw last;
        } finally { if (interrupted) Thread.currentThread().interrupt(); }
    }
}
