package org.gradle.samples.plugins.generators;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

/**
 * Clones/pulls changes from external repo.
 */
public abstract /*final*/ class SyncExternalRepoTask extends DefaultTask {
    public SyncExternalRepoTask() {
        getOutputs().upToDateWhen(it -> false);
    }

    @TaskAction
    private void checkout() throws IOException, GitAPIException {
        File checkoutDir = getCheckoutDirectory().get().getAsFile();
        if (new File(checkoutDir, ".git").exists()) {
            getLogger().lifecycle("Pull " + getRepoUrl().get() + " into " + checkoutDir);
            try (Git git = Git.open(checkoutDir)) {
                git.pull().setFastForward(MergeCommand.FastForwardMode.FF_ONLY).call();
            }
        } else {
            getLogger().lifecycle("Clone " + getRepoUrl().get() + " into " + checkoutDir);
            Git git = Git.cloneRepository()
                    .setURI(getRepoUrl().get())
                    .setDirectory(checkoutDir)
                    .call();
            git.close();
        }
    }


    @Input
    public abstract Property<String> getRepoUrl();

    @OutputDirectory
    public abstract DirectoryProperty getCheckoutDirectory();
}
