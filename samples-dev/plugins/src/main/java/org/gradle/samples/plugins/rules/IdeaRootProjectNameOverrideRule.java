package org.gradle.samples.plugins.rules;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.samples.plugins.SampleExtension;

import javax.annotation.Nullable;
import javax.inject.Inject;

/*private*/ abstract /*final*/ class IdeaRootProjectNameOverrideRule implements Plugin<Settings> {
    private final ProviderFactory providers;

    @Inject
    public IdeaRootProjectNameOverrideRule(ProviderFactory providers) {
        this.providers = providers;
    }

    @Override
    public void apply(Settings settings) {
        if (isIdeaSyncActive()) {
            settings.getGradle().settingsEvaluated(__ -> {
                @Nullable
                final SampleExtension extension = settings.getExtensions().findByType(SampleExtension.class);
                if (extension != null) {
                    System.out.println("OVERRIDE : " + extension.getName().get());
                    settings.getRootProject().setName(extension.getName().get());
                }
            });
        }
    }

    private boolean isIdeaSyncActive() {
        return providers.systemProperty("idea.sync.active").map(Boolean::parseBoolean).orElse(false).get();
    }
}
