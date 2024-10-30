package org.gradle.samples.plugins.generators;

import org.apache.commons.lang3.StringUtils;
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
import org.gradle.api.file.CopySpec;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.internal.component.external.model.ProjectDerivedCapability;
import org.gradle.samples.plugins.generators.manifest.ManifestExtension;

import javax.inject.Inject;
import java.io.File;
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
        SamplesExtension extension = project.getExtensions().create("samples", SamplesExtension.class);

        extension.getSamples().all(sample -> {
            TaskProvider<Sync> contentTask = project.getTasks().register("sync" + sample.getName() + "Sample", Sync.class);
            contentTask.configure(task -> {
                task.setDestinationDir(project.file(project.provider(task.getTemporaryDirFactory()::create).map(it -> new File(it, "out"))));
            });
            sample.getExtensions().add(CopySpec.class, "content", contentTask.get());
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
        project.getPluginManager().apply("dev.nokee.samples.manifest");
        project.getPluginManager().apply("dev.nokee.samples.summary");

        extension.getSamples().configureEach(sample -> {
            sample.getExtensions().configure(ManifestExtension.class, manifest -> {
                manifest.put("variants", project.provider(() -> Arrays.asList(project.getTasks().named("zip" + sample.getName(), Zip.class).get().getArchiveFileName().get())));
            });
        });

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
            sample.getExtensions().configure(CopySpec.class, spec -> {
                spec.from(zipTask);
            });

            TaskProvider<Zip> zipSampleTask = project.getTasks().register("zip" + sample.getName() + "Sample", Zip.class);
            zipSampleTask.configure(task -> {
                task.from(project.getTasks().named("sync" + sample.getName() + "Sample"));
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
                            it.artifact(project.getTasks().named("sync" + sample.getName() + "Sample"), spec -> spec.setType("sample-directory"));
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
}
