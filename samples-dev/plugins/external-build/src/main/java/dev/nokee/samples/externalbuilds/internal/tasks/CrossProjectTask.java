package dev.nokee.samples.externalbuilds.internal.tasks;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;

import static dev.nokee.samples.externalbuilds.internal.NeverUpToDateTaskSpec.never_alwaysExecute;

public abstract /*final*/ class CrossProjectTask extends ParameterizedTask.UsingWorker<CrossProjectTask.Parameters> implements MaxParallelGradleTask, GradleVersionAwareParameterTask<CrossProjectTask.Parameters> {
        @Inject
        public CrossProjectTask(WorkerExecutor worker) {
            super(TaskWorkAction.class, worker::noIsolation);
            getOutputs().upToDateWhen(never_alwaysExecute());
        }

        public interface Parameters extends ParameterizedTask.Parameters, WorkParameters, GradleVersionAwareParameterTask.Parameters, ParameterizedTask.UsingWorker.CopyTo<Parameters> {
            @Internal
            Property<String> getTaskPath();

            @Internal
            DirectoryProperty getProjectDirectory();

            @Internal
            Property<String> getGradleVersion();
        }

        /*private*/ static abstract /*final*/ class TaskWorkAction implements WorkAction<Parameters> {
            @Inject
            public TaskWorkAction() {}

            @Override
            public void execute() {
                final GradleConnector connector = GradleConnector.newConnector();
                connector.useGradleVersion(getParameters().getGradleVersion().get());
                connector.forProjectDirectory(getParameters().getProjectDirectory().getAsFile().get());

                try (ProjectConnection connection = connector.connect()) {
                    final BuildLauncher launcher = connection.newBuild().forTasks(getParameters().getTaskPath().get());
                    launcher.run();
                }
            }
        }
    }