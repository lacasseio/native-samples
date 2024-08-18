package dev.nokee.samples.externalbuilds.internal;

import dev.nokee.samples.externalbuilds.internal.model.BuildModules;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Transformer;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/*private*/ abstract /*final*/ class IdeaLinkedExternalGradleProjectRule implements Plugin<Project> {
    private final ProviderFactory providers;

    @Inject
    public IdeaLinkedExternalGradleProjectRule(ProviderFactory providers) {
        this.providers = providers;
    }

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("samplesdev.rules.idea-external-builds-model");

        project.getTasks().configureEach(task -> {
            if (task.getName().equals("prepareKotlinBuildScriptModel")) {
                task.dependsOn((Callable<?>) () -> {
                    if (project.file(".idea").exists()) {
//                        Provider<BuildModules> modules = CrossProjectExtension.forProject(project).externalBuild(project.getRootProject().file("cpp/composite-build")).model(BuildModules.class).asProvider();
//                        for (ExternalBuild it : externalBuildsOf(project)) {
//                            CrossProjectExtension.ExternalBuildReference externalBuild = CrossProjectExtension.forProject(project).externalBuild(it.getLocation());
//                            externalBuild.model(BuildModules.class).asProvider()
//                        }



//                                .map(it -> CrossProjectExtension.forProject(project).externalBuild(it.location))
//                                .map(it -> it.model(ExternalBuilds.class)).forEach(it -> {});
                        MyTask result = project.getTasks().maybeCreate("ideaExternalGradleProjects", MyTask.class);
                        for (dev.nokee.samples.externalbuilds.ExternalBuild externalBuild : externalBuildsOf(project)) {
                            result.getExternalBuilds().add(externalBuild.model(BuildModules.class).asProvider().map(t -> {
                                return new ExternalBuild(externalBuild.getProjectDirectory().get().getAsFile().toPath(), t.get());
                            }));
                        }
//                        result.getExternalBuilds().set(allExternalBuilds(externalBuildsOf(project)));
                        result.getExternalBuilds().finalizeValueOnRead();

                        result.getGradleXmlFile().set(project.getLayout().getProjectDirectory().file(".idea/gradle.xml"));
                        result.getGradleXmlFile().finalizeValueOnRead();

                        return result;
                    }
                    return Collections.emptyList();
                });
            }
        });
    }

    public Iterable<dev.nokee.samples.externalbuilds.ExternalBuild> externalBuildsOf(Project project) {
        return Optional.ofNullable(project.getExtensions().findByType(ExternalBuildsInternal.class)).map(it -> (Iterable<dev.nokee.samples.externalbuilds.ExternalBuild>) it).orElse(Collections.emptyList());
    }

    private static <OUT, IN> Transformer<Iterable<OUT>, Iterable<IN>> transformEach(Transformer<OUT, IN> mapper) {
        return it -> {
            return StreamSupport.stream(it.spliterator(), false).map(mapper::transform).collect(Collectors.toList());
        };
    }

