# Application with operating system specific library dependencies (operating-system-specific-dependencies)

This sample demonstrates an application that has dependencies on different libraries for each operating system.
Currently, there are no conveniences for using libraries that are installed on the build machine.

```
> cd swift/operating-system-specific-dependencies
> ./gradlew assemble

BUILD SUCCESSFUL

> app/build/install/main/debug/App
Hello, World!
```

The application selects the 'MacOsConsole' library that prints the output in blue when building on macOS.  On Linux, it selects the 'LinuxConsole' library that prints the output in green. Each console library is configured to only build on specific operating systems.