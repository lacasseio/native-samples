# Targetting multiple machines (multiple-target-machines)

This sample shows how a simple C++ application can target multiple machines with Gradle.
The application has no dependencies and the build has minimal configuration.

To build and run the application for the target machine of the same type as the current host:

```
> cd cpp/multiple-target-machines
> ./gradlew assemble

BUILD SUCCESSFUL

> ./build/install/main/debug/app
Hello, World!
```