package dev.nokee.samples.externalbuilds.internal;

import dev.nokee.samples.externalbuilds.ExternalBuild;
import dev.nokee.samples.externalbuilds.ExternalBuilds;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Transformer;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;

import javax.inject.Inject;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/*private*/ class IdeaExternalBuildsModel {
    /*private*/ static abstract /*final*/ class Rule implements Plugin<Project> {
        private final ToolingModelBuilderRegistry registry;

        @Inject
        public Rule(ToolingModelBuilderRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void apply(Project project) {
            assert project.getParent() == null : "must be applied only on root project";
            registry.register(new Builder(project.provider(() -> project.getExtensions().findByType(ExternalBuildsInternal.class)).map(transformEach(ExternalBuild::getProjectDirectory)).flatMap(it -> {
                SetProperty<Directory> result = project.getObjects().setProperty(Directory.class);
                it.forEach(result::add);
                return result;
            })));
        }
    }

    private static <OUT, IN> Transformer<Iterable<OUT>, Iterable<IN>> transformEach(Transformer<OUT, IN> mapper) {
        return it -> {
            return StreamSupport.stream(it.spliterator(), false).map(mapper::transform).collect(Collectors.toList());
        };
    }

    private static class Builder implements ToolingModelBuilder {
        private final Provider<Iterable<File>> locations;

        public Builder(Provider<Iterable<Directory>> locations) {
            this.locations = locations.map(transformEach(Directory::getAsFile));
        }

        @Override
        public boolean canBuild(String modelName) {
            return modelName.equals(ExternalBuildsM.class.getName());
        }

        @Override
        public Object buildAll(String modelName, Project project) {
            return new ExternalBuildsModel(locations.getOrElse(Collections.emptyList()));
        }

        private static final class ExternalBuildsModel implements ExternalBuildsM, Serializable {
            private final Iterable<File> values;

            public ExternalBuildsModel(Iterable<File> values) {
                this.values = values;
            }

            public Iterable<File> get() {
                return values;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o)
                    return true;
                if (o == null || getClass() != o.getClass())
                    return false;
                ExternalBuildsModel that = (ExternalBuildsModel) o;
                return Objects.equals(values, that.values);
            }

            @Override
            public int hashCode() {
                return Objects.hash(values);
            }
        }
    }
}
