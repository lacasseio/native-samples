package dev.nokee.samples.externalbuilds.internal.tasks;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Internal;

import javax.inject.Inject;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.nokee.samples.externalbuilds.internal.tasks.ParameterizedTask.type;

interface GradleToolingTask<P extends GradleToolingTask.Parameters> extends ParameterizedTask<P> {
    interface Parameters extends ParameterizedTask.Parameters {
        @Internal
        ListProperty<String> getArguments();
    }

    /*private*/ abstract /*final*/ class Rule implements Plugin<Project> {
        @Inject
        public Rule() {}

        @Override
        public void apply(Project project) {
            project.getTasks().withType(type(GradleToolingTask.Parameters.class)).configureEach(task -> {
                task.parameters(it -> it.getArguments().addAll(project.getGradle().getStartParameter().getInitScripts().stream().flatMap(t -> Stream.of("--init-script", t.toString())).collect(Collectors.toList())));
            });
        }
    }
}
