# Application with prebuilt library dependencies in a Maven repository (binary-dependencies)

This sample shows how to publish C++ libraries to a Maven repository and use them from another build. This is currently not supported for Swift.

To use the sample, first publish a library to the repository using the `simple-library` build:

```
> cd cpp/binary-dependencies
> ./gradlew -p ../simple-library publish

BUILD SUCCESSFUL in 1s
```

You can find the repository in the `cpp/repo` directory.

Next, build the application that uses the library from this repository.

```
> ./gradlew assemble

BUILD SUCCESSFUL in 1s

> ./build/install/main/debug/app
Hello, World!
```

The build is also configured to download the Google test binaries from a Maven repository to build and run the unit tests:

```
> ./gradlew test

BUILD SUCCESSFUL in 1s
```