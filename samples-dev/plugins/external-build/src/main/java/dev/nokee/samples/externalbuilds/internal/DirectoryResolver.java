package dev.nokee.samples.externalbuilds.internal;

import org.gradle.api.file.Directory;
import org.gradle.api.model.ObjectFactory;

import java.io.File;

public final class DirectoryResolver {
    private final ObjectFactory objects;

    public DirectoryResolver(ObjectFactory objects) {
        this.objects = objects;
    }

    public Directory resolve(Object path) {
        final File resolved = objects.fileCollection().from(path).getSingleFile();
        assert resolved.isDirectory();
        return objects.directoryProperty().fileValue(resolved).get();
    }
}
