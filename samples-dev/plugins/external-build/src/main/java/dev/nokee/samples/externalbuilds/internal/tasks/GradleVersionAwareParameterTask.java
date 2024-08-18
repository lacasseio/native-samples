package dev.nokee.samples.externalbuilds.internal.tasks;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;

import javax.inject.Inject;

import static dev.nokee.samples.externalbuilds.internal.tasks.ParameterizedTask.type;

interface GradleVersionAwareParameterTask<P extends GradleVersionAwareParameterTask.Parameters> extends ParameterizedTask<P> {
    interface Parameters extends ParameterizedTask.Parameters {
        @Internal
        Property<String> getGradleVersion();
    }

    /*private*/ abstract /*final*/ class Rule implements Plugin<Project> {
        @Inject
        public Rule() {}

        @Override
        public void apply(Project project) {
            project.getTasks().withType(type(GradleVersionAwareParameterTask.Parameters.class)).configureEach(task -> {
                task.parameters(it -> it.getGradleVersion().convention(project.getGradle().getGradleVersion()));
            });
        }
    }
}