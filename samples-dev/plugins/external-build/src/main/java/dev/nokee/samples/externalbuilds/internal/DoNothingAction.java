package dev.nokee.samples.externalbuilds.internal;

import org.gradle.api.Action;

public final class DoNothingAction<T> implements Action<T> {
    @Override
    public void execute(T t) {
        // do nothing
    }

    public static <T> DoNothingAction<T> doNothing() {
        return new DoNothingAction<>();
    }
}
