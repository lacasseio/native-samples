# Simple Library (simple-library)

This sample shows how a C++ library can be built with Gradle.
The library has no dependencies.
The build is configured to add unit tests.
The C++ sample also adds binary publishing to a Maven repository.

To build the library:

```
> cd cpp/simple-library
> ./gradlew assemble

BUILD SUCCESSFUL in 1s

> find build/lib/main/debug
build/lib/main/debug/liblist.dylib
```

To run the unit tests for the library:

```
> ./gradlew test

BUILD SUCCESSFUL in 1s
```