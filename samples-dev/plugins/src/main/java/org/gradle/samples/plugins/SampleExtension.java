package org.gradle.samples.plugins;

import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.Property;

public interface SampleExtension extends ExtensionAware {
    Property<String> getName(); // TODO: Should be slug
}
