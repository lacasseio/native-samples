package org.gradle.samples.plugins.generators;

import com.google.common.base.CaseFormat;
import groovy.json.JsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.attributes.Usage;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.component.SoftwareComponentFactory;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.internal.component.external.model.ProjectDerivedCapability;
import org.gradle.process.ExecOperations;
import org.gradle.samples.plugins.SampleGeneratorTask;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.stream.Collectors;

public class GeneratorPlugin implements Plugin<Project> {
    private final SoftwareComponentFactory softwareComponentFactory;

    @Inject
    public GeneratorPlugin(SoftwareComponentFactory softwareComponentFactory) {
        this.softwareComponentFactory = softwareComponentFactory;
    }


    public void apply(Project project) {
        TaskCollection<SampleGeneratorTask> generatorTasks = project.getTasks().withType(SampleGeneratorTask.class);
        TaskCollection<GitRepoTask> repoTasks = project.getTasks().withType(GitRepoTask.class);

        // Add project extension
        SamplesExtension extension = project.getExtensions().create("samples", SamplesExtension.class, project);

        // Add a task to generate the list of samples
        TaskProvider<SamplesManifestTask> manifestTask = project.getTasks().register("samplesManifest", SamplesManifestTask.class, task -> {
            task.getManifest().set(project.file("samples-list.txt"));
            task.getSampleDirs().set(project.provider(() -> {
                return generatorTasks.stream().map(generator -> {
                    return generator.getSampleDir().get().getAsFile().getAbsolutePath();
                }).collect(Collectors.toList());
            }));
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

        // Apply conventions to the generator tasks
        generatorTasks.configureEach( task -> {
            task.getTemplatesDir().set(project.file("src/templates"));
        });

        // Add a lifecycle task to generate the source files for the samples
        TaskProvider<Task> generateSource = project.getTasks().register("generateSource", task -> {
            task.dependsOn(generatorTasks);
            task.dependsOn(manifestTask);
            task.setGroup("source generation");
            task.setDescription("generate the source files for all samples");
        });

        extension.getExternalRepos().all(it -> {
            addTasksForRepo(it, generateSource, project);
        });

        extension.getSamples().all(it -> {
            addTasksForSample(it, project);
        });

        //region README
        extension.getSamples().configureEach(sample -> {
            ReadMeExtension readme = sample.getExtensions().create("readme", ReadMeExtension.class);
            readme.getLocation().convention(sample.getSampleDir().map(it -> {
                if (it.file("README.md").getAsFile().exists()) {
                    return it.file("README.md");
                } else if (it.file("README.adoc").getAsFile().exists()) {
                    return it.file("README.adoc");
                }
                return null;
            }));

            project.getTasks().register(RenderPlainReadMeTask.taskName(sample), RenderPlainReadMeTask.class, task -> {
                task.parameters(parameters -> {
                    parameters.getInputFile().set(readme.getLocation());
                    parameters.getOutputFile().fileProvider(project.provider(task.getTemporaryDirFactory()::create).map(it -> new File(it, "README")));
                });
            });

            project.getTasks().withType(Zip.class).configureEach(task -> {
                if (task.getName().equals("zip" + sample.getName())) {
                    task.exclude(it -> it.getFile().equals(readme.getLocation().getAsFile().get()));
                    task.from(project.getTasks().named(RenderPlainReadMeTask.taskName(sample)));
                }
            });
        });
        //endregion

        //region Summary/Manifest
        extension.getSamples().configureEach(sample -> {
            sample.getTitle().convention(project.provider(() -> sample.getExtensions().findByType(ReadMeExtension.class)).flatMap(ReadMeExtension::getLocation).map(it -> {
                try {
                    return Files.readAllLines(it.getAsFile().toPath()).stream().map(String::trim).filter(s -> s.startsWith("# ")).findFirst().map(s -> s.substring(2)).orElse(null);
                } catch (IOException e) {
                    return null;
                }
            }));

            project.getTasks().register(sample.getName() + "Manifest", WriteSampleManifestTask.class, task -> {
                task.getElements().put("title", sample.getTitle());
                task.getElements().put("name", sample.getName());
                task.getOutputFile().fileProvider(project.provider(task.getTemporaryDirFactory()::create).map(it -> new File(it, "manifest.json")));
            });
        });
        //endregion

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
                task.getArchiveClassifier().set(sample.getName());
            });

            NamedDomainObjectProvider<Configuration> configuration = project.getConfigurations().register(sample.getName() + "SampleElements");
            configuration.configure(config -> {
                config.setCanBeResolved(false);
                config.setCanBeConsumed(true);
                config.outgoing(outgoing -> {
                    outgoing.capability(new ProjectDerivedCapability(project, sample.getName()));
                    outgoing.artifact(zipTask);
                });
                config.attributes(attributes -> {
                    attributes.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, "sample"));
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
        TaskProvider<SourceCopyTask> setupTask = project.getTasks().register("copy" + StringUtils.capitalize(repo.getName()), SourceCopyTask.class, task -> {
            task.dependsOn(syncTask);
            task.getSampleDir().set(syncTask.get().getCheckoutDirectory());
            task.doFirst(task1 -> {
                repo.getSourceActions().forEach(it -> {
                    it.execute(task);
                });
            });
        });
        TaskProvider<UpdateRepoTask> updateTask = project.getTasks().register("update" + StringUtils.capitalize(repo.getName()), UpdateRepoTask.class, task -> {
            task.dependsOn(setupTask);
            task.getSampleDir().set(syncTask.get().getCheckoutDirectory());
            repo.getRepoActions().forEach(it -> {
                task.change(it);
            });
        });
        generateSource.configure(task -> {
            task.dependsOn(updateTask);
        });
    }

    private void addTasksForSample(Sample sample, Project project) {
        String sampleNameCamelCase = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, sample.getName());
        TaskProvider<SourceCopyTask> sourceCopyTask = project.getTasks().register(sampleNameCamelCase, SourceCopyTask.class, task -> {
            task.getSampleDir().set(sample.getSampleDir());
            sample.getSourceActions().forEach( it -> it.execute(task));
        });
    }

    /*private*/ static abstract /*final*/ class WriteSampleManifestTask extends DefaultTask {
        @Inject
        public WriteSampleManifestTask() {}

        @Input
        public abstract MapProperty<String, String> getElements();

        @OutputFile
        public abstract RegularFileProperty getOutputFile();

        @TaskAction
        private void doWrite() throws IOException {
            try (PrintWriter out = new PrintWriter(getOutputFile().get().getAsFile())) {
                new JsonBuilder(getElements().get()).writeTo(out);
            }
        }
    }

    /*private*/ static abstract /*final*/ class RenderPlainReadMeTask extends ParameterizedTask.UsingWorker<RenderPlainReadMeTask.Parameters> {
        public interface Parameters extends ParameterizedTask.Parameters, WorkParameters, ParameterizedTask.UsingWorker.CopyTo<Parameters> {
            @InputFile
            public abstract RegularFileProperty getInputFile();

            @OutputFile
            public abstract RegularFileProperty getOutputFile();
        }

        @Inject
        public RenderPlainReadMeTask(WorkerExecutor executor) {
            super(TaskWorkAction.class, executor::noIsolation);
        }

        public static String taskName(Sample sample) {
            return sample.getName() + "PlainReadMe";
        }

        /*private*/ static abstract /*final*/ class TaskWorkAction implements WorkAction<RenderPlainReadMeTask.Parameters> {
            private final ExecOperations execOperations;

            @Inject
            public TaskWorkAction(ExecOperations execOperations) {
                this.execOperations = execOperations;
            }

            @Override
            public void execute() {
                execOperations.exec(spec -> {
                    spec.commandLine("pandoc", "-t", "plain", "-o", getParameters().getOutputFile().getAsFile().get(), getParameters().getInputFile().getAsFile().get());
                });
            }
        }
    }
}
