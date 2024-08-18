package org.gradle.samples

import org.gradle.samples.fixtures.Samples
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Unroll

import static org.gradle.samples.fixtures.Samples.withArgs

class ExecuteCSamplesIntegrationTest extends ExecuteSamplesIntegrationTest {
    @Unroll
    def "can build C '#sample.name'"() {
        given:
        sample.clean()
        runSetupFor(sample)

        expect:
        GradleRunner.create()
                .withProjectDir(sample.workingDir)
                .withArguments(withArgs("build"))
                .build()

        GradleRunner.create()
                .withProjectDir(sample.workingDir)
                .withArguments(withArgs("xcode"))
                .build()

        GradleRunner.create()
                .withProjectDir(sample.workingDir)
                .withArguments(withArgs("assembleRelease"))
                .build()

        where:
        sample << Samples.getSamples("c")
    }
    String getSampleLanguage() { 'c' }
}
