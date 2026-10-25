package com.javaanime.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnimationClipTest {
    @Test void minimumDurationIsFiveSeconds() { assertEquals(5, AnimationClip.durationFor(0)); assertEquals(5, AnimationClip.durationFor(2)); }
    @Test void narrationIsNeverTruncatedAndHasTail() { for (double speech : new double[]{4.9, 6.005, 35.9}) { double duration = AnimationClip.durationFor(speech); assertTrue(duration >= speech + .5); assertEquals(Math.rint(duration * 24), duration * 24, .00001); } }
    @Test void invalidAudioDurationRejected() { assertThrows(IllegalArgumentException.class, () -> AnimationClip.durationFor(Double.NaN)); assertThrows(IllegalArgumentException.class, () -> AnimationClip.durationFor(-1)); }
}
