# Application with prebuilt library dependencies (prebuilt-binaries)

This sample shows how to use pre-built binaries that are already available on the local machine. Currently, Gradle does not offer a convenient way to do this but it is possible to configure Gradle to use these binaries.

To use the sample, first create the binaries using the `simple-library` build:

```
> cd swift/prebuilt-binaries
> ./gradlew -p ../simple-library assembleDebug assembleRelease

BUILD SUCCESSFUL in 1s
```

Next, build and run the application that uses these binaries:

```
> ./gradlew assemble

BUILD SUCCESSFUL in 0s

> ./build/install/main/debug/App
Hello, World!
```