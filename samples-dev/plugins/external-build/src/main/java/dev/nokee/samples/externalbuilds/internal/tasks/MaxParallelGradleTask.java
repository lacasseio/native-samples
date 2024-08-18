package dev.nokee.samples.externalbuilds.internal.tasks;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.tasks.Internal;

import javax.inject.Inject;

// Limit parallel tasks
interface MaxParallelGradleTask extends Task {
    @Internal
    Property<MaxParallelGradleBuildService> getMaxParallelGradle();

    /*private*/ abstract /*final*/ class Rule implements Plugin<Project> {
        @Inject
        public Rule() {}

        @Override
        public void apply(Project project) {
            final Provider<MaxParallelGradleBuildService> maxGradleBuild = project.getGradle().getSharedServices().registerIfAbsent("cross", MaxParallelGradleBuildService.class, spec -> {
                spec.getMaxParallelUsages().set(project.getProviders().gradleProperty("max-gradle-build").map(Integer::parseInt).orElse(5));
            });

            project.getTasks().withType(MaxParallelGradleTask.class).configureEach(task -> {
                task.getMaxParallelGradle().set(maxGradleBuild);
                task.usesService(maxGradleBuild);
            });
        }
    }

    /*private*/ abstract /*final*/ class MaxParallelGradleBuildService implements BuildService<BuildServiceParameters.None> {}
}