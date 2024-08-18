package org.gradle.samples.plugins;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.TaskProvider;

import javax.inject.Inject;

/*private*/ abstract /*final*/ class SampleLifecycleBasePlugin implements Plugin<Project> {
    @Inject
    public SampleLifecycleBasePlugin() {}

    @Override
    public void apply(Project project) {
        TaskProvider<Task> generateSource = project.getTasks().register("generateSource");
        generateSource.configure(task -> {
//            task.dependsOn(manifestTask);
            task.setGroup("source generation");
            task.setDescription("generate the source files for all samples");
        });

        TaskProvider<Delete> cleanSample = project.getTasks().register("cleanSample", Delete.class);
    }
}
