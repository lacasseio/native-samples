# Application with library dependencies in a single build (transitive-dependencies)

This sample shows how a Swift application and several libraries can be built with Gradle and linked together.
The sample is structured as a multi-project build, with the application and each library as separate projects in this build. Dependencies are added using project dependencies.

In this sample, the application and libraries all use the same implementation language.
Mixing C++ and Swift is shown in [another sample](TODO:link to sample).

To build and run the application:

```
> cd swift/transitive-dependencies
> ./gradlew assemble

BUILD SUCCESSFUL in 1s

> ./app/build/install/main/debug/App
Hello, World!
```