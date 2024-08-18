# Application with library dependencies in a single build (transitive-dependencies)

This sample shows how a C++ application and several libraries can be built with Gradle and linked together.
The sample is structured as a multi-project build, with the application and each library as separate projects in this build. Dependencies are added using project dependencies.

In this sample, the application and libraries all use the same implementation language.
Mixing C++ and Swift is shown in [another sample](TODO: link to sample).

To build and run the application:

```
> cd cpp/transitive-dependencies
> ./gradlew assemble

BUILD SUCCESSFUL

> ./app/build/install/main/debug/app
Hello, World!
```

The build script also demonstrates how to configure convenience tasks like `assembleDebuggable`, which will assemble all "debuggable" binaries.

```
> ./gradlew assembleDebuggable

BUILD SUCCESSFUL
```