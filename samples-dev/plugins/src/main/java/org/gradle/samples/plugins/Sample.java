package org.gradle.samples.plugins;

import java.nio.file.Path;

public interface Sample {
    String getName();
    Path getLocation();
}
