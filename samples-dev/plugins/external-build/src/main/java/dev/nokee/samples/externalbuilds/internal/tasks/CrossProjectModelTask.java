package dev.nokee.samples.externalbuilds.internal.tasks;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Internal;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static dev.nokee.samples.externalbuilds.internal.NeverUpToDateTaskSpec.never_alwaysExecute;

/*private*/public abstract /*final*/ class CrossProjectModelTask extends ParameterizedTask.UsingWorker<CrossProjectModelTask.Parameters> implements MaxParallelGradleTask, GradleVersionAwareParameterTask<CrossProjectModelTask.Parameters>, ModelCacheAwareTask<CrossProjectModelTask.Parameters> {
    @Inject
    public CrossProjectModelTask(WorkerExecutor worker, ProviderFactory providers) {
        super(TaskWorkAction.class, worker::noIsolation);
        parameters(it -> it.getOutputFile().fileProvider(providers.provider(getTemporaryDirFactory()::create).map(t -> new File(t, "output.txt"))));
        getOutputs().upToDateWhen(never_alwaysExecute());
    }

    public interface Parameters extends ParameterizedTask.Parameters, WorkParameters, GradleVersionAwareParameterTask.Parameters, ModelCacheAwareTask.Parameters, CopyTo<Parameters> {
        @Internal
        Property<String> getModelType();

        @Internal
        DirectoryProperty getProjectDirectory();

        @Internal
        Property<String> getGradleVersion();

        @Internal
        RegularFileProperty getOutputFile();
    }

    @Internal
    @SuppressWarnings("unchecked")
    public <T> T getDeserializedModel() {
        return (T) getParameters().getCache().get().load();
    }

    /*private*/ static abstract /*final*/ class TaskWorkAction implements WorkAction<Parameters> {
        @Inject
        public TaskWorkAction() {}

        @Override
        public void execute() {
            final GradleConnector connector = GradleConnector.newConnector();
            connector.useGradleVersion(getParameters().getGradleVersion().get());
            connector.forProjectDirectory(getParameters().getProjectDirectory().getAsFile().get());

            try (OutputStream outStream = new FileOutputStream(getParameters().getOutputFile().getAsFile().get())) {
                try (ProjectConnection connection = connector.connect()) {
                    final ModelBuilder<?> launcher = connection.model(Class.forName(getParameters().getModelType().get()));
                    launcher.setStandardError(outStream);
                    launcher.setStandardOutput(outStream);
                    Object obj = launcher.get();
                    getParameters().getCache().get().store(obj);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}