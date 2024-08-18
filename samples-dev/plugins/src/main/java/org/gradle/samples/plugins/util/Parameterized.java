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
package org.gradle.samples.plugins.util;

import com.google.common.reflect.TypeToken;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.gradle.api.provider.ValueSourceSpec;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.services.BuildServiceRegistration;
import org.gradle.api.services.BuildServiceSpec;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;

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
public interface Parameterized<P> {
	void parameters(Action<? super P> action);
	P getParameters();

	abstract class Task<P extends WorkParameters & Task.CopyTo<P>> extends DefaultTask implements Parameterized<P> {
		private final Class<? extends WorkAction<P>> actionType;
		private final WorkQueueFactory factory;
		private final P parameters;

		protected Task(Class<? extends WorkAction<P>> actionType, WorkQueueFactory factory) {
			this.actionType = actionType;
			this.factory = factory;
			this.parameters = getObjects().newInstance((Class<P>) new TypeToken<P>(getClass()) {
			}.getRawType());
		}

		public final void parameters(Action<? super P> action) {
			action.execute(getParameters());
//		return this;
		}

		protected interface CopyTo<T> {
			CopyTo<T> copyTo(T other);
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
