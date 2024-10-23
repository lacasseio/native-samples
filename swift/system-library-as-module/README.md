# Using a module for a system library with Swift (system-library-as-module)

Existing system libraries can be wrapped in user defined `module.modulemap` files.

This sample demonstrates a Swift application that uses libcurl to fetch `example.com`.

```
> cd swift/system-library-as-module
> ./gradlew assemble

BUILD SUCCESSFUL

> ./build/install/main/debug/App
```