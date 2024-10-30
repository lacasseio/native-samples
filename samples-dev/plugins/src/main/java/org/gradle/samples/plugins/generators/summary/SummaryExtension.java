package org.gradle.samples.plugins.generators.summary;

import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class SummaryExtension {
    @Inject
    public SummaryExtension() {}

    public abstract Property<String> getTitle();

    // TODO: description
    // TODO: version
    // TODO: author
    // TODO: tags
}
