package dev.nokee.samples.externalbuilds;

import org.gradle.api.Task;
import org.gradle.api.specs.Spec;

final class NeverUpToDateTaskSpec implements Spec<Task> {
    private NeverUpToDateTaskSpec() {}

    @Override
    public boolean isSatisfiedBy(Task element) {
        return false;
    }

    public static NeverUpToDateTaskSpec never_alwaysExecute() {
        return new NeverUpToDateTaskSpec();
    }
}
