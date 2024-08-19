package org.gradle.samples.plugins;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/*private*/ abstract class SamplesExtension implements ExtensionAware {
    public abstract void include(String path);

    private interface PathResolver {
        Path resolve(String path);
    }

    private interface SampleNamer {
        String determineName(Path path);
    }

    /*private*/ static abstract /*final*/ class ForSettings extends SamplesExtension implements SamplesExtensionInternal {
        private final Set<Sample> samples = new LinkedHashSet<>();
        private final PathResolver resolver;
        private final SampleNamer namer;

        @Inject
        public ForSettings(PathResolver resolver, SampleNamer namer) {
            this.resolver = resolver;
            this.namer = namer;
        }

        @Override
        public void include(String path) {
            final Path location = resolver.resolve(path);
            samples.add(new DefaultSample(namer.determineName(location), location));
        }

        @Override
        public Iterator<Sample> iterator() {
            return samples.iterator();
        }
    }

    /*private*/ static abstract /*final*/ class ForSettingsRule implements Plugin<Settings> {
        @Inject
        public ForSettingsRule() {}

        @Override
        public void apply(Settings settings) {
            final SamplesExtension.ForSettings extension = settings.getExtensions().create("samples", SamplesExtension.ForSettings.class, (PathResolver) settings.getSettingsDir().toPath()::resolve, new SampleNamer() {
                @Override
                public String determineName(Path path) {
                    return settings.getSettingsDir().toPath().relativize(path).toString().replace('/', '-').replace('\\', '-');
                }
            });

            settings.getGradle().rootProject(project -> {
                project.getPluginManager().apply(ForProject.Rule.class);

                // Transfer samples from settings to project
                project.getExtensions().getByType(ForProject.class).samples.addAll(extension.samples);
            });
        }
    }

    /*private*/ static abstract /*final*/ class ForProject implements Samples {
        private final List<Sample> samples = new ArrayList<>();
        private final ProviderFactory providers;

        @Inject
        public ForProject(ProviderFactory providers) {
            this.providers = providers;
        }

        @Override
        public Provider<List<Sample>> toProvider() {
            return providers.provider(() -> samples);
        }

        /*private*/ static abstract /*final*/ class Rule implements Plugin<Project> {
            @Inject
            public Rule() {}

            @Override
            public void apply(Project project) {
                project.getExtensions().create("samples", ForProject.class);
            }
        }
    }

    private static final class DefaultSample implements Sample {
        private final String name;
        private final Path location;

        public DefaultSample(String name, Path location) {
            this.name = name;
            this.location = location;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Path getLocation() {
            return location;
        }
    }
}
