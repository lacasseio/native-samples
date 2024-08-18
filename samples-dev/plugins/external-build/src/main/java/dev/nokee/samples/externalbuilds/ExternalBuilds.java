package dev.nokee.samples.externalbuilds;

public interface ExternalBuilds {
    ExternalBuild getByName(String name);
    ExternalBuild externalBuildAt(Object rootProject);
}
