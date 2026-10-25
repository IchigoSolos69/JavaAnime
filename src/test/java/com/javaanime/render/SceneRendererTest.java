package com.javaanime.render;

import com.javaanime.model.SceneSpec;
import com.javaanime.model.SceneSpec.*;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class SceneRendererTest {
    private final SceneRenderer renderer = new SceneRenderer();
    @Test void allSceneCombinationsRenderAndMove() {
        for (Action action : Action.values()) for (Setting setting : Setting.values()) for (Lighting lighting : Lighting.values()) for (Weather weather : Weather.values()) {
            var scene = new SceneSpec(action, setting, lighting, weather);
            var first = renderer.frame(scene, 0, 320, 180); var later = renderer.frame(scene, .63, 320, 180);
            assertEquals(320, first.getWidth()); assertEquals(180, first.getHeight());
            assertFalse(Arrays.equals(pixels(first), pixels(later)), scene.toString());
        }
    }
    @Test void exactTimeRendersDeterministically() {
        var scene = new SceneSpec(Action.RUNNING, Setting.CITY, Lighting.NIGHT, Weather.RAIN);
        assertArrayEquals(pixels(renderer.frame(scene, 2.7, 640, 360)), pixels(renderer.frame(scene, 2.7, 640, 360)));
    }
    @Test void characterMovesEvenWithoutEnvironmentalEffects() {
        for (Action action : Action.values()) {
            var scene = new SceneSpec(action, Setting.PARK, Lighting.DAY, Weather.CLEAR);
            var a = renderer.frame(scene, 0, 640, 360).getSubimage(190, 170, 260, 140);
            var b = renderer.frame(scene, .63, 640, 360).getSubimage(190, 170, 260, 140);
            assertFalse(Arrays.equals(pixels(a), pixels(b)), action.toString());
        }
    }
    private static int[] pixels(BufferedImage image) { return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()); }
}
