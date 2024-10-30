package org.gradle.samples.plugins.generators.manifest;

import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;

public abstract class ManifestExtension {
    @Inject
    public ManifestExtension() {}

    public void put(String key, Object value) {
        getElements().put(key, value);
    }

    public void put(String key, Provider<?> value) {
        getElements().put(key, value);
    }

    abstract MapProperty<String, Object> getElements();
}
