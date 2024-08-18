# Application with Windows Resources (windows-resources)

This sample shows how Gradle is able to compile Windows Resources (`rc`) files and link them into a native binary.
This sample applies the `'cpp-application'` plugin.
This sample requires you have VisualCpp toolchain installed

To build and run the application:
(Note the application only runs and build on Windows)

```
> cd cpp/windows-resources
> ./gradlew assemble

BUILD SUCCESSFUL

> ./build/exe/main/debug/app.exe
Hello, World!
```