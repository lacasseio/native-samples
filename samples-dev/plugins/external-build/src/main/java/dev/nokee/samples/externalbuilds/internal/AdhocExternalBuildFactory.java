package dev.nokee.samples.externalbuilds.internal;

import dev.nokee.samples.externalbuilds.ExternalBuild;
import dev.nokee.samples.externalbuilds.internal.tasks.CrossProjectModelTask;
import dev.nokee.samples.externalbuilds.internal.tasks.CrossProjectTask;
import org.gradle.api.Buildable;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskDependency;
import org.gradle.api.tasks.TaskProvider;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class AdhocExternalBuildFactory {
    private final Project project;
    private final ObjectFactory objects;

    public AdhocExternalBuildFactory(Project project) {
        this.project = project;
        this.objects = project.getObjects();
    }

    public ExternalBuild create(Directory rootProject) {
        return objects.newInstance(DefaultExternalBuild.class, new ExternalBuildLocation(rootProject, project.getRootProject().relativePath(rootProject)));
    }

    private interface TaskReferenceFactory {
        ExternalBuild.TaskReference create(String path);
    }

    private static final class CachingTaskReferenceFactory implements TaskReferenceFactory {
        private final Map<String, ExternalBuild.TaskReference> cache = new HashMap<>();
        private final TaskReferenceFactory delegate;

        private CachingTaskReferenceFactory(TaskReferenceFactory delegate) {
            this.delegate = delegate;
        }

        @Override
        public ExternalBuild.TaskReference create(String path) {
            return cache.computeIfAbsent(path, delegate::create);
        }
    }

    private static final class DefaultTaskReferenceFactory implements TaskReferenceFactory {
        private final TaskContainer tasks;
        private final ExternalBuildLocation location;

        private DefaultTaskReferenceFactory(TaskContainer tasks, ExternalBuildLocation location) {
            this.tasks = tasks;
            this.location = location;
        }

        @Override
        public ExternalBuild.TaskReference create(String taskPath) {
            assert taskPath.startsWith(":") : "must be an absolute task path";
            return new ExternalTaskReference() {
                private final TaskProvider<CrossProjectTask> referenceTask = tasks.register(location.taskFragment() + "_" + taskPath.replace(':', '_'), CrossProjectTask.class, task -> {
                    task.setDescription(String.format("[internal] Execute task '%s' on external project '%s'.", taskPath, location.getRelativePath()));
                    task.parameters(parameters -> {
                        parameters.getTaskPath().set(taskPath);
                        parameters.getProjectDirectory().set(location.getPath());
                    });
                });

                @Override
                public TaskDependency getBuildDependencies() {
                    return __ -> Collections.singleton(referenceTask.get());
                }
            };
        }

        public static abstract class ExternalTaskReference implements ExternalBuild.TaskReference, Buildable {}
    }

    private interface ModelReferenceFactory {
        <T> ExternalBuild.ModelReference<T> create(Class<T> model);
    }

    private static final class CachingModelReferenceFactory implements ModelReferenceFactory {
        private final Map<Class<?>, ExternalBuild.ModelReference<?>> cache = new HashMap<>();
        private final ModelReferenceFactory delegate;

        private CachingModelReferenceFactory(ModelReferenceFactory delegate) {
            this.delegate = delegate;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> ExternalBuild.ModelReference<T> create(Class<T> model) {
            return (ExternalBuild.ModelReference<T>) cache.computeIfAbsent(model, delegate::create);
        }
    }

    private static final class DefaultModelReferenceFactory implements ModelReferenceFactory {
        private final TaskContainer tasks;
        private final ExternalBuildLocation location;

        private DefaultModelReferenceFactory(TaskContainer tasks, ExternalBuildLocation location) {
            this.tasks = tasks;
            this.location = location;
        }

        @Override
        public <T> ExternalBuild.ModelReference<T> create(Class<T> model) {
            return new ExternalModelReference<T>() {
                private final TaskProvider<CrossProjectModelTask> referenceTask = tasks.register(location.taskFragment() + "__" + model.getName(), CrossProjectModelTask.class, task -> {
                    task.setDescription(String.format("[internal] Query model '%s' on external project '%s'.", model.getCanonicalName(), location.getRelativePath()));
                    task.parameters(parameters -> {
                        parameters.getModelType().set(model.getCanonicalName());
                        parameters.getProjectDirectory().set(location.getPath());
                    });
                });

                @Override
                public Provider<T> asProvider() {
                    return referenceTask.map(CrossProjectModelTask::getDeserializedModel);
                }
            };
        }

        public static abstract class ExternalModelReference<T> implements ExternalBuild.ModelReference<T> {}
    }

    /*private*/ static class DefaultExternalBuild implements ExternalBuild {
        private final TaskReferenceFactory taskReferenceFactory;
        private final ModelReferenceFactory modelReferenceFactory;
        private final Provider<Directory> projectDirectory;

        @Inject
        public DefaultExternalBuild(ProviderFactory providers, TaskContainer tasks, ExternalBuildLocation projectDirectory) {
            this.projectDirectory = providers.provider(() -> projectDirectory.getPath());
            this.taskReferenceFactory = new CachingTaskReferenceFactory(new DefaultTaskReferenceFactory(tasks, projectDirectory));
            this.modelReferenceFactory = new CachingModelReferenceFactory(new DefaultModelReferenceFactory(tasks, projectDirectory));
        }

        @Override
        public Provider<Directory> getProjectDirectory() {
            return projectDirectory;
        }

        @Override
        public TaskReference task(String path) {
            return taskReferenceFactory.create(path);
        }

        @Override
        public <T> ModelReference<T> model(Class<T> model) {
            return modelReferenceFactory.create(model);
        }
    }

    public static final class ExternalBuildLocation {
            private final Directory path;
            private final String relativePath;

            public ExternalBuildLocation(Directory path, String relativePath) {
                assert !relativePath.startsWith("..") : "does not support external build location outside of the workspace";
                this.path = path;
                this.relativePath = relativePath;
            }

            public Directory getPath() {
                return path;
            }

            public String getRelativePath() {
                return relativePath;
            }

            private String taskFragment() {
                return relativePath.replace('/', '_');
            }
        }
}
