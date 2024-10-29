package org.gradle.samples.plugins.generators;

import com.google.common.base.CaseFormat;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.TaskProvider;

import javax.inject.Inject;
import java.util.stream.Collectors;

/*private*/ abstract /*final*/ class CopySourcePlugin implements Plugin<Project> {
    @Inject
    public CopySourcePlugin() {}

    @Override
    public void apply(Project project) {
        TaskCollection<SourceCopyTask> generatorTasks = project.getTasks().withType(SourceCopyTask.class);

        project.getTasks().named("generateSource").configure(task -> task.dependsOn(generatorTasks));

        project.getTasks().named("samplesManifest", SamplesManifestTask.class).configure(task -> {
            task.getSampleDirs().set(project.provider(() -> {
                return generatorTasks.stream().map(generator -> {
                    return generator.getSampleDir().get().getAsFile().getAbsolutePath();
                }).collect(Collectors.toList());
            }));
        });

        SamplesExtension extension = project.getExtensions().getByType(SamplesExtension.class);
        extension.getSamples().all(sample -> {
            String sampleNameCamelCase = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, sample.getName());
            TaskProvider<SourceCopyTask> sourceCopyTask = project.getTasks().register(sampleNameCamelCase, SourceCopyTask.class, task -> {
                task.getSampleDir().set(sample.getSampleDir());
            });

            sample.getExtensions().add("copySource", sourceCopyTask.get());
        });
        extension.getExternalRepos().all(repo -> {
            TaskProvider<SyncExternalRepoTask> syncTask = project.getTasks().named("sync" + StringUtils.capitalize(repo.getName()), SyncExternalRepoTask.class);
            TaskProvider<SourceCopyTask> setupTask = project.getTasks().register("copy" + StringUtils.capitalize(repo.getName()), SourceCopyTask.class, task -> {
                task.dependsOn(syncTask);
                task.getSampleDir().set(syncTask.get().getCheckoutDirectory());
            });
            project.getTasks().named("update" + StringUtils.capitalize(repo.getName()), UpdateRepoTask.class).configure(task -> task.dependsOn(setupTask));
            repo.getExtensions().add("copySource", setupTask.get());
        });

        // Apply conventions to the generator tasks
        generatorTasks.configureEach( task -> {
            task.getTemplatesDir().set(project.file("src/templates"));
        });
    }
}
