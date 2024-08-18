# Simple Library (simple-library)

This sample shows how a Swift library can be built with Gradle.
The library has no dependencies.
The build is configured to add unit tests.

To build the library:

```
> cd swift/simple-library
> ./gradlew assemble

BUILD SUCCESSFUL

> find build/lib/main/debug
build/lib/main/debug/libList.dylib
```

To run the unit tests for the library:

```
> ./gradlew test
> open build/reports/tests/xcTest/index.html
```