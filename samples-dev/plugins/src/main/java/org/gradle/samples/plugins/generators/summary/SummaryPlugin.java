package org.gradle.samples.plugins.generators.summary;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.samples.plugins.generators.SamplesExtension;
import org.gradle.samples.plugins.generators.manifest.ManifestExtension;
import org.gradle.samples.plugins.generators.readme.ReadMeExtension;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;

/*private*/ abstract /*final*/ class SummaryPlugin implements Plugin<Project> {
    @Inject
    public SummaryPlugin() {}

    @Override
    public void apply(Project project) {
        SamplesExtension extension = project.getExtensions().getByType(SamplesExtension.class);
        extension.getSamples().all(sample -> {
            SummaryExtension summary = sample.getExtensions().create("summary", SummaryExtension.class);
            summary.getTitle().convention(project.provider(() -> sample.getExtensions().findByType(ReadMeExtension.class)).flatMap(ReadMeExtension::getLocation).map(it -> {
                try {
                    return Files.readAllLines(it.getAsFile().toPath()).stream().map(String::trim).filter(s -> s.startsWith("# ") || s.startsWith("= ")).findFirst().map(s -> s.substring(2)).orElse(null);
                } catch (IOException e) {
                    return null;
                }
            }));

            sample.getExtensions().configure(ManifestExtension.class, it -> it.put("title", summary.getTitle()));
        });
    }
}
