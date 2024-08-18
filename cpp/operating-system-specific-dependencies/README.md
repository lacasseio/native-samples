# Application with operating system specific library dependencies (operating-system-specific-dependencies)

This sample demonstrates an application that has dependencies on different libraries for each operating system.
Currently, there are no conveniences for using libraries that are installed on the build machine.

```
> cd cpp/operating-system-specific-dependencies
> ./gradlew assemble

BUILD SUCCESSFUL

> ./app/build/install/main/debug/app
Hello, World!
```

The application selects the 'ansiConsole' library on macOS and Linux and the 'winConsole' library when built on Windows.
The output is blue on macOS and green on Linux and Windows.
Each console library is configured to only build on specific operating systems, while the application is configured to build on all operating systems (Windows, Linux, macOS).