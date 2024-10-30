package org.gradle.samples.plugins.generators;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;

public class SamplesExtension {
    private final NamedDomainObjectContainer<ExternalRepo> externalRepos;
    private final NamedDomainObjectContainer<Sample> samples;

    @Inject
    public SamplesExtension(ObjectFactory objects) {
        externalRepos = objects.domainObjectContainer(ExternalRepo.class, name -> {
            return objects.newInstance(ExternalRepo.class, name);
        });
        samples = objects.domainObjectContainer(Sample.class, name -> {
            return objects.newInstance(Sample.class, name);
        });
    }

    public NamedDomainObjectContainer<ExternalRepo> getExternalRepos() {
        return externalRepos;
    }

    public NamedDomainObjectContainer<Sample> getSamples() {
        return samples;
    }
}
