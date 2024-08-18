package dev.nokee.samples.externalbuilds.internal;

import org.gradle.api.Action;
import org.gradle.api.Transformer;

public final class PeekTransformer<T> implements Transformer<T, T> {
    private final Action<T> action;

    private PeekTransformer(Action<T> action) {
        this.action = action;
    }

    @Override
    public T transform(T t) {
        action.execute(t);
        return t;
    }

    public static <T> PeekTransformer<T> peek(Action<T> action) {
        return new PeekTransformer<>(action);
    }
}
