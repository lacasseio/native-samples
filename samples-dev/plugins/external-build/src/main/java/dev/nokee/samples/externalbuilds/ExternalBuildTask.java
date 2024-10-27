package dev.nokee.samples.externalbuilds;

import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;

public interface ExternalBuildTask extends Task {
    static Class<? extends ExternalBuildTask> implementationType() {
        return CrossProjectTask.class;
    }

    @Internal
    DirectoryProperty getProjectDirectory();

    @Internal
    ListProperty<String> getArguments();

    @Internal
    Property<String> getTaskPath();
}
