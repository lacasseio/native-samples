package dev.nokee.samples.externalbuilds.plugins;

import dev.nokee.samples.externalbuilds.ExternalBuild;
import dev.nokee.samples.externalbuilds.ExternalBuilds;
import dev.nokee.samples.externalbuilds.internal.AdhocExternalBuildFactory;
import dev.nokee.samples.externalbuilds.internal.DirectoryResolver;
import dev.nokee.samples.externalbuilds.internal.ExternalBuildSpec;
import dev.nokee.samples.externalbuilds.internal.ExternalBuildsInternal;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.Set;

import static dev.nokee.samples.externalbuilds.internal.TransformEachTransformer.transformEach;

/*private*/ abstract /*final*/ class ExternalBuildsExtensionPlugin implements Plugin<Project> {
    @Inject
    public ExternalBuildsExtensionPlugin() {}

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("samplesdev.rules.gradle-version-tasks");
        project.getPluginManager().apply("samplesdev.rules.max-parallel-gradle-tasks");
        project.getPluginManager().apply("samplesdev.rules.model-cache-tasks");

        project.getExtensions().create("externalBuilds", DefaultExternalBuilds.class, new AdhocExternalBuildFactory(project), new DirectoryResolver(project.getObjects()));
    }

    /*private*/ static abstract /*final*/ class DefaultExternalBuilds implements ExternalBuilds, ExternalBuildsInternal {
        private final NamedDomainObjectSet<NamedExternalBuild> knownExternalBuilds;
        private final ObjectFactory objects;
        private final AdhocExternalBuildFactory adhocFactory;
        private final DirectoryResolver resolver;

        @Inject
        public DefaultExternalBuilds(ObjectFactory objects, AdhocExternalBuildFactory adhocFactory, DirectoryResolver resolver) {
            this.knownExternalBuilds = objects.namedDomainObjectSet(NamedExternalBuild.class);
            this.objects = objects;
            this.adhocFactory = adhocFactory;
            this.resolver = resolver;
        }

        @Override
        public boolean addAll(Iterable<ExternalBuildSpec> elements) {
            final Provider<Set<NamedExternalBuild>> mappedElements = objects.setProperty(NamedExternalBuild.class).value(objects.setProperty(ExternalBuildSpec.class).value(elements).map(transformEach(it -> objects.newInstance(NamedExternalBuild.class, it.getName().get(), adhocFactory.create(it.getProjectDirectory().get())))));
            knownExternalBuilds.addAllLater(mappedElements);
            return true;
        }

        @Override
        public ExternalBuild getByName(String name) {
            return knownExternalBuilds.getByName(name);
        }

        @Override
        public ExternalBuild externalBuildAt(Object rootProject) {
            return adhocFactory.create(resolver.resolve(rootProject));
        }

        @Override
        public Iterator<ExternalBuild> iterator() {
            return knownExternalBuilds.stream().map(ExternalBuild.class::cast).iterator();
        }
    }

    /*private*/ static abstract /*final*/ class NamedExternalBuild implements ExternalBuild, Named {
        private final String name;
        private final ExternalBuild delegate;

        @Inject
        public NamedExternalBuild(String name, ExternalBuild delegate) {
            this.name = name;
            this.delegate = delegate;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Provider<Directory> getProjectDirectory() {
            return delegate.getProjectDirectory();
        }

        @Override
        public TaskReference task(String path) {
            return delegate.task(path);
        }

        @Override
        public <T> ModelReference<T> model(Class<T> model) {
            return delegate.model(model);
        }
    }
}
