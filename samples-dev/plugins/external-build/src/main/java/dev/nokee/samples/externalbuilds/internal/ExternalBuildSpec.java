package dev.nokee.samples.externalbuilds.internal;

import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;

public interface ExternalBuildSpec {
    Provider<String> getName();
    Provider<Directory> getProjectDirectory();
}
