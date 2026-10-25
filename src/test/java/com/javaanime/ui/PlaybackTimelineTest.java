package com.javaanime.ui;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackTimelineTest {
    @Test void pauseResumeSeekAndRestartPreserveTime() {
        var clock = new AtomicLong(); var timeline = new PlaybackTimeline(5, clock::get);
        timeline.play(); clock.set(2_000_000_000L); assertEquals(2, timeline.position());
        timeline.pause(); clock.set(4_000_000_000L); assertEquals(2, timeline.position());
        timeline.play(); clock.set(5_000_000_000L); assertEquals(3, timeline.position());
        timeline.seek(1); assertEquals(1, timeline.position());
        clock.set(10_000_000_000L); assertEquals(5, timeline.position()); assertTrue(timeline.isFinished());
        timeline.play(); assertEquals(0, timeline.position());
    }
    @Test void seekClampsAtBothEnds() { var t = new PlaybackTimeline(5, () -> 0); t.seek(-5); assertEquals(0, t.position()); t.seek(20); assertEquals(5, t.position()); }
}
