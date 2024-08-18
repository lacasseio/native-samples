package org.gradle.samples.plugins;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Callable;

/*private*/ abstract /*final*/ class SamplePlugin implements Plugin<Settings> {
    @Inject
    public SamplePlugin() {}

    @Override
    public void apply(Settings settings) {
        // IDEA
        settings.getPluginManager().apply("samplesdev.rules.idea-root-project-name-override");
        settings.getPluginManager().apply("dev.nokee.samples.external-build-models");

        settings.getPluginManager().apply("samplesdev.rules.settings-sample-extension");
        settings.getPluginManager().apply("samplesdev.rules.copy-source-extension");

        settings.getGradle().rootProject(project -> {
            project.getPluginManager().apply("samplesdev.rules.sample-lifecycle-base");

            project.getTasks().named("generateSource", task -> {
                task.dependsOn((Callable<?>) () -> {
                    return Optional.ofNullable(project.getTasks().findByName("copySource")).map(Object.class::cast).orElse(Collections.emptyList());
                });
            });
        });

        SampleExtension extension = settings.getExtensions().getByType(SampleExtension.class);
        extension.getName().convention(settings.getSettingsDir().getParentFile().getName() + "-" + settings.getSettingsDir().getName());
    }
}
