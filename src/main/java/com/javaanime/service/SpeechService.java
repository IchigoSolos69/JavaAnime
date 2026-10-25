package com.javaanime.service;

import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

public final class SpeechService {
    public record Voice(String name, String culture) {
        @Override public String toString() { return name + " (" + culture + ")"; }
    }

    public List<Voice> voices(Cancellation cancellation) throws IOException {
        Path directory = Files.createTempDirectory("javaanime-voices-");
        try {
            Path helper = extractHelper(directory);
            String result = ProcessRunner.run(command(helper, "voices"), Duration.ofSeconds(30), cancellation);
            List<Voice> voices = new ArrayList<>();
            for (String line : result.split("\\R")) {
                if (!line.startsWith("VOICE\t")) continue;
                String[] fields = line.split("\t");
                if (fields.length == 3 && fields[2].startsWith("en")) {
                    voices.add(new Voice(new String(Base64.getDecoder().decode(fields[1]), StandardCharsets.UTF_8), fields[2]));
                }
            }
            return List.copyOf(voices);
        } finally { deleteDirectory(directory); }
    }

    public Path synthesize(String text, Voice voice, Path directory, Cancellation cancellation) throws IOException {
        if (voice == null) throw new IOException("Choose an installed English Windows voice, or turn narration off.");
        Path helper = extractHelper(directory);
        Path input = directory.resolve("prompt.txt");
        Path config = directory.resolve("voice.txt");
        Path output = directory.resolve("narration.wav");
        Files.writeString(input, text, StandardCharsets.UTF_8);
        Files.writeString(config, voice.name(), StandardCharsets.UTF_8);
        var command = new ArrayList<>(command(helper, "speak"));
        command.addAll(List.of("-TextPath", input.toString(), "-VoicePath", config.toString(), "-OutputPath", output.toString()));
        ProcessRunner.run(command, Duration.ofMinutes(2), cancellation);
        cancellation.check();
        if (!Files.isRegularFile(output) || Files.size(output) <= 44) throw new IOException("Windows did not produce narration. Try another voice or turn narration off.");
        return output;
    }

    public static double duration(Path wav) throws IOException {
        try (var audio = AudioSystem.getAudioInputStream(wav.toFile())) {
            double seconds = audio.getFrameLength() / (double) audio.getFormat().getFrameRate();
            if (!Double.isFinite(seconds) || seconds <= 0) throw new IOException("Invalid narration audio.");
            return seconds;
        } catch (javax.sound.sampled.UnsupportedAudioFileException e) { throw new IOException("Windows produced an unsupported audio format.", e); }
    }

    private static List<String> command(Path helper, String operation) {
        return List.of(ToolPaths.powershell(), "-NoLogo", "-NoProfile", "-NonInteractive", "-STA", "-ExecutionPolicy", "Bypass", "-File", helper.toString(), "-Operation", operation);
    }
    private static Path extractHelper(Path directory) throws IOException {
        Path helper = directory.resolve("speech.ps1");
        try (var resource = SpeechService.class.getResourceAsStream("/speech.ps1")) {
            if (resource == null) throw new IOException("Speech helper is missing from the application.");
            Files.copy(resource, helper, StandardCopyOption.REPLACE_EXISTING);
        }
        return helper;
    }
    public static void deleteDirectory(Path directory) throws IOException {
        if (directory == null || !Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
