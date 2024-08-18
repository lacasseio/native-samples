package org.gradle.samples.plugins.generators;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

public abstract class SamplesManifestTaskEx extends DefaultTask {
    @InputFiles
    public abstract ConfigurableFileCollection getManifestFiles();

    @OutputFile
    public abstract RegularFileProperty getManifest();

    @TaskAction
    private void generateManifest() throws IOException {
        try (PrintWriter writer = new PrintWriter(Files.newOutputStream(getManifest().get().getAsFile().toPath()))) {
            for (File manifestFile : getManifestFiles()) {
                for (String line : Files.readAllLines(manifestFile.toPath())) {
                    String[] tokens = line.split("=");
                    String path = tokens[1];
                    String relPath = getProject().getProjectDir().toPath().relativize(Paths.get(path)).toString();
                    writer.println(tokens[0] + "=" + relPath);
                }
            }
        }
    }
}
