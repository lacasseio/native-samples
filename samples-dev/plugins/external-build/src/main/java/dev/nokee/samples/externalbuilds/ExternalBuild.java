package dev.nokee.samples.externalbuilds;

import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderConvertible;

public interface ExternalBuild {
    Provider<Directory> getProjectDirectory();

    TaskReference task(String path);

    // Can be used anywhere that accept org.gradle.api.tasks.TaskReference
    interface TaskReference {}

    <T> ModelReference<T> model(Class<T> model);

    // Can be depended on
    interface ModelReference<T> extends ProviderConvertible<T> {}
}