//    public Provider<Set<ExternalBuild>> allExternalBuilds(Provider<Set<ExternalBuild>> externalBuilds) {
//        return externalBuilds.map(it -> {
//                    final Set<ExternalBuild> result = new LinkedHashSet<>();
//                    final Deque<ExternalBuild> queue = new ArrayDeque<>(it);
//                    while (!queue.isEmpty()) {
//                        final ExternalBuild build = queue.pop();
//                        if (result.add(build)) {
//                            try {
//                                build.loadExternalBuilds().forEach(queue::add);
//                            } catch (Throwable ex) {
//                                System.out.println("Could not load build '" + build.getLocation() + "'");
//                            }
//                        }
//                    }
//
//                    return result;
//                });
//    }

    public static abstract class MyTask extends DefaultTask {
        @Internal
        public abstract SetProperty<ExternalBuild> getExternalBuilds();

        @Internal
        public abstract RegularFileProperty getGradleXmlFile();

        @Inject
        public MyTask() {
            dependsOn(getExternalBuilds());
        }

        @TaskAction
        private void doAction() throws IOException {
            // Assume no-op IF parent directory doesn't exists (aka gradle.xml)
            if (!getGradleXmlFile().getAsFile().map(it -> it.getParentFile().exists()).getOrElse(false)) {
                this.setDidWork(false);
            }

            // NOTE: Parent directories exists because of assumption.

            try (PrintWriter out = new PrintWriter(getGradleXmlFile().get().getAsFile())) {
                out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                out.println("<project version=\"4\">");
                out.println("  <component name=\"GradleSettings\">");
                out.println("    <option name=\"linkedExternalProjectsSettings\">");
                getExternalBuilds().get().forEach(build -> {
                    out.println("    <GradleProjectSettings>");
                    out.println("      <compositeConfiguration>");
                    out.println("        <compositeBuild compositeDefinitionSource=\"SCRIPT\">");
                    out.println("          <builds>");
                    StreamSupport.stream(build.getIncludedBuildModule().spliterator(), false).forEach(it -> {
                        Path path = getProject().getProjectDir().toPath().relativize(it.getLocation());
                        out.println("            <build path=\"$PROJECT_DIR$/" + path + "\" name=\"" + it.getName() + "\">");
                        out.println("              <projects>");
                        out.println("                <project path=\"$PROJECT_DIR$/" + path + "\" />");
                        out.println("              </projects>");
                        out.println("            </build>");
                    });
                    out.println("          </builds>");
                    out.println("        </compositeBuild>");
                    out.println("      </compositeConfiguration>");


                    Path projectPath = getProject().getProjectDir().toPath().relativize(build.getLocation());
                    out.println("      <option name=\"externalProjectPath\" value=\"$PROJECT_DIR$/" + projectPath + "\" />");
                    out.println("      <option name=\"gradleJvm\" value=\"#JAVA_HOME\" />");
                    out.println("      <option name=\"modules\">");
                    out.println("        <set>");
                    // TODO: Only if can't load modules
//                    out.println("          <option value=\"$PROJECT_DIR$/" + projectPath + "\" />");
                    StreamSupport.stream(build.getModules().spliterator(), false).forEach(it -> {
                        Path path = getProject().getProjectDir().toPath().relativize(it.getLocation());
                        out.println("          <option value=\"$PROJECT_DIR$/" + path + "\" />");
                    });
                    out.println("        </set>");
                    out.println("      </option>");
                    out.println("    </GradleProjectSettings>");
                });
                out.println("    </option>");
                out.println("  </component>");
                out.println("</project>");
            }
        }
    }

    public static final class ExternalBuild implements Serializable {
        public final File location;
        private transient Iterable<BuildModules.Module> cachedModules;

        public ExternalBuild(Path location) {
            this.location = location.toFile();
        }

        public ExternalBuild(Path location, Iterable<BuildModules.Module> modules) {
            this.location = location.toFile();
            this.cachedModules = modules;
        }

        public Path getLocation() {
            return location.toPath();
        }

        public Iterable<BuildModules.Module> getModules() {
            if (cachedModules == null) {
                cachedModules = loadModules();
            }
            return cachedModules;
        }

        public Iterable<BuildModules.Module.IncludedBuild> getIncludedBuildModule() {
            List<BuildModules.Module.IncludedBuild> result = new ArrayList<>();
            for (BuildModules.Module module : getModules()) {
                if (module instanceof BuildModules.Module.IncludedBuild) {
                    result.add((BuildModules.Module.IncludedBuild) module);
                }
            }
            return result;
        }

        private Iterable<BuildModules.Module> loadModules() {
            try {
                final GradleConnector connector = GradleConnector.newConnector();
                connector.forProjectDirectory(location);

                try (ProjectConnection connection = connector.connect()) {
                    final ModelBuilder<BuildModules> modelBuilder = connection.model(BuildModules.class);
                    final BuildModules customModel = modelBuilder.get();
                    return customModel.get();
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
                System.out.println("Can't load modules for '" + location + "'");
            }
            return Collections.emptyList();
        }

//        public Iterable<ExternalBuild> loadExternalBuilds() {
//            final GradleConnector connector = GradleConnector.newConnector();
//            connector.forProjectDirectory(location);
//
//            try (ProjectConnection connection = connector.connect()) {
//                final ModelBuilder<ExternalBuilds> modelBuilder = connection.model(ExternalBuilds.class);
//                final ExternalBuilds customModel = modelBuilder.get();
//                return StreamSupport.stream(customModel.get().spliterator(), false).map(it -> new ExternalBuild(it.get())).collect(Collectors.toList());
//            }
//        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ExternalBuild that = (ExternalBuild) o;
            return Objects.equals(location, that.location);
        }

        @Override
        public int hashCode() {
            return Objects.hash(location);
        }
    }
}
