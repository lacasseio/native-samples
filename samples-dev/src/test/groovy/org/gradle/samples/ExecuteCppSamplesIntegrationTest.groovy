package org.gradle.samples

import org.gradle.internal.os.OperatingSystem
import org.gradle.samples.fixtures.Samples
import org.gradle.samples.fixtures.SwiftPmRunner
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assumptions
import spock.lang.Requires
import spock.lang.Unroll

import static org.gradle.samples.fixtures.Samples.withArgs
import static org.junit.jupiter.api.Assumptions.assumeFalse
import static org.junit.jupiter.api.Assumptions.assumeTrue

class ExecuteCppSamplesIntegrationTest extends ExecuteSamplesIntegrationTest {
    @Unroll
    def "can build C++ '#sample.name'"() {
        // TODO - remove these once documentation parsing can better understand the setup
        assumeTrue(sample.sampleName != 'swift-package-manager-publish')

        // CMake may not be available
        if (sample.name.contains('cmake') || sample.name == "cpp/library-with-tests") {
            assumeTrue(cmakeAvailable())
        }

        if (sample.name.contains('autotools')) {
            assumeTrue(notWindows())
        }

        if (sample.name == "cpp/windows-resources") {
            assumeTrue(isWindows())
        }

        // Tool chains can only be provision on Linux and macOS for C++
        assumeFalse(sample.sampleName == 'provisionable-tool-chains' && OperatingSystem.current().windows)

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
        sample << Samples.getSamples("cpp")
    }

    @Requires({ !OperatingSystem.current().windows })
    def "can build 'swift-package-manager-publish'"() {
        given:
        def sample = Samples.useSampleIn("cpp/swift-package-manager-publish")
        sample.clean()

        expect:
        GradleRunner.create()
                .withProjectDir(sample.sampleDir.parentFile.parentFile)
                .withArguments(withArgs("generateRepos"))
                .build()

        GradleRunner.create()
                .withProjectDir(new File(sample.sampleDir, "list-library"))
                .withArguments(withArgs("build", "release"))
                .build()

        SwiftPmRunner.create()
                .withProjectDir(new File(sample.sampleDir, "list-library"))
                .withArguments(withArgs("build"))
                .build()

        GradleRunner.create()
                .withProjectDir(new File(sample.sampleDir, "utilities-library"))
                .withArguments(withArgs("build", "release"))
                .build()

        SwiftPmRunner.create()
                .withProjectDir(new File(sample.sampleDir, "utilities-library"))
                .withArguments(withArgs("build"))
                .build()

        SwiftPmRunner.create()
                .withProjectDir(new File(sample.sampleDir, "app"))
                .withArguments(withArgs("build"))
                .build()
    }

    String getSampleLanguage() { 'cpp' }
}
