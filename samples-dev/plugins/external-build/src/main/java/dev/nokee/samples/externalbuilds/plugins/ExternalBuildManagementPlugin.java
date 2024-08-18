package dev.nokee.samples.externalbuilds.plugins;

import dev.nokee.samples.externalbuilds.ExternalBuildManagement;
import dev.nokee.samples.externalbuilds.ConfigurableExternalBuildSpec;
import dev.nokee.samples.externalbuilds.internal.DirectoryResolver;
import dev.nokee.samples.externalbuilds.internal.ExternalBuildSpec;
import dev.nokee.samples.externalbuilds.internal.ExternalBuildsInternal;
import org.gradle.api.Action;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.initialization.Settings;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;

import javax.inject.Inject;

import static dev.nokee.samples.externalbuilds.internal.DoNothingAction.doNothing;

/*private*/ abstract /*final*/ class ExternalBuildManagementPlugin implements Plugin<Settings> {
    private final ObjectFactory objects;

    @Inject
    public ExternalBuildManagementPlugin(ObjectFactory objects) {
        this.objects = objects;
    }

    @Override
    public void apply(Settings settings) {
        final DefaultExternalBuildManagement extension = settings.getExtensions().create("externalBuildManagement", DefaultExternalBuildManagement.class, new DirectoryResolver(objects));
        settings.getGradle().allprojects(new FlushExternalBuildsToProjectAction(extension.externalBuildSpecs));

        settings.getPluginManager().apply("dev.nokee.samples.external-build-models");
        settings.getGradle().rootProject(project -> {
            project.getPluginManager().apply("samplesdev.rules.idea-linked-external-gradle-projects");
        });
    }

    private static final class FlushExternalBuildsToProjectAction implements Action<Project> {
        private final Iterable<ExternalBuildSpec> specs;

        private FlushExternalBuildsToProjectAction(Iterable<ExternalBuildSpec> specs) {
            this.specs = specs;
        }

        @Override
        public void execute(Project project) {
            project.getPluginManager().apply(ExternalBuildsExtensionPlugin.class);
            project.getExtensions().getByType(ExternalBuildsInternal.class).addAll(specs);
        }
    }

    /*private*/ static abstract /*final*/ class DefaultExternalBuildManagement implements ExternalBuildManagement {
        private final DirectoryResolver directoryResolver;
        private final DomainObjectSet<ExternalBuildSpec> externalBuildSpecs;
        private final ObjectFactory objects;
        private final ProviderFactory providers;

        @Inject
        public DefaultExternalBuildManagement(DirectoryResolver directoryResolver, ObjectFactory objects, ProviderFactory providers) {
            this.directoryResolver = directoryResolver;
            this.externalBuildSpecs = objects.domainObjectSet(ExternalBuildSpec.class);
            this.objects = objects;
            this.providers = providers;
        }

        @Override
        public void externalBuild(Object rootProject) {
            externalBuild(rootProject, doNothing());
        }

        @Override
        public void externalBuild(Object rootProject, Action<ConfigurableExternalBuildSpec> action) {
            final DefaultExternalBuildSpec spec = objects.newInstance(DefaultExternalBuildSpec.class);
            spec.getName().convention(spec.getProjectDirectory().map(it -> it.getAsFile().getName()));
            spec.getProjectDirectory().value(providers.provider(() -> directoryResolver.resolve(rootProject))).disallowChanges();
            action.execute(spec);
            externalBuildSpecs.add(spec);
        }
    }

    /*private*/ static abstract /*final*/ class DefaultExternalBuildSpec implements ExternalBuildSpec, ConfigurableExternalBuildSpec {
        @Inject
        public DefaultExternalBuildSpec() {}

        @Override
        public abstract DirectoryProperty getProjectDirectory();
    }
}
