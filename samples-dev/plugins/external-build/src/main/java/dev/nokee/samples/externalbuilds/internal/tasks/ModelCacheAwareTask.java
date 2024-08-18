package dev.nokee.samples.externalbuilds.internal.tasks;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.tasks.Internal;
import org.gradle.build.event.BuildEventsListenerRegistry;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationCompletionListener;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static dev.nokee.samples.externalbuilds.internal.tasks.ParameterizedTask.type;

interface ModelCacheAwareTask<P extends ModelCacheAwareTask.Parameters> extends ParameterizedTask<P> {
    interface Parameters extends ParameterizedTask.Parameters {
        @Internal
        Property<CacheAccess> getCache();
    }

    /*private*/ abstract /*final*/ class Rule implements Plugin<Project> {
        private final BuildEventsListenerRegistry registry;

        @Inject
        public Rule(BuildEventsListenerRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void apply(Project project) {
            final Provider<ClearStateService> d = project.getGradle().getSharedServices().registerIfAbsent("clearState", ClearStateService.class, spec -> {});
            registry.onTaskCompletion(d);
            project.getTasks().withType(type(ModelCacheAwareTask.Parameters.class)).configureEach(task -> {
                task.usesService(d);
                task.parameters(it -> it.getCache().set(d.map(t -> t.cacheOf(task))));
            });
        }
    }

    public static final class CacheAccess implements Serializable {
        private final String name;

        @Inject
        public CacheAccess(String name) {
            this.name = name;
        }

        public void store(Object obj) {
            ClearStateService.map.put(name, obj);
        }

        public Object load() {
            return ClearStateService.map.get(name);
        }
    }

    /*private*/ static abstract /*final*/ class ClearStateService implements BuildService<BuildServiceParameters.None>, OperationCompletionListener, AutoCloseable {
        private static final ConcurrentMap<String, Object> map = new ConcurrentHashMap<>();
        private final ObjectFactory objects;

        @Inject
        public ClearStateService(ObjectFactory objects) {
            this.objects = objects;
        }

        @Override
        public void close() throws Exception {
            map.clear();
        }

        @Override
        public void onFinish(FinishEvent event) {

        }

        public CacheAccess cacheOf(Task task) {
            return new CacheAccess(task.getName());
        }
    }
}
