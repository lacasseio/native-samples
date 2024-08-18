package org.gradle.samples.plugins;

import dev.nokee.samples.externalbuilds.ExternalBuildManagement;
import dev.nokee.samples.externalbuilds.ExternalBuilds;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.samples.plugins.generators.CleanSamplesTask;
import org.gradle.samples.plugins.generators.SamplesManifestTask;
import org.gradle.samples.plugins.generators.SamplesManifestTaskEx;
import org.gradle.samples.plugins.tasks.NewSampleTask;

import javax.inject.Inject;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.gradle.samples.plugins.util.TransformEachTransformer.transformEach;

/*private*/ abstract /*final*/ class SamplesPlugin implements Plugin<Settings> {
    @Inject
    public SamplesPlugin() {}

    @Override
    public void apply(Settings settings) {
        settings.getPluginManager().apply("dev.nokee.samples.external-build-management");

        settings.getPluginManager().apply("samplesdev.rules.samples-extension");

        // All samples are assumed to be external builds (and have to be because of Gradle limitations)
        settings.getGradle().settingsEvaluated(__ -> {
            ExternalBuildManagement externalBuildManagement = settings.getExtensions().getByType(ExternalBuildManagement.class);
            for (Sample sample : settings.getExtensions().getByType(SamplesExtensionInternal.class)) {
                externalBuildManagement.externalBuild(sample.getLocation(), it -> {
                    it.getName().set(sample.getName());
                });
            }
        });

        settings.getGradle().rootProject(project -> {
            project.getPluginManager().apply("samplesdev.rules.sample-lifecycle-base");

            // Add a task to generate the list of samples
            TaskProvider<SamplesManifestTask> manifestTaskLegacy = project.getTasks().register("samplesManifestLegacy", SamplesManifestTask.class, task -> {
                task.getManifest().fileProvider(project.provider(() -> task.getTemporaryDirFactory().create()).map(it -> new File(it, "samples-list.txt")));
                task.getSampleDirs().addAll(project.provider(() -> project.getRootProject().getExtensions().findByType(Samples.class)).flatMap(Samples::toProvider).map(transformEach(it -> it.getLocation().toString())));
            });
            TaskProvider<SamplesManifestTaskEx> manifestTask = project.getTasks().register("samplesManifest", SamplesManifestTaskEx.class, task -> {
                // TODO: Convert to model
                Provider<List<Sample>> samples = project.provider(() -> project.getRootProject().getExtensions().findByType(Samples.class)).flatMap(Samples::toProvider).orElse(Collections.emptyList());
                task.dependsOn((Callable<?>) () -> {
                    return samples.map(transformEach(it -> {
                        return project.getExtensions().getByType(ExternalBuilds.class).getByName(it.getName()).task(":samplesManifest");
                    })).get();
                });
                task.getManifest().set(project.file("samples-list.txt"));
                task.getManifestFiles().from(manifestTaskLegacy.map(it -> it.getManifest().get()));
                task.getManifestFiles().from(samples.map(transformEach(it -> it.getLocation().resolve("build/samples-list.txt"))));
            });

            project.getTasks().named("generateSource", task -> {
                task.dependsOn(manifestTask);
                task.dependsOn((Callable<?>) () -> {
                    return project.provider(() -> project.getExtensions().findByType(Samples.class)).flatMap(Samples::toProvider).orElse(Collections.emptyList()).map(transformEach(it -> {
                        return project.getExtensions().getByType(ExternalBuilds.class).getByName(it.getName()).task(":generateSource");
                    })).get();
                });
            });

            project.getTasks().register("cleanSamples", task -> {
                task.dependsOn((Callable<?>) () -> {
                    return project.provider(() -> project.getExtensions().findByType(Samples.class)).flatMap(Samples::toProvider).orElse(Collections.emptyList()).map(transformEach(it -> {
                        return project.getExtensions().getByType(ExternalBuilds.class).getByName(it.getName()).task(":cleanSample");
                    })).get();
                });
            });

            project.getTasks().register("generateRepos", task -> {
                task.dependsOn((Callable<?>) () -> {
                    return project.provider(() -> project.getExtensions().findByType(Samples.class)).flatMap(Samples::toProvider).orElse(Collections.emptyList()).map(transformEach(it -> {
                        return project.getExtensions().getByType(ExternalBuilds.class).getByName(it.getName()).task(":generateRepos");
                    })).get();
                });
            });

            project.getTasks().register("newSample", NewSampleTask.class, task -> {
                task.setDescription("Create a new sample in " + project + ".");
            });
        });
    }
}
