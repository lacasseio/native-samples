package dev.nokee.samples.externalbuilds.internal.model;

import java.nio.file.Path;

public interface BuildModules {
    Iterable<Module> get();

    interface Module {
        Path getLocation();

        interface IncludedBuild extends Module {
            String getName();
        }
    }
}
