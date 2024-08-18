package org.gradle.samples.plugins;

import org.gradle.api.provider.Provider;

import java.util.List;

public interface Samples {
    Provider<List<Sample>> toProvider();
}
