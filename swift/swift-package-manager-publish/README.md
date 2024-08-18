# Using Gradle builds from Swift Package Manager (swift-package-manager-publish)

This sample shows how libraries built with Gradle can be used by projects that are built with Swift Package Manager, without having to maintain separate Gradle and Swift PM build for the library.

The sample is made up of an application built using Swift PM, and two libraries that are built using Gradle. The sample also includes a 'release' plugin that takes care of generating a Swift PM build from a Gradle build.

To use the sample, setup the Git repositories for the libraries:

```
> ./gradlew generateRepos
```

Next, create a release of the list library that can be used by Swift PM. This generates a `Package.swift` file to be used by Swift PM, commits the changes and creates a tag:

```
> cd swift/swift-package-manager-publish/list-library
> ./gradlew build release

BUILD SUCCESSFUL in 1s

```

Do the same for the utilities library:

```
> cd ../utilities-library
> ./gradlew build release

BUILD SUCCESSFUL in 1s

```

Now build the application using Swift PM:

```
> cd ../app
> swift build
```