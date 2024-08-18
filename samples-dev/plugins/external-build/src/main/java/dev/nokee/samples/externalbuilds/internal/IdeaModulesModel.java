package dev.nokee.samples.externalbuilds.internal;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.IncludedBuild;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;

import javax.inject.Inject;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*private*/ final class IdeaModulesModel implements BuildModules, Serializable {
    private final Set<Module> modules;

    public IdeaModulesModel(Collection<Module> modules) {
        this.modules = new LinkedHashSet<>(modules);
    }

    @Override
    public Iterable<Module> get() {
        return modules;
    }

    /*private*/ static abstract /*final*/ class Rule implements Plugin<Project> {
        private final ToolingModelBuilderRegistry registry;

        @Inject
        public Rule(ToolingModelBuilderRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void apply(Project project) {
            assert project.getParent() == null : "must be applied only on root project";
            registry.register(new Builder());
        }
    }

    private static class Builder implements ToolingModelBuilder {
        @Override
        public boolean canBuild(String modelName) {
            return modelName.equals(BuildModules.class.getName());
        }

        @Override
        public Object buildAll(String modelName, Project project) {
            return new BuildModulesModel(Stream.concat(project.getGradle().getIncludedBuilds().stream().map(this::transform), project.getRootProject().getAllprojects().stream().map(this::transform)).collect(Collectors.toList()));
        }

        public Module transform(IncludedBuild build) {
            return new IncludedBuildModule(build.getName(), build.getProjectDir().toPath());
        }

        public Module transform(Project project) {
            return new ProjectModule(project.getProjectDir().toPath());
        }

        private static class BuildModulesModel implements BuildModules, Serializable {
            private final Iterable<Module> values;

            public BuildModulesModel(Iterable<Module> values) {
                this.values = values;
            }

            public Iterable<Module> get() {
                return values;
            }
        }

        private static final class IncludedBuildModule implements Module, Serializable {
            private final String name;
            private final File location;

            public IncludedBuildModule(String name, Path location) {
                this.name = name;
                this.location = location.toFile();
            }

            public String getName() {
                return name;
            }

            @Override
            public Path getLocation() {
                return location.toPath();
            }
        }

        private static final class ProjectModule implements Module, Serializable {
            private final File location;

            public ProjectModule(Path location) {
                this.location = location.toFile();
            }

            @Override
            public Path getLocation() {
                return location.toPath();
            }
        }
    }
}
