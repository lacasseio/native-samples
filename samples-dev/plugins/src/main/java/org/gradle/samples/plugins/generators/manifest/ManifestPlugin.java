package org.gradle.samples.plugins.generators.manifest;

import groovy.json.JsonBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.samples.plugins.generators.GeneratorPlugin;
import org.gradle.samples.plugins.generators.SamplesExtension;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/*private*/ abstract /*final*/ class ManifestPlugin implements Plugin<Project> {
    @Inject
    public ManifestPlugin() {}

    @Override
    public void apply(Project project) {
        SamplesExtension extension = project.getExtensions().getByType(SamplesExtension.class);
        extension.getSamples().configureEach(sample -> {
            ManifestExtension manifest = sample.getExtensions().create("manifest", ManifestExtension.class);
            manifest.put("name", sample.getName());

            TaskProvider<WriteSampleManifestTask> manifestTask = project.getTasks().register(sample.getName() + "Manifest", WriteSampleManifestTask.class, task -> {
                task.getElements().putAll(manifest.getElements());
                task.getOutputFile().fileProvider(project.provider(task.getTemporaryDirFactory()::create).map(it -> new File(it, "manifest.json")));
            });

            sample.content(spec -> spec.from(manifestTask.flatMap(WriteSampleManifestTask::getOutputFile)));
        });
    }

    /*private*/ static abstract /*final*/ class WriteSampleManifestTask extends DefaultTask {
        @Inject
        public WriteSampleManifestTask() {}

        @Input
        public abstract MapProperty<String, Object> getElements();

        @OutputFile
        public abstract RegularFileProperty getOutputFile();

        @TaskAction
        private void doWrite() throws IOException {
            try (PrintWriter out = new PrintWriter(getOutputFile().get().getAsFile())) {
                new JsonBuilder(getElements().get()).writeTo(out);
            }
        }
    }
}
