package org.gradle.samples.plugins;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.samples.plugins.generators.SourceCopyTask;
import org.gradle.samples.plugins.util.ClosureWrappedConfigureAction;
import org.gradle.util.Configurable;
import org.gradle.util.ConfigureUtil;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract /*final*/ class CopySourceExtension {

    /*private*/ static abstract /*final*/ class DefaultCopySourceExtension extends CopySourceExtension implements Configurable<SampleGeneratorTask> {
        private final List<Action<SampleGeneratorTask>> sourceActions = new ArrayList<>();

        @Inject
        public DefaultCopySourceExtension() {}

        @Override
        public SampleGeneratorTask configure(@SuppressWarnings("rawtypes") Closure cl) {
            sourceActions.add(new ClosureWrappedConfigureAction<>(cl));
            return null; // don't bother returning something
        }

        public List<Action<SampleGeneratorTask>> getSourceActions() {
            return sourceActions;
        }
    }

    /*private*/ static abstract /*final*/ class Rule implements Plugin<Settings> {
        @Inject
        public Rule() {}

        @Override
        public void apply(Settings settings) {
            settings.getPluginManager().withPlugin("samplesdev.rules.settings-sample-extension", __ -> {
                DefaultCopySourceExtension sample = settings.getExtensions().getByType(SampleExtension.class).getExtensions().create("copySource", DefaultCopySourceExtension.class);

                settings.getGradle().rootProject(project -> {
                    TaskProvider<SourceCopyTask> generatorTask = project.getTasks().register("gen", SourceCopyTask.class);
                    generatorTask.configure(task -> {
                        task.getSampleDir().set(project.getProjectDir());
                        task.getTemplatesDir().fileProvider(project.provider(() -> {
                            File baseDir = project.getProjectDir();
                            do {
                                if (new File(baseDir, ".sampleroot").exists()) {
                                    return new File(baseDir, "samples-dev/src/templates");
                                }
                                baseDir = baseDir.getParentFile();
                            } while (baseDir != null);

                            return null;
                        }));
                        sample.getSourceActions().forEach(it -> it.execute(task));
                    });
                });
            });
        }
    }
}
