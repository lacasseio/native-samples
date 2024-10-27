package org.gradle.samples

import org.gradle.internal.os.OperatingSystem
import org.gradle.samples.fixtures.NativeSample
import org.gradle.samples.fixtures.Samples
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.Unroll

import static org.junit.jupiter.api.Assumptions.assumeFalse
import static org.junit.jupiter.api.Assumptions.assumeTrue

abstract class ExecuteSamplesIntegrationTest extends Specification {

    static def wrapperGradleVersion() {
        return { GradleRunner runner ->
            def wrapperPropertiesFile = new File(runner.projectDir, 'gradle/wrapper/gradle-wrapper.properties')
            if (wrapperPropertiesFile.exists()) {
                Properties properties = new Properties()
                wrapperPropertiesFile.withInputStream {
                    properties.load(it)
                }
                def matcher = properties.distributionUrl =~ /(\d+\.\d+(\.\d+)?)/
                if (matcher.find()) {
                    println "Using Gradle v${matcher[0][0]}"
                    runner = runner.withGradleVersion(matcher[0][0])
                }
            }
            return runner
        }
    }

    def runSetupFor(NativeSample sample) {
        // Ensure only one test process is running the setup steps
        withFileLock {
            def docs = sample.documentation
            docs.setupSteps.each { command ->
                println "Running setup step " + command + " in " + docs.workingDir
                GradleRunner.create()
                        .withProjectDir(sample.workingDir)
                        .withArguments((command.split().drop(1) as List) + ["-S"])
                        .with(wrapperGradleVersion())
                        .build()
            }
        }
    }

    def withFileLock(Closure cl) {
        def lockFile = new File(Samples.rootSampleDir, "build/test.lock")
        if (!lockFile.isFile()) {
            lockFile.parentFile.mkdirs()
            lockFile.createNewFile()
        }
        def fileAccess = new RandomAccessFile(lockFile, "rw")
        def lock = fileAccess.channel.lock()
        try {
            cl.call()
        } finally {
            lock.close()
        }
    }

    boolean cmakeAvailable() {
         OperatingSystem.current().findInPath("cmake") != null
    }

    boolean notWindows() {
        return !OperatingSystem.current().isWindows()
    }

    boolean isWindows() {
        return OperatingSystem.current().isWindows()
    }

    @Unroll
    def "can run help for '#sample.name' without running any setup steps"() {
        // TODO - remove this when instruction parsing is smarter
        assumeTrue(sample.sampleName != 'swift-package-manager-publish')
        assumeTrue(sample.sampleName != 'cmake-library')
        assumeTrue(sample.sampleName != 'cmake-source-dependencies')
        assumeTrue(sample.sampleName != 'autotools-library')
        assumeTrue(sample.sampleName != 'library-with-tests')
        // Tool chains can only be provision on Linux for Swift and Linux and macOS for C++
        assumeFalse(sample.languageName == 'swift' && sample.sampleName == 'provisionable-tool-chains' && OperatingSystem.current().macOsX)
        assumeFalse(sample.sampleName == 'provisionable-tool-chains' && OperatingSystem.current().windows)

        given:
        sample.clean()

        expect:
        new File(sample.workingDir, "gradlew").file
        new File(sample.workingDir, "gradlew.bat").file
        new File(sample.workingDir, "settings.gradle").file

        GradleRunner.create()
                .withProjectDir(sample.workingDir)
                .withArguments("help")
                .with(wrapperGradleVersion())
                .build()

        where:
        sample << Samples.getSamples(getSampleLanguage())
    }

    abstract String getSampleLanguage()
}
