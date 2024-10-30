package org.gradle.samples.plugins.generators.idea;

import com.google.common.base.CaseFormat;
import dev.nokee.samples.externalbuilds.ExternalBuildTask;
import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.component.external.model.ProjectDerivedCapability;
import org.gradle.samples.plugins.generators.SamplesExtension;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public abstract /*final*/ class IdeaPlugin implements Plugin<Project> {
    public void apply(Project project) {
        //region IDEA extension
        project.getPluginManager().withPlugin("org.gradle.samples.generators", __ -> {
            project.getExtensions().getByType(SamplesExtension.class).getSamples().configureEach(sample -> {
                IdeaExtension extension = sample.getExtensions().create("idea", IdeaExtension.class);
                extension.getName().convention(sample.getName());
                extension.getExternalProjectLocation().convention(sample.getSampleDir());
            });
        });
        //endregion

        //region External Project
        project.getPluginManager().withPlugin("org.gradle.samples.generators", __ -> {
            if (isIdeaSyncActive()) {
                NamedDomainObjectProvider<Configuration> ideaElements = project.getConfigurations().register("ideaElements");
                ideaElements.configure(it -> {
                    it.setCanBeConsumed(true);
                    it.setCanBeResolved(false);
                    it.extendsFrom(project.getConfigurations().getByName("sample"));
                    it.attributes(attributes -> {
                        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, "sampleidea"));
                    });
                });
            }

            RegularFileProperty l = project.getObjects().fileProperty().value(project.getLayout().getBuildDirectory().file("model.init.gradle").zip(project.provider(() -> {
                return new Scanner(IdeaPlugin.class.getResourceAsStream("/model.init.gradle")).useDelimiter("\\A").next();
            }), (a, b) -> {
                try {
                    Files.write(a.getAsFile().toPath(), b.getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return a;
            }));
            l.finalizeValueOnRead();

            project.getPluginManager().apply("dev.nokee.samples.external-builds-base");

            project.getExtensions().getByType(SamplesExtension.class).getSamples().configureEach(sample -> {
                IdeaExtension extension = sample.getExtensions().getByType(IdeaExtension.class);


                TaskProvider<? extends ExternalBuildTask> buildTask = project.getTasks().register(sample.getName() + "ExternalGradleProjectSettings", ExternalBuildTask.implementationType(), task -> {
                    // TODO: Only if sourceCopy template plugin exists
                    task.dependsOn(project.getTasks().named(CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, sample.getName())));
                    task.getProjectDirectory().set(extension.getExternalProjectLocation());
                    task.getArguments().addAll("--init-script");
                    task.getArguments().add(l.map(it -> it.getAsFile().getAbsolutePath()));
                    task.getArguments().addAll("-PoutFile=" + new File(task.getTemporaryDir(), "gradle.json"));
                    task.getTaskPath().set("generate");
                });

                if (isIdeaSyncActive()) {
                    project.getConfigurations().register(sample.getName() + "IdeaExternalElements", config -> {
                        config.setCanBeConsumed(true);
                        config.setCanBeResolved(false);
                        config.attributes(attributes -> {
                            attributes.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, "sampleideagradle"));
                        });
                        config.outgoing(outgoing -> {
                            outgoing.capability(new ProjectDerivedCapability(project, sample.getName()));
                            outgoing.artifact(buildTask.map(it -> new File(it.getTemporaryDir(), "gradle.json")));
                        });
                    });
                }
            });
        });

        if (project.getParent() == null) {
            project.getTasks().configureEach(it -> {
                if (it.getName().equals("prepareKotlinBuildScriptModel")) {
                    it.finalizedBy((Callable<?>) () -> {
                        return project.getTasks().register("prepareExternalGradleProjectSettings", MyTask.class, task -> {
                            task.getInputFiles().from(new Callable<Object>() {
                                private Task value = null;

                                @Override
                                public Object call() throws Exception {
                                    if (value == null) {
                                        TaskProvider<ComputeExternal> task = project.getTasks().register("mainproject", ComputeExternal.class, t -> {
                                            t.getOutputFile().fileProvider(project.provider(t.getTemporaryDirFactory()::create).map(f -> new File(f, "gradle.json")));
                                        });
                                        value = task.get();
                                    }
                                    return value;
                                }
                            });
                            task.getInputFiles().from(new Callable<Object>() {
                                private Configuration value;

                                @Override
                                public Object call() throws Exception {
                                    if (value == null) {
                                        NamedDomainObjectProvider<Configuration> config = project.getConfigurations().register("sampleideagradle");
                                        config.configure(c -> {
                                            c.setCanBeResolved(true);
                                            c.setCanBeConsumed(false);
                                            c.extendsFrom(project.getConfigurations().getByName("sample"));
                                            c.attributes(attributes -> {
                                                attributes.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, "sampleideagradle"));
                                            });
                                        });
                                        value = config.get();
                                    }
                                    return value;
                                }
                            });
                            task.getProjectDir().fileProvider(project.provider(() -> {
                                Path result = project.getLayout().getProjectDirectory().getAsFile().toPath();
                                while (result != null && !Files.exists(result.resolve(".idea"))) {
                                    result = result.getParent();
                                }
                                return Objects.requireNonNull(result).toFile();
                            }));
                            task.getGradleXmlFile().set(task.getProjectDir().file(".idea/gradle.xml"));
                        });
                    });
                }
            });
        }
        //endregion


        //region Override external project name
        project.getPluginManager().withPlugin("org.gradle.samples.generators", __ -> {
            if (isIdeaSyncActive()) {
                NamedDomainObjectProvider<Configuration> ideaGradleElements = project.getConfigurations().register("ideaGradleElements");
                ideaGradleElements.configure(it -> {
                    it.setCanBeConsumed(true);
                    it.setCanBeResolved(false);
                    it.extendsFrom(project.getConfigurations().getByName("sample"));
                    it.attributes(attributes -> {
                        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, "sampleideagradle"));
                    });
                });

                project.getExtensions().getByType(SamplesExtension.class).getSamples().configureEach(sample -> {
                    IdeaExtension extension = sample.getExtensions().getByType(IdeaExtension.class);

                    project.getConfigurations().register(sample.getName() + "IdeaElements", config -> {
                        config.setCanBeConsumed(true);
                        config.setCanBeResolved(false);
                        config.attributes(attributes -> {
                            attributes.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, "sampleidea"));
                        });
                        config.outgoing(outgoing -> {
                            outgoing.capability(new ProjectDerivedCapability(project, sample.getName()));
                            outgoing.artifact(project.getTasks().register(sample.getName() + "IdeaManifest", WriteIdeaManifestTask.class, task -> {
                                task.getProjectName().set(extension.getName());
                                task.getProjectDir().set(extension.getExternalProjectLocation());
                                task.getOutputFile().fileProvider(project.provider(task.getTemporaryDirFactory()::create).map(it -> new File(it, "manifest.json")));
                            }));
                        });
                    });
                });
            }
        });

        if (project.getParent() == null) {
            project.getTasks().configureEach(it -> {
                if (it.getName().equals("prepareKotlinBuildScriptModel")) {
                    it.finalizedBy((Callable<?>) () -> {
                        return project.getTasks().register("overrideIdeaSampleProjectName", WriteOverrideInitScriptTask.class, task -> {
                            task.getOutputFile().set(new File(project.getGradle().getGradleUserHomeDir(), "init.d/idea-sync-init.gradle"));
                        });
                    });
                    it.finalizedBy((Callable<?>) () -> {
                        return project.getTasks().register("writeSampleRoot", WriteSampleRootTask.class, task -> {
                            task.doLast(new Action<Task>() {
                                public void execute(Task t) {
                                    final Path gitIgnoreFile = project.file(".gitignore").toPath();
                                    if (Files.exists(gitIgnoreFile)) {
                                        try {
                                            if (Files.readAllLines(gitIgnoreFile).stream().map(String::trim).noneMatch(it -> it.equals(".sampleroot"))) {
                                                t.getLogger().warn("Add ignore to .gitignore for '.sampleroot'.");
                                            }
                                        } catch (IOException e) {
                                            throw new UncheckedIOException(e);
                                        }
                                    }
                                }
                            });
                            task.getInputFiles().from(new Callable<Object>() {
                                private Configuration value = null;

                                @Override
                                public Object call() throws Exception {
                                    if (value == null) {
                                        NamedDomainObjectProvider<Configuration> config = project.getConfigurations().register("sampleidea");
                                        config.configure(c -> {
                                            c.setCanBeResolved(true);
                                            c.setCanBeConsumed(false);
                                            c.extendsFrom(project.getConfigurations().getByName("sample"));
                                            c.attributes(attributes -> {
                                                attributes.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, "sampleidea"));
                                            });
                                        });
                                        value = config.get();
                                    }
                                    return value;
                                }
                            });
                            task.getOutputFile().set(project.file(".sampleroot"));
                        });
                    });
                }
            });
        }
        //endregion
    }

    private boolean isIdeaSyncActive() {
        String value = System.getProperty("idea.sync.active");
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    public static abstract class IdeaExtension {
        public abstract Property<String> getName();
        public abstract DirectoryProperty getExternalProjectLocation();
    }

    public static abstract class MyTask extends DefaultTask {
        @InputFiles
        public abstract ConfigurableFileCollection getInputFiles();

        @OutputFile
        public abstract RegularFileProperty getGradleXmlFile();

        @Internal
        public abstract DirectoryProperty getProjectDir();

        @Input
        protected Provider<String> getProjectDirLocationOnly() {
            return getProjectDir().map(it -> it.getAsFile().getAbsolutePath());
        }

        @Inject
        public MyTask() {}

        @TaskAction
        private void doAction() throws IOException {
            // Assume no-op IF parent directory doesn't exists (aka gradle.xml)
            if (!Optional.ofNullable(getGradleXmlFile().getOrNull()).map(it -> it.getAsFile().getParentFile().exists()).orElse(false)) {
                this.setDidWork(false);
            }

            // NOTE: Parent directories exists because of assumption.

            try (PrintWriter out = new PrintWriter(getGradleXmlFile().get().getAsFile())) {
                out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                out.println("<project version=\"4\">");
                out.println("  <component name=\"GradleSettings\">");
                out.println("    <option name=\"linkedExternalProjectsSettings\">");
                for (File inputFile : getInputFiles().getAsFileTree()) { // use FileTree to ignore missing files
                    Map value = (Map) new JsonSlurper().parse(inputFile);

                    out.println("    <GradleProjectSettings>");
                    out.println("      <compositeConfiguration>");
                    out.println("        <compositeBuild compositeDefinitionSource=\"SCRIPT\">");
                    out.println("          <builds>");
                    ((List<Map>) value.get("compositeBuilds")).forEach((Map it) -> {
                        Path path = getProjectDir().get().getAsFile().toPath().relativize(Paths.get(it.get("location").toString()));
                        out.println("            <build path=\"$PROJECT_DIR$/" + path + "\" name=\"" + it.get("name") + "\">");
                        out.println("              <projects>");
                        out.println("                <project path=\"$PROJECT_DIR$/" + path + "\" />");
                        out.println("              </projects>");
                        out.println("            </build>");
                    });
                    out.println("          </builds>");
                    out.println("        </compositeBuild>");
                    out.println("      </compositeConfiguration>");

                    Path projectPath = getProjectDir().get().getAsFile().toPath().relativize(Paths.get(value.get("location").toString()));
                    out.println("      <option name=\"externalProjectPath\" value=\"$PROJECT_DIR$/" + projectPath + "\" />");
                    out.println("      <option name=\"gradleJvm\" value=\"11\" />");
//                    out.println("      <option name=\"gradleJvm\" value=\"#JAVA_HOME\" />");
                    out.println("      <option name=\"modules\">");
                    out.println("        <set>");
                    // TODO: Only if can't load modules
//                    out.println("          <option value=\"$PROJECT_DIR$/" + projectPath + "\" />");
                    ((List<String>) value.get("modules")).forEach(it -> {
                        Path path = getProjectDir().get().getAsFile().toPath().relativize(Paths.get(it.toString()));
                        out.println("          <option value=\"$PROJECT_DIR$/" + path + "\" />");
                    });
                    out.println("        </set>");
                    out.println("      </option>");
                    out.println("    </GradleProjectSettings>");
                }
                out.println("    </option>");
                out.println("  </component>");
                out.println("</project>");
            }
        }
    }

    /*private*/ static abstract /*final*/ class WriteOverrideInitScriptTask extends DefaultTask {
        @Inject
        public WriteOverrideInitScriptTask() {}

        @OutputFile
        public abstract RegularFileProperty getOutputFile();

        @TaskAction
        private void doWrite() throws IOException {
            Files.createDirectories(getOutputFile().getAsFile().get().toPath().getParent());
            Files.copy(WriteOverrideInitScriptTask.class.getResourceAsStream("/idea-sync.init.gradle"), getOutputFile().getAsFile().get().toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /*private*/ static abstract /*final*/ class WriteSampleRootTask extends DefaultTask {
        @Inject
        public WriteSampleRootTask() {}

        @InputFiles
        public abstract ConfigurableFileCollection getInputFiles();

        @OutputFile
        public abstract RegularFileProperty getOutputFile();

        @TaskAction
        private void doWrite() throws IOException {
            Map<String, String> result = new LinkedHashMap<>();
            for (File file : getInputFiles()) {
                Map values = (Map) new JsonSlurper().parse(file);
                result.put(getProject().relativePath(values.get("path")), values.get("name").toString());
            }

            try (PrintWriter out = new PrintWriter(getOutputFile().get().getAsFile())) {
                new JsonBuilder(result).writeTo(out);
            }
        }
    }

    /*private*/ static abstract /*final*/ class WriteIdeaManifestTask extends DefaultTask {
        @Inject
        public WriteIdeaManifestTask() {}

        @Internal
        public abstract DirectoryProperty getProjectDir();

        @Input
        protected Provider<String> getProjectDirLocationOnly() {
            return getProjectDir().map(it -> it.getAsFile().getAbsolutePath());
        }

        @Input
        public abstract Property<String> getProjectName();

        @OutputFile
        public abstract RegularFileProperty getOutputFile();

        @TaskAction
        private void doWrite() throws IOException {
            try (PrintWriter out = new PrintWriter(getOutputFile().get().getAsFile())) {
                out.println("{");
                out.println("  \"name\": \"" + getProjectName().get() + "\",");
                out.println("  \"path\": \"" + getProjectDir().get().getAsFile().getAbsolutePath() + "\"");
                out.println("}");
            }
        }
    }

    /*private*/ static abstract /*final*/ class ComputeExternal extends DefaultTask {
        @Inject
        public ComputeExternal() {
            getOutputs().upToDateWhen(__ -> false);
        }

        @OutputFile
        public abstract RegularFileProperty getOutputFile();

        @TaskAction
        private void doWrite() throws IOException {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("location", getProject().getRootDir().getAbsolutePath());
            result.put("compositeBuilds", getProject().getGradle().getIncludedBuilds().stream().map(it -> {
                Map<String, String> r = new LinkedHashMap<>();
                r.put("name", it.getName());
                r.put("location", it.getProjectDir().getAbsolutePath());
                return r;
            }).collect(Collectors.toList()));
            result.put("modules", getProject().getAllprojects().stream().map(it -> it.getProjectDir().getAbsolutePath()).collect(Collectors.toList()));

            try (PrintWriter out = new PrintWriter(getOutputFile().getAsFile().get())) {
                new JsonBuilder(result).writeTo(out);
            }
        }
    }
}
