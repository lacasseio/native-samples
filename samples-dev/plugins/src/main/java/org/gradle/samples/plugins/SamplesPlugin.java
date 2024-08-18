package org.gradle.samples.plugins;

import dev.nokee.samples.externalbuilds.ExternalBuildManagement;
import dev.nokee.samples.externalbuilds.ExternalBuilds;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.samples.plugins.tasks.NewSampleTask;

import javax.inject.Inject;
import java.util.Collections;
import java.util.concurrent.Callable;

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
                    it.getName().set(it.getName());
                });
            }
        });

        settings.getGradle().rootProject(project -> {
            project.getPluginManager().apply("samplesdev.rules.sample-lifecycle-base");

            project.getTasks().named("generateSource", task -> {
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

            project.getTasks().register("newSample", NewSampleTask.class, task -> {
                task.setDescription("Create a new sample in " + project + ".");
            });
        });
    }
}
