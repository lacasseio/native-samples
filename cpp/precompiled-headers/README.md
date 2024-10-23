# Application with precompiled headers (precompiled-headers)

This sample shows how Gradle is able to compile code using precompiled headers.
This sample applies the `'cpp-application'` plugin.

To build and run the application:

```
> cd cpp/precompiled-headers
> ./gradlew assemble

BUILD SUCCESSFUL

> ./build/exe/main/debug/app
Hello, World!
```