package dev.nokee.samples.externalbuilds.internal;

import org.gradle.api.Task;
import org.gradle.api.specs.Spec;

public final class NeverUpToDateTaskSpec implements Spec<Task> {
    private NeverUpToDateTaskSpec() {}

    @Override
    public boolean isSatisfiedBy(Task element) {
        return false;
    }

    public static NeverUpToDateTaskSpec never_alwaysExecute() {
        return new NeverUpToDateTaskSpec();
    }
}
