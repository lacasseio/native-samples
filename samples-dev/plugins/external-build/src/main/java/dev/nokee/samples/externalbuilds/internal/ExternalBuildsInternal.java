package dev.nokee.samples.externalbuilds.internal;

import dev.nokee.samples.externalbuilds.ExternalBuild;

public interface ExternalBuildsInternal extends Iterable<ExternalBuild> {
    boolean addAll(Iterable<ExternalBuildSpec> elements);
}
