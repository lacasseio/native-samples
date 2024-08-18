# Simple application (application)

This sample shows how a simple C++ application can be built with Gradle.
The application has no dependencies and the build has minimal configuration.

Although there is currently no out-of-the-box support for building applications and libraries from C, there is also a sample build that shows how the C++ support can be configured to build a C application.

To build and run the application:

```
> cd cpp/application
> ./gradlew assemble

BUILD SUCCESSFUL

> ./build/install/main/debug/app
Hello, World!
```