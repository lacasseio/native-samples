# Application with Swift package manager conventions (swift-package-manager)

This sample shows how to configure Gradle to use a source layout that is different to its conventions. In this case, the sample uses the typical layout for a Swift Package Manager package.
It contains an application and a single library. The source files for the application and libraries are all under a single `Sources` directory.

This sample also includes a Swift Package Manager build file, so the same source can be built using Swift Package Manager

```
> cd cpp/swift-package-manager
> ./gradlew assemble

BUILD SUCCESSFUL in 1s

> ./build/app/install/main/debug/app
Hello, World!
```