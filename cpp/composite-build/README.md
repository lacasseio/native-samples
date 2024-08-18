# Application with library dependencies in a composite build (composite-build)

This sample shows how several otherwise independent Swift or C++ libraries can be built together with Gradle. The sample is structured as separate builds for each of the libraries and a composite build that includes these.

To build and run the application:

```
> cd cpp/composite-build
> ./gradlew assemble

BUILD SUCCESSFUL in 1s

> ./build/install/main/debug/app
Hello, World!
```