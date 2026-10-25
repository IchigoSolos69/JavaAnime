package com.javaanime.model;

import java.util.Objects;

public record SceneSpec(Action action, Setting setting, Lighting lighting, Weather weather) {
    public SceneSpec {
        Objects.requireNonNull(action);
        Objects.requireNonNull(setting);
        Objects.requireNonNull(lighting);
        Objects.requireNonNull(weather);
    }

    public enum Action {
        WALKING("Walking"), RUNNING("Running"), JUMPING("Jumping"), WAVING("Waving"), DANCING("Dancing");
        private final String label;
        Action(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    public enum Setting {
        PARK("Park"), CITY("City"), BEACH("Beach");
        private final String label;
        Setting(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    public enum Lighting {
        DAY("Day"), NIGHT("Night");
        private final String label;
        Lighting(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    public enum Weather {
        CLEAR("Clear"), RAIN("Rain");
        private final String label;
        Weather(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
}
