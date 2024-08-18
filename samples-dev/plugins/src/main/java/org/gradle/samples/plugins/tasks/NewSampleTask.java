package org.gradle.samples.plugins.tasks;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract /*final*/ class NewSampleTask extends DefaultTask {
    @Option(option = "path", description = "path where to create project")
    @Internal
    public abstract Property<String> getProjectPath();

    @Internal
    protected abstract DirectoryProperty getProjectDirectory();

    @Internal
    protected abstract RegularFileProperty getSettingsFile();

    @Inject
    public NewSampleTask(ProjectLayout layout) {
        getProjectDirectory().value(layout.getProjectDirectory().dir(getProjectPath())).disallowChanges();
        getSettingsFile().value(layout.getProjectDirectory().file("settings.gradle"));
    }

    @TaskAction
    private void doGenerate() throws IOException {
        List<Writable> result = new ArrayList<>();

        Path projectDirectory = getProjectDirectory().get().getAsFile().toPath();

        result.add(new SettingsFile());
        result.add(new BuildFile());
        result.add(new GradleWrapperFiles(projectDirectory));
        result.add(new ReadmeFile(projectDirectory));
        result.add(new SampleEntry());

        for (Writable writable : result) {
            writable.write();
        }
    }

    private interface Writable {
        void write() throws IOException;
    }

    private final class SettingsFile implements Writable {
        private final Path settingsFile;

        public SettingsFile() {
            this.settingsFile = getProjectDirectory().getAsFile().get().toPath().resolve("settings.gradle");
        }

        public void write() throws IOException {
            Files.createDirectories(settingsFile.getParent());
            Files.write(settingsFile, Arrays.asList(
                    "plugins {",
                    "\tid 'dev.nokee.sample'",
                    "}"
            ));
        }
    }

    private final class BuildFile implements Writable {
        private final Path buildFile;

        private BuildFile() {
            this.buildFile = getProjectDirectory().getAsFile().get().toPath().resolve("build.gradle");
        }

        @Override
        public void write() throws IOException {
            Files.createDirectories(buildFile.getParent());
            Files.createFile(buildFile);
        }
    }

    private final class GradleWrapperFiles implements Writable {
        private final Path projectDirectory;
        private final int dirUp;

        private GradleWrapperFiles(Path projectDirectory) {
            this.projectDirectory = projectDirectory;
            this.dirUp = StringUtils.countMatches(getProjectPath().get(), "/") + 1;
            Path baseDir = projectDirectory.resolve(StringUtils.repeat("../", dirUp));
            if (!Files.exists(baseDir.resolve("gradlew")) || !Files.exists(baseDir.resolve("gradlew.bat"))) {
                throw new UnsupportedOperationException("gradlew[.bat] does not exists");
            }
        }

        @Override
        public void write() throws IOException {
            Files.createDirectories(projectDirectory);
            Files.write(projectDirectory.resolve("gradlew"), Arrays.asList(
                    "#!/usr/bin/env sh",
                    "",
                    "exec \"$(dirname \"$0\")/" + StringUtils.repeat("../", dirUp) + "gradlew\" \"$@\"",
                    ""
            ));
            Files.setPosixFilePermissions(projectDirectory.resolve("gradlew"), PosixFilePermissions.fromString("rwxr-xr-x"));
            Files.write(projectDirectory.resolve("gradlew.bat"), Arrays.asList(
                    "@echo off",
                    "call " + StringUtils.repeat("..\\", dirUp) + "gradlew.bat %*",
                    ""
            ));
        }
    }

    private final class ReadmeFile implements Writable {
        private final Path projectDirectory;

        private ReadmeFile(Path projectDirectory) {
            this.projectDirectory = projectDirectory;
        }

        @Override
        public void write() throws IOException {
            Files.createDirectories(projectDirectory);
            Files.write(projectDirectory.resolve("README.md"), Arrays.asList(
                    "# SAMPLE-DISPLAY-NAME (" + projectDirectory.getFileName() + ")",
                    ""
            ));
        }
    }

    private final class SampleEntry implements Writable {
        public SampleEntry() {
            if (!Files.exists(getSettingsFile().get().getAsFile().toPath())) {
                throw new UnsupportedOperationException("root settings.gradle does not exists");
            }
        }

        @Override
        public void write() throws IOException {
            Files.write(getSettingsFile().get().getAsFile().toPath(), Arrays.asList(
                    "samples {",
                    "\tinclude '" + getProjectPath().get().replace('\\', '/') + "'",
                    "}",
                    ""
            ), StandardOpenOption.APPEND);
        }
    }
}
