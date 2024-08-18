/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.nokee.samples.externalbuilds.internal.tasks;

import com.google.common.reflect.TypeToken;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.HasConfigurableValue;
import org.gradle.api.provider.HasMultipleValues;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkQueue;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Parameterized tasks represent a task that execute solely through a Gradle worker allowing parallel-by-default.
 *
 * <p>The implementor needs to specify two important aspect: 1) the WorkAction class and 2) a WorkQueue factory.
 *
 * <p>The ParameterizedTask type should not be exposed to the users, they should only be aware of {@code parameters(Action)}.
 *
 * @param <P> the parameter types
 */
@SuppressWarnings({"unchecked", "UnstableApiUsage"})
public interface ParameterizedTask<P extends ParameterizedTask.Parameters> extends Task {
	interface Parameters {
		class None implements Parameters {}
	}

	void parameters(Action<? super P> action);
	P getParameters();

	static <T extends ParameterizedTask<P>, P extends ParameterizedTask.Parameters> Class<T> type(Class<P> type) {
		return (Class<T>) type.getEnclosingClass();
	}

	abstract class UsingWorker<P extends Parameters & WorkParameters & UsingWorker.CopyTo<P>> extends DefaultTask implements ParameterizedTask<P> {
		private final Class<? extends WorkAction<P>> actionType;
		private final WorkQueueFactory factory;
		private final P parameters;

		protected UsingWorker(Class<? extends WorkAction<P>> actionType, WorkQueueFactory factory) {
			this.actionType = actionType;
			this.factory = factory;
			this.parameters = getObjects().newInstance((Class<P>) new TypeToken<P>(getClass()) {
			}.getRawType());
		}

		public final void parameters(Action<? super P> action) {
			action.execute(getParameters());
		}

		protected interface CopyTo<T> {
			default CopyTo<T> copyTo(T other) {
				for (Method method : other.getClass().getDeclaredMethods()) {
					if (method.getName().startsWith("get") && HasConfigurableValue.class.isAssignableFrom(method.getReturnType())) {
						try {
							final Object src = method.invoke(this);
							final Object dst = method.invoke(other);
							if (ConfigurableFileCollection.class.isAssignableFrom(method.getReturnType())) {
								((ConfigurableFileCollection) dst).setFrom(src);
							} else if (Property.class.isAssignableFrom(method.getReturnType())) {
								((Property<Object>) dst).set((Provider<?>) src);
							} else if (HasMultipleValues.class.isAssignableFrom(method.getReturnType())) {
								((HasMultipleValues<Object>) dst).set((Provider<? extends Iterable<?>>) src);
							} else if (MapProperty.class.isAssignableFrom(method.getReturnType())) {
								((MapProperty<Object, Object>) dst).set((Provider<? extends
										Map<Object, Object>>) src);
							}
						} catch (InvocationTargetException | IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
				}
				return this;
			}
		}

		@Nested
		// We cannot mark this getter as `final` because of https://github.com/gradle/gradle/issues/24747
		//   The intention here is to prevent any implementer to override this getter as they don't own the returned instance.
		//   Please, do not override this method!
		public /*final*/ P getParameters() {
			return parameters;
		}

		@Inject
		// This method is considered private to this class, do not use!
		// TODO: try to see if it can be private
		protected abstract ObjectFactory getObjects();

		@TaskAction
		private void doGenerate() {
			factory.create().submit(actionType, getParameters()::copyTo);
		}

		public interface WorkQueueFactory {
			WorkQueue create();
		}
	}
}
