package org.gradle.samples.plugins.rules;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.samples.plugins.SampleExtension;

import javax.inject.Inject;

/*private*/ abstract /*final*/ class SampleExtensionRule implements Plugin<Settings> {
    @Inject
    public SampleExtensionRule() {}

    @Override
    public void apply(Settings settings) {
        settings.getExtensions().create("sample", DefaultSampleExtension.class);
    }

    /*private*/ static abstract /*final*/ class DefaultSampleExtension implements SampleExtension {
        @Inject
        public DefaultSampleExtension() {}
    }
}
