package dev.nokee.samples.externalbuilds;

import org.gradle.api.file.Directory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

public interface ConfigurableExternalBuildSpec {
    Property<String> getName();
    Provider<Directory> getProjectDirectory();
}
