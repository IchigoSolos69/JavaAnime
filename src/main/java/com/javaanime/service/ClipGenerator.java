package com.javaanime.service;

import com.javaanime.model.*;
import java.io.IOException;
import java.nio.file.*;

public final class ClipGenerator {
    private final SpeechService speech;
    public ClipGenerator(SpeechService speech) { this.speech = speech; }
    public AnimationClip generate(String prompt, SceneSpec scene, boolean narrated, SpeechService.Voice voice,
                                  Cancellation cancellation) throws IOException {
        cancellation.check();
        Path directory = Files.createTempDirectory("javaanime-scene-");
        try {
            Path audio = narrated ? speech.synthesize(prompt, voice, directory, cancellation) : null;
            double duration = AnimationClip.durationFor(audio == null ? 0 : SpeechService.duration(audio));
            cancellation.check();
            return new AnimationClip(prompt, scene, duration, audio, directory);
        } catch (IOException | RuntimeException error) {
            try { SpeechService.deleteDirectory(directory); } catch (IOException cleanup) { error.addSuppressed(cleanup); }
            throw error;
        }
    }
}
