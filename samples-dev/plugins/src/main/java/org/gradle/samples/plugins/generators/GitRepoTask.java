package org.gradle.samples.plugins.generators;

public abstract /*final*/ class GitRepoTask extends UpdateRepoTask {
    @Override
    boolean isDeleteRepo() {
        return true;
    }
}
