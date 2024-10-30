package org.gradle.samples.plugins.generators;

import groovy.json.JsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.component.SoftwareComponentFactory;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.internal.component.external.model.ProjectDerivedCapability;
import org.gradle.samples.plugins.generators.readme.ReadMeExtension;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;

public class GeneratorPlugin implements Plugin<Project> {
    private final SoftwareComponentFactory softwareComponentFactory;

    @Inject
    public GeneratorPlugin(SoftwareComponentFactory softwareComponentFactory) {
        this.softwareComponentFactory = softwareComponentFactory;
    }

    public void apply(Project project) {
        TaskCollection<GitRepoTask> repoTasks = project.getTasks().withType(GitRepoTask.class);

        // Add project extension
        SamplesExtension extension = project.getExtensions().create("samples", SamplesExtension.class, project, (NamedDomainObjectFactory<Sample>) name -> {
            return project.getObjects().newInstance(Sample.class, name, project.getTasks().register(name + "Manifest", WriteSampleManifestTask.class), project.getTasks().register("sync" + name + "Sample", Sync.class));
        });

        // Add a task to generate the list of samples
        TaskProvider<SamplesManifestTask> manifestTask = project.getTasks().register("samplesManifest", SamplesManifestTask.class, task -> {
            task.getManifest().set(project.file("samples-list.txt"));
            task.getRepoDirs().set(project.provider(() -> {
                return repoTasks.stream().map(generator -> {
                    return generator.getSampleDir().get().getAsFile().getAbsolutePath();
                }).collect(Collectors.toList());
            }));
        });

        // Add a task to clean the samples
        project.getTasks().register("cleanSamples", CleanSamplesTask.class, task -> {
            // Need the location without the task dependency as we want to clean whatever was generated last time, not whatever will be generated next time
            task.getManifest().set(project.provider(() -> {
                return manifestTask.get().getManifest().get();
            }));
        });

        // Add a lifecycle task to generate the source files for the samples
        TaskProvider<Task> generateSource = project.getTasks().register("generateSource", task -> {
            task.dependsOn(manifestTask);
            task.setGroup("source generation");
            task.setDescription("generate the source files for all samples");
        });

        extension.getExternalRepos().all(it -> {
            addTasksForRepo(it, generateSource, project);
        });

        project.getPluginManager().apply("dev.nokee.samples.readme");

        //region Summary/Manifest
        // TODO: This should be modeled as summary which adds to the manifest (this is a different capability)
        //   Summary:
        //    - title
        //    - description
        //    - version
        //    - author
        //    - tags
        extension.getSamples().configureEach(sample -> {
            sample.getTitle().convention(project.provider(() -> sample.getExtensions().findByType(ReadMeExtension.class)).flatMap(ReadMeExtension::getLocation).map(it -> {
                try {
                    return Files.readAllLines(it.getAsFile().toPath()).stream().map(String::trim).filter(s -> s.startsWith("# ")).findFirst().map(s -> s.substring(2)).orElse(null);
                } catch (IOException e) {
                    return null;
                }
            }));

            sample.getManifestTask().configure(t -> {
                WriteSampleManifestTask task = (WriteSampleManifestTask) t;
                task.getElements().put("title", sample.getTitle());
                task.getElements().put("name", sample.getName());
                task.getElements().put("variants", project.provider(() -> Arrays.asList(project.getTasks().named("zip" + sample.getName(), Zip.class).get().getArchiveFileName().get())));
                task.getOutputFile().fileProvider(project.provider(task.getTemporaryDirFactory()::create).map(it -> new File(it, "manifest.json")));
            });

            sample.content(spec -> spec.from(sample.getManifestTask()));
        });
        //endregion

        // TODO: OG meta
        // TODO: HTML meta
        // TODO: Twitter meta

        //region Export all samples as outgoing elements
        AdhocComponentWithVariants allSamplesComponent = softwareComponentFactory.adhoc("samples");

        NamedDomainObjectProvider<Configuration> sampleBucket = project.getConfigurations().register("sample");
        sampleBucket.configure(config -> {
            config.setCanBeConsumed(false);
            config.setCanBeResolved(false);
        });

        NamedDomainObjectProvider<Configuration> sampleElements = project.getConfigurations().register("sampleElements");
        sampleElements.configure(config -> {
            config.extendsFrom(sampleBucket.get());
            config.setCanBeResolved(false);
            config.setCanBeConsumed(true);
            config.attributes(attributes -> {
                attributes.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, "sample"));
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, "documentation"));
            });
        });

        allSamplesComponent.addVariantsFromConfiguration(sampleElements.get(), __ -> {});

        extension.getSamples().all(sample -> {
            TaskProvider<Zip> zipTask = project.getTasks().register("zip" + sample.getName(), Zip.class);
            zipTask.configure(task -> {
                // TODO: depends on respective generateSource && generateRepos
                task.from(sample.getSampleDir());

                task.exclude("**/.build/**", "**/.gradle/**", "**/build/**");
                task.exclude("**/*.xcodeproj", "**/*.xcworkspace");
                task.exclude("**/.vs/**", "**/*.sln", "**/*.vcxproj", "**/*.vcxproj.filters", "**/*.vcxproj.user");

                task.getArchiveBaseName().set(sample.getName());
                task.getArchiveVersion().set(project.getVersion().toString());
                task.getArchiveClassifier().set("groovy-dsl");
            });

            //region Export sample content
            sample.content(spec -> {
                spec.from(zipTask);
            });

            sample.getContentTask().configure(task -> {
                task.setDestinationDir(project.file(project.provider(task.getTemporaryDirFactory()::create).map(it -> new File(it, "out"))));
            });

            TaskProvider<Zip> zipSampleTask = project.getTasks().register("zip" + sample.getName() + "Sample", Zip.class);
            zipSampleTask.configure(task -> {
                task.from(sample.getContentTask());
                task.getArchiveBaseName().set(sample.getName());
                task.getArchiveVersion().set(project.getVersion().toString());
                task.getArchiveExtension().set("sample");
            });

            NamedDomainObjectProvider<Configuration> configuration = project.getConfigurations().register(sample.getName() + "SampleElements");
            configuration.configure(config -> {
                config.setCanBeResolved(false);
                config.setCanBeConsumed(true);
                config.outgoing(outgoing -> {
                    outgoing.capability(new ProjectDerivedCapability(project, sample.getName()));
                    outgoing.artifact(zipSampleTask);
                    outgoing.variants(variants -> {
                        variants.create("sample-directory", it -> {
                            it.artifact(sample.getContentTask(), spec -> spec.setType("sample-directory"));
                        });
                    });
                });
                config.attributes(attributes -> {
                    attributes.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, "sample"));
                    attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, "documentation"));
                });
            });
            allSamplesComponent.addVariantsFromConfiguration(configuration.get(), __ -> {});
            sampleBucket.configure(config -> {
                ModuleDependency dependency = (ModuleDependency) project.getDependencies().create(project);
                dependency.capabilities(cc -> {
                    cc.requireCapability(new ProjectDerivedCapability(project, sample.getName()));
                });
                config.getDependencies().add(dependency);
            });
            //endregion
        });

        project.getComponents().add(allSamplesComponent);
        //endregion

        // Add a lifecycle task to generate the repositories
        project.getTasks().register("generateRepos", task -> {
            task.dependsOn(repoTasks);
            task.setGroup("source generation");
            task.setDescription("generate the Git repositories for all samples");
        });
    }

    private void addTasksForRepo(ExternalRepo repo, TaskProvider<Task> generateSource, Project project) {
        TaskProvider<SyncExternalRepoTask> syncTask = project.getTasks().register("sync" + StringUtils.capitalize(repo.getName()), SyncExternalRepoTask.class, task -> {
            task.getRepoUrl().set(repo.getRepoUrl());
            task.getCheckoutDirectory().set(project.file("repos/" + repo.getName()));
        });
        TaskProvider<UpdateRepoTask> updateTask = project.getTasks().register("update" + StringUtils.capitalize(repo.getName()), UpdateRepoTask.class, task -> {
            task.getSampleDir().set(syncTask.get().getCheckoutDirectory());
            repo.getRepoActions().forEach(it -> {
                task.change(it);
            });
        });
        generateSource.configure(task -> {
            task.dependsOn(updateTask);
        });
    }

    /*private*/ static abstract /*final*/ class WriteSampleManifestTask extends DefaultTask {
        @Inject
        public WriteSampleManifestTask() {}

        @Input
        public abstract MapProperty<String, Object> getElements();

        @OutputFile
        public abstract RegularFileProperty getOutputFile();

        @TaskAction
        private void doWrite() throws IOException {
            try (PrintWriter out = new PrintWriter(getOutputFile().get().getAsFile())) {
                new JsonBuilder(getElements().get()).writeTo(out);
            }
        }
    }
}
