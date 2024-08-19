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

// Ensure one delegation per project is done
interface LimitConcurrentBuildTask extends Task {
    @Internal
    Property<ExternalBuildConcurrencyService> getBuildConcurrency();

    /*private*/ abstract /*final*/ class Rule implements Plugin<Project> {
        @Inject
        public Rule() {}

        @Override
        public void apply(Project project) {
            project.getTasks().withType(LimitConcurrentBuildTask.class).configureEach(task -> {
                String[] tokens = task.getName().split("__");
                if (tokens.length != 2) throw new RuntimeException("assumption violated");

                final Provider<ExternalBuildConcurrencyService> maxGradleBuild = project.getGradle().getSharedServices().registerIfAbsent(tokens[0], ExternalBuildConcurrencyService.class, spec -> {
                    spec.getMaxParallelUsages().set(1);
                });
                task.getBuildConcurrency().set(maxGradleBuild);
                task.usesService(maxGradleBuild);
            });
        }
    }

    /*private*/ abstract /*final*/ class ExternalBuildConcurrencyService implements BuildService<BuildServiceParameters.None> {}
}