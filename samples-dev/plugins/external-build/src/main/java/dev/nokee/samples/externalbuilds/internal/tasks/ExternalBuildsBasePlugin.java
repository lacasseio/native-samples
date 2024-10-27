package dev.nokee.samples.externalbuilds.internal.tasks;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.reflect.TypeOf;

import javax.inject.Inject;

/*private*/ abstract /*final*/ class ExternalBuildsBasePlugin implements Plugin<Project> {
    @Inject
    public ExternalBuildsBasePlugin() {}

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("samplesdev.rules.gradle-version-tasks");
        project.getPluginManager().apply("samplesdev.rules.max-parallel-gradle-tasks");
        project.getPluginManager().apply("samplesdev.rules.model-cache-tasks");
        project.getPluginManager().apply("samplesdev.rules.init-script-arguments");
        project.getPluginManager().apply("sampledevs.rules.limit-concurrent-builds");

        project.getExtensions().add(new TypeOf<Class<? extends ExternalBuildTask>>() {}, "ExternalBuildTask", CrossProjectTask.class);
    }
}
