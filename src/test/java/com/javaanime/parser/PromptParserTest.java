package com.javaanime.parser;

import com.javaanime.model.SceneSpec.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class PromptParserTest {
    private final PromptParser parser = new PromptParser();
    @ParameterizedTest
    @CsvSource({"A person walks through a park in the rain.,WALKING,PARK,DAY,RAIN", "A girl dances on the beach at night.,DANCING,BEACH,NIGHT,CLEAR", "He ran down the city street.,RUNNING,CITY,DAY,CLEAR", "A boy is jumping.,JUMPING,PARK,DAY,CLEAR", "She waves.,WAVING,PARK,DAY,CLEAR", "A person is strolling.,WALKING,PARK,DAY,CLEAR"})
    void recognizesSupportedScenes(String prompt, Action action, Setting setting, Lighting lighting, Weather weather) {
        var result = parser.parse(prompt); assertEquals(java.util.List.of(action), result.actions());
        assertEquals(setting, result.setting()); assertEquals(lighting, result.lighting()); assertEquals(weather, result.weather());
    }
    @Test void ambiguousActionsRequireSelection() { assertTrue(parser.parse("A person walks and dances.").needsActionSelection()); }
    @Test void unsupportedActionRequiresSelection() { assertTrue(parser.parse("A person swims in the ocean.").needsActionSelection()); }
    @Test void tokensDoNotMatchInsideOtherWords() { assertTrue(parser.parse("A runway and a walkway.").actions().isEmpty()); }
    @Test void rejectsEmptyAndLongInput() { assertThrows(IllegalArgumentException.class, () -> parser.parse(" ")); assertThrows(IllegalArgumentException.class, () -> parser.parse("a".repeat(501))); }
    @Test void acceptsLimit() { assertDoesNotThrow(() -> parser.parse("walk " + "a".repeat(495))); }
    @Test void ignoresNegatedAction() { assertEquals(java.util.List.of(Action.RUNNING), parser.parse("A person does not walk, but runs.").actions()); }
    @Test void handlesNoRain() { assertEquals(Weather.CLEAR, parser.parse("A person walks without rain.").weather()); }
    @Test void oceanWavesAreNotCharacterActions() { assertTrue(parser.parse("Ocean waves on the beach.").actions().isEmpty()); }
    @Test void characterWavingAtBeachStillWorks() { assertEquals(java.util.List.of(Action.WAVING), parser.parse("A person waves on the beach.").actions()); }
}
