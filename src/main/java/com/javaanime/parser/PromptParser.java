package com.javaanime.parser;

import com.javaanime.model.SceneSpec;
import com.javaanime.model.SceneSpec.*;
import java.util.*;
import java.util.regex.Pattern;

public final class PromptParser {
    public static final int MAX_LENGTH = 500;
    private static final Map<Action, Pattern> ACTIONS = Map.of(
        Action.WALKING, words("walk|walks|walking|walked|stroll|strolls|strolling"),
        Action.RUNNING, words("run|runs|running|ran|jog|jogs|jogging"),
        Action.JUMPING, words("jump|jumps|jumping|jumped|hop|hops|hopping"),
        Action.WAVING, words("wave|waves|waving|waved"),
        Action.DANCING, words("dance|dances|dancing|danced"));

    public Interpretation parse(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Enter a short scene description first.");
        if (text.length() > MAX_LENGTH) throw new IllegalArgumentException("Keep your scene under 500 characters.");
        String normalized = text.toLowerCase(Locale.ROOT);
        var actions = EnumSet.noneOf(Action.class);
        ACTIONS.forEach((action, pattern) -> {
            var matcher = pattern.matcher(normalized);
            while (matcher.find()) {
                // Don't mistake scenery ("ocean waves") or a negated action for character movement.
                if (action == Action.WAVING && matcher.group().equals("waves")
                        && words("ocean|sea|water|beach").matcher(normalized).find()
                        && !words("person|man|woman|boy|girl|character|he|she").matcher(normalized.substring(0, matcher.start())).find()) continue;
                String prefix = normalized.substring(Math.max(0, matcher.start() - 24), matcher.start());
                if (prefix.matches(".*\\b(?:not|never|without|don't|doesn't|do not|does not)\\s+(?:\\w+\\s+)?$")) continue;
                actions.add(action);
            }
        });
        Setting setting = words("beach|ocean|seaside|shore").matcher(normalized).find() ? Setting.BEACH
                : words("city|street|town|urban").matcher(normalized).find() ? Setting.CITY : Setting.PARK;
        Lighting lighting = words("night|nighttime|moon|midnight|evening").matcher(normalized).find() ? Lighting.NIGHT : Lighting.DAY;
        Weather weather = words("rain|rains|raining|rainy|rainfall|storm").matcher(normalized).find()
                && !Pattern.compile("\\b(?:no|without)\\s+rain(?:fall)?\\b").matcher(normalized).find() ? Weather.RAIN : Weather.CLEAR;
        return new Interpretation(List.copyOf(actions), setting, lighting, weather);
    }

    private static Pattern words(String alternatives) { return Pattern.compile("\\b(?:" + alternatives + ")\\b"); }

    public record Interpretation(List<Action> actions, Setting setting, Lighting lighting, Weather weather) {
        public boolean needsActionSelection() { return actions.size() != 1; }
        public SceneSpec scene(Action selected) { return new SceneSpec(selected, setting, lighting, weather); }
        public String message() {
            return actions.isEmpty() ? "Choose an action below. This prompt has no supported action."
                    : actions.size() > 1 ? "Choose one action below. This version animates one action per scene."
                    : "Scene understood. Adjust the controls if you like, then generate.";
        }
    }
}
