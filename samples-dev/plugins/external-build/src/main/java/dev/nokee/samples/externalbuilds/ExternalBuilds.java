package dev.nokee.samples.externalbuilds;

public interface ExternalBuilds {
    ExternalBuild getByName(String name);
    default ExternalBuild named(String name) {
        return getByName(name);
    }
    ExternalBuild externalBuildAt(Object rootProject);
}
