package com.javaanime.service;

import com.javaanime.model.*;
import com.javaanime.model.SceneSpec.*;
import com.javaanime.render.SceneRenderer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CancellationException;
import static org.junit.jupiter.api.Assertions.*;

class VideoExporterTest {
    @TempDir Path directory;
    private final SceneSpec scene = new SceneSpec(Action.WALKING, Setting.PARK, Lighting.DAY, Weather.RAIN);
    private Path encoder() throws IOException {
        Path path = ToolPaths.ffmpeg(); Assumptions.assumeTrue(Files.isRegularFile(path)); return path;
    }
    private String probe(Path video, String entries) throws IOException {
        Path probe = encoder().resolveSibling("ffprobe.exe");
        return ProcessRunner.run(List.of(probe.toString(), "-v", "error", "-show_entries", entries, "-of", "default=noprint_wrappers=1", video.toString()), Duration.ofSeconds(30), new Cancellation());
    }
    @Test void writesPlayable720pVideo() throws Exception {
        Path video = directory.resolve("animation with spaces.mp4");
        var clip = new AnimationClip("Walking", scene, 5, null, null);
        new VideoExporter(encoder(), new SceneRenderer()).export(clip, video, new Cancellation(), ignored -> {});
        String metadata = probe(video, "stream=codec_name,width,height,avg_frame_rate:format=duration");
        assertTrue(metadata.contains("codec_name=h264")); assertTrue(metadata.contains("width=1280")); assertTrue(metadata.contains("height=720")); assertTrue(metadata.contains("avg_frame_rate=24/1")); assertTrue(metadata.contains("duration=5.000000"));
        ProcessRunner.run(List.of(encoder().toString(), "-v", "error", "-i", video.toString(), "-f", "null", "-"), Duration.ofSeconds(30), new Cancellation());
    }
    @Test void includesFullAudioAndSilentTail() throws Exception {
        Path wav = directory.resolve("voice.wav");
        var format = new AudioFormat(22050, 16, 1, true, false);
        byte[] samples = new byte[22050 * 2 * 6];
        for (int i = 0; i < samples.length / 2; i++) { short value = (short) (Math.sin(i * 2 * Math.PI * 220 / 22050) * 2000); samples[2 * i] = (byte) value; samples[2 * i + 1] = (byte) (value >> 8); }
        try (var input = new AudioInputStream(new ByteArrayInputStream(samples), format, samples.length / 2)) { AudioSystem.write(input, AudioFileFormat.Type.WAVE, wav.toFile()); }
        assertEquals(6, SpeechService.duration(wav));
        Path video = directory.resolve("narrated.mp4");
        new VideoExporter(encoder(), new SceneRenderer()).export(new AnimationClip("Walking", scene, AnimationClip.durationFor(6), wav, null), video, new Cancellation(), ignored -> {});
        String metadata = probe(video, "stream=codec_name,duration:format=duration");
        assertTrue(metadata.contains("codec_name=aac")); assertTrue(metadata.contains("duration=6.500000"), metadata);
        ProcessRunner.run(List.of(encoder().toString(), "-v", "error", "-i", video.toString(), "-f", "null", "-"), Duration.ofSeconds(30), new Cancellation());
    }
    @Test void cancellationPreservesExistingOutputAndCleansTemporaryFile() throws Exception {
        Path video = directory.resolve("keep.mp4"); Files.writeString(video, "existing video"); var cancellation = new Cancellation();
        assertThrows(CancellationException.class, () -> new VideoExporter(encoder(), new SceneRenderer()).export(new AnimationClip("Walking", scene, 5, null, null), video, cancellation, n -> { if (n >= 8) cancellation.cancel(); }));
        assertEquals("existing video", Files.readString(video));
        try (var paths = Files.list(directory)) { assertEquals(List.of(video), paths.toList()); }
    }
    @Test void encoderFailurePreservesExistingOutput() throws Exception {
        Path video = directory.resolve("keep.mp4"); Files.writeString(video, "existing video");
        Path wrongEncoder = Path.of(System.getProperty("java.home"), "bin", "java.exe");
        assertThrows(IOException.class, () -> new VideoExporter(wrongEncoder, new SceneRenderer()).export(new AnimationClip("Walking", scene, 5, null, null), video, new Cancellation(), ignored -> {}));
        assertEquals("existing video", Files.readString(video));
        try (var paths = Files.list(directory)) { assertEquals(List.of(video), paths.toList()); }
    }
    @Test void rejectsMissingDestinationFolderAndDirectoryTarget() throws Exception {
        var exporter = new VideoExporter(encoder(), new SceneRenderer()); var clip = new AnimationClip("Walking", scene, 5, null, null);
        assertThrows(IOException.class, () -> exporter.export(clip, directory.resolve("missing/video.mp4"), new Cancellation(), ignored -> {}));
        assertThrows(IOException.class, () -> exporter.export(clip, directory, new Cancellation(), ignored -> {}));
    }
    @Test void preCancelledOperationDoesNotTouchFiles() throws Exception {
        var cancellation = new Cancellation(); cancellation.cancel();
        assertThrows(CancellationException.class, () -> new VideoExporter(encoder(), new SceneRenderer()).export(new AnimationClip("Walking", scene, 5, null, null), directory.resolve("video.mp4"), cancellation, ignored -> {}));
        try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
    }
}
