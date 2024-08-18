package dev.nokee.samples.externalbuilds;

import org.gradle.api.Action;

public interface ExternalBuildManagement {
    void externalBuild(Object rootProject);
    void externalBuild(Object rootProject, Action<ConfigurableExternalBuildSpec> action);
}
