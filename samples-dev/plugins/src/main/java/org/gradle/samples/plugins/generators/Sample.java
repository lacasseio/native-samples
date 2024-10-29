package org.gradle.samples.plugins.generators;

import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.Task;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public abstract class Sample implements Named, ExtensionAware {
    private final String name;
    private final TaskProvider<? extends Task> manifestTask;
    private final TaskProvider<Sync> contentTask;
    private final List<Action<Zip>> zipActions = new ArrayList<Action<Zip>>();

    @Inject
    public Sample(String name, TaskProvider<? extends Task> manifestTask, TaskProvider<Sync> contentTask) {
        this.name = name;
        this.manifestTask = manifestTask;
        this.contentTask = contentTask;
    }

    @Override
    public String getName() {
        return name;
    }

    public abstract DirectoryProperty getSampleDir();

    public abstract Property<String> getTitle();

    public TaskProvider<? extends Task> getManifestTask() {
        return manifestTask;
    }

    public void content(Action<? super CopySpec> action) {
        contentTask.configure(action);
    }

    public TaskProvider<Sync> getContentTask() {
        return contentTask;
    }

    public void zipSource(Action<Zip> action) {
        zipActions.add(action);
    }

    public List<Action<Zip>> getZipActions() {
        return zipActions;
    }
}
