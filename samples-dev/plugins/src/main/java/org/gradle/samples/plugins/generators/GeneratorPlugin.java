package org.gradle.samples.plugins.generators;

import dev.nokee.samples.externalbuilds.ExternalBuilds;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.samples.plugins.SampleGeneratorTask;
import org.gradle.samples.plugins.Samples;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.gradle.samples.plugins.util.TransformEachTransformer.transformEach;

public class GeneratorPlugin implements Plugin<Project> {
    public void apply(Project project) {
        TaskCollection<SampleGeneratorTask> generatorTasks = project.getTasks().withType(SampleGeneratorTask.class);
        TaskCollection<GitRepoTask> repoTasks = project.getTasks().withType(GitRepoTask.class);

        // Add project extension
        SamplesExtension extension = project.getExtensions().create("samples", SamplesExtension.class, project);

        // Add a task to generate the list of samples
        TaskProvider<SamplesManifestTask> manifestTaskLegacy = project.getTasks().register("samplesManifestLegacy", SamplesManifestTask.class, task -> {
            task.getManifest().fileProvider(project.provider(() -> task.getTemporaryDirFactory().create()).map(it -> new File(it, "samples-list.txt")));
            task.getSampleDirs().addAll(project.provider(() -> {
                return generatorTasks.stream().map(generator -> {
                    return generator.getSampleDir().get().getAsFile().getAbsolutePath();
                }).collect(Collectors.toList());
            }));
            task.getSampleDirs().addAll(project.provider(() -> project.getRootProject().getExtensions().findByType(Samples.class)).flatMap(Samples::toProvider).map(transformEach(it -> it.getLocation().toString())));
            task.getRepoDirs().set(project.provider(() -> {
                return repoTasks.stream().map(generator -> {
                    return generator.getSampleDir().get().getAsFile().getAbsolutePath();
                }).collect(Collectors.toList());
            }));
        });

        TaskProvider<SamplesManifestTaskEx> manifestTask = project.getTasks().register("samplesManifest", SamplesManifestTaskEx.class, task -> {
            Provider<List<org.gradle.samples.plugins.Sample>> samples = project.provider(() -> project.getRootProject().getExtensions().findByType(Samples.class)).flatMap(Samples::toProvider).orElse(Collections.emptyList());
            task.dependsOn((Callable<?>) () -> {
                return samples.map(transformEach(it -> {
                    return project.getExtensions().getByType(ExternalBuilds.class).getByName(it.getName()).task(":samplesManifest");
                })).get();
            });
            task.getManifest().set(project.file("samples-list.txt"));
            task.getManifestFiles().from(manifestTaskLegacy.map(it -> it.getManifest().get()));
            task.getManifestFiles().from(samples.map(transformEach(it -> it.getLocation().resolve("build/samples-list.txt"))));
        });

        // Add a task to clean the samples
        project.getTasks().register("cleanSamples", CleanSamplesTask.class, task -> {
            // Need the location without the task dependency as we want to clean whatever was generated last time, not whatever will be generated next time
            task.getManifest().set(project.provider(() -> {
                return manifestTask.get().getManifest().get();
            }));
        });

        // Apply conventions to the generator tasks
        generatorTasks.configureEach( task -> {
            task.getTemplatesDir().set(project.file("src/templates"));
        });

        // Add a lifecycle task to generate the source files for the samples
        TaskProvider<Task> generateSource = project.getTasks().register("generateSource", task -> {
            task.dependsOn(generatorTasks);
            task.dependsOn(manifestTask);
            task.setGroup("source generation");
            task.setDescription("generate the source files for all samples");
        });

        extension.getExternalRepos().all(it -> {
            addTasksForRepo(it, generateSource, project);
        });

        // Add a lifecycle task to generate the repositories
        project.getTasks().register("generateRepos", task -> {
            task.dependsOn(repoTasks);
            task.setGroup("source generation");
            task.setDescription("generate the Git repositories for all samples");
        });
    }

    private void addTasksForRepo(ExternalRepo repo, TaskProvider<Task> generateSource, Project project) {
        TaskProvider<SyncExternalRepoTask> syncTask = project.getTasks().register("sync" + StringUtils.capitalize(repo.getName()), SyncExternalRepoTask.class, task -> {
            task.getRepoUrl().set(repo.getRepoUrl());
            task.getCheckoutDirectory().set(project.file("repos/" + repo.getName()));
        });
        TaskProvider<SourceCopyTask> setupTask = project.getTasks().register("copy" + StringUtils.capitalize(repo.getName()), SourceCopyTask.class, task -> {
            task.dependsOn(syncTask);
            task.getSampleDir().set(syncTask.get().getCheckoutDirectory());
            task.doFirst(task1 -> {
                repo.getSourceActions().forEach(it -> {
                    it.execute(task);
                });
            });
        });
        TaskProvider<UpdateRepoTask> updateTask = project.getTasks().register("update" + StringUtils.capitalize(repo.getName()), UpdateRepoTask.class, task -> {
            task.dependsOn(setupTask);
            task.getSampleDir().set(syncTask.get().getCheckoutDirectory());
            repo.getRepoActions().forEach(it -> {
                task.change(it);
            });
        });
        generateSource.configure(task -> {
            task.dependsOn(updateTask);
        });
    }
}
