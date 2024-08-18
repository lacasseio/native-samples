package dev.nokee.samples.externalbuilds.plugins;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;

import javax.inject.Inject;

/*private*/ abstract /*final*/ class ExternalBuildModelPlugin implements Plugin<Settings> {
    @Inject
    public ExternalBuildModelPlugin() {}

    @Override
    public void apply(Settings settings) {
        settings.getGradle().rootProject(this::applyTo);
    }

    private void applyTo(Project project) {
        project.getPluginManager().apply("samplesdev.rules.idea-external-builds-model");
        project.getPluginManager().apply("samplesdev.rules.idea-build-modules-model");
    }
}
