package org.gradle.samples.plugins;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.samples.plugins.generators.CleanSamplesTask;
import org.gradle.samples.plugins.generators.GitRepoTask;
import org.gradle.samples.plugins.generators.SamplesManifestTask;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/*private*/ abstract /*final*/ class SamplePlugin implements Plugin<Settings> {
    @Inject
    public SamplePlugin() {}

    @Override
    public void apply(Settings settings) {
        // IDEA
        settings.getPluginManager().apply("samplesdev.rules.idea-root-project-name-override");
        settings.getPluginManager().apply("dev.nokee.samples.external-build-models");

        settings.getPluginManager().apply("samplesdev.rules.settings-sample-extension");
        settings.getPluginManager().apply("samplesdev.rules.copy-source-extension");

        settings.getGradle().rootProject(project -> {
            project.getPluginManager().apply("samplesdev.rules.sample-lifecycle-base");

            TaskCollection<SampleGeneratorTask> generatorTasks = project.getTasks().withType(SampleGeneratorTask.class);
            TaskCollection<GitRepoTask> repoTasks = project.getTasks().withType(GitRepoTask.class);
            TaskProvider<SamplesManifestTask> manifestTask = project.getTasks().register("samplesManifest", SamplesManifestTask.class, task -> {
                task.getManifest().set(project.getLayout().getBuildDirectory().file("samples-list.txt"));
                task.getRepoDirs().set(project.provider(() -> {
                    return repoTasks.stream().map(generator -> {
                        return generator.getSampleDir().get().getAsFile().getAbsolutePath();
                    }).collect(Collectors.toList());
                }));
            });

            // Add a task to clean the samples
            project.getTasks().register("cleanSamples", CleanSamplesTask.class, task -> {
                // Need the location without the task dependency as we want to clean whatever was generated last time, not whatever will be generated next time
                task.getManifest().set(project.provider(() -> {
                    return manifestTask.get().getManifest().get();
                }));
            });
            project.getTasks().named("cleanSample", task -> task.dependsOn("cleanSamples"));

            // Add a lifecycle task to generate the repositories
            project.getTasks().register("generateRepos", task -> {
                task.dependsOn(repoTasks);
                task.setGroup("source generation");
                task.setDescription("generate the Git repositories for all samples");
            });

            project.getTasks().named("generateSource", task -> {
                task.dependsOn((Callable<?>) () -> {
                    return Optional.ofNullable(project.getTasks().findByName("copySource")).map(Object.class::cast).orElse(Collections.emptyList());
                });
            });
        });

        SampleExtension extension = settings.getExtensions().getByType(SampleExtension.class);
        extension.getName().convention(settings.getSettingsDir().getParentFile().getName() + "-" + settings.getSettingsDir().getName());
    }
}
