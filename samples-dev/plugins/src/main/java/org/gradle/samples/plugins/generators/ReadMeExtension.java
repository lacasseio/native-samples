package org.gradle.samples.plugins.generators;

import org.gradle.api.file.RegularFileProperty;

public abstract class ReadMeExtension {
    public abstract RegularFileProperty getLocation();
}
