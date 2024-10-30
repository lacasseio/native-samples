package org.gradle.samples.plugins.generators.readme;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.process.ExecOperations;
import org.gradle.samples.plugins.generators.Sample;
import org.gradle.samples.plugins.generators.SamplesExtension;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;

/*private*/ abstract /*final*/ class ReadMePlugin implements Plugin<Project> {
    @Inject
    public ReadMePlugin() {}

    @Override
    public void apply(Project project) {
        SamplesExtension extension = project.getExtensions().getByType(SamplesExtension.class);
        extension.getSamples().configureEach(sample -> {
            ReadMeExtension readme = sample.getExtensions().create("readme", ReadMeExtension.class);
            readme.getLocation().convention(sample.getSampleDir().map(it -> {
                if (it.file("README.md").getAsFile().exists()) {
                    return it.file("README.md");
                } else if (it.file("README.adoc").getAsFile().exists()) {
                    return it.file("README.adoc");
                }
                return null;
            }));

            project.getTasks().register(RenderPlainReadMeTask.taskName(sample), RenderPlainReadMeTask.class, task -> {
                task.parameters(parameters -> {
                    parameters.getInputFile().set(readme.getLocation());
                    parameters.getOutputFile().fileProvider(project.provider(task.getTemporaryDirFactory()::create).map(it -> new File(it, "README")));
                });
            });

            project.getTasks().withType(Zip.class).configureEach(task -> {
                if (task.getName().equals("zip" + sample.getName())) {
                    task.exclude(it -> it.getFile().equals(readme.getLocation().getAsFile().get()));
                    task.from(project.getTasks().named(RenderPlainReadMeTask.taskName(sample)));
                }
            });

            sample.content(spec -> spec.from(readme.getLocation()));
        });
    }

    /*private*/ static abstract /*final*/ class RenderPlainReadMeTask extends ParameterizedTask.UsingWorker<RenderPlainReadMeTask.Parameters> {
        public interface Parameters extends ParameterizedTask.Parameters, WorkParameters, ParameterizedTask.UsingWorker.CopyTo<Parameters> {
            @InputFile
            public abstract RegularFileProperty getInputFile();

            @OutputFile
            public abstract RegularFileProperty getOutputFile();
        }

        @Inject
        public RenderPlainReadMeTask(WorkerExecutor executor) {
            super(TaskWorkAction.class, executor::noIsolation);
        }

        public static String taskName(Sample sample) {
            return sample.getName() + "PlainReadMe";
        }

        /*private*/ static abstract /*final*/ class TaskWorkAction implements WorkAction<Parameters> {
            private final ExecOperations execOperations;

            @Inject
            public TaskWorkAction(ExecOperations execOperations) {
                this.execOperations = execOperations;
            }

            @Override
            public void execute() {
                execOperations.exec(spec -> {
                    spec.commandLine("pandoc", "-t", "plain", "-o", getParameters().getOutputFile().getAsFile().get(), getParameters().getInputFile().getAsFile().get());
                });
            }
        }
    }
}
