# Using Gradle builds from Swift Package Manager (swift-package-manager-publish)

This sample shows how libraries built with Gradle can be used by projects that are built with Swift Package Manager, without having to maintain separate Gradle and Swift PM build for the library.

The sample is made up of an application built using Swift PM, and two libraries that are built using Gradle. The sample also includes a 'release' plugin that takes care of generating a Swift PM build from a Gradle build.

```
> ./gradlew generateRepos
> cd cpp/swift-package-manager-publish/list-library
> ./gradlew build release

BUILD SUCCESSFUL in 1s

> cd ../utilities-library
> ./gradlew build release

BUILD SUCCESSFUL in 1s

> cd ../app
> swift build
```