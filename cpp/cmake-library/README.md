# Application uses a library built by CMake (cmake-library)

This sample demonstrates integrating a library that is built by CMake into a Gradle build.
The sample is structured as a multi-project build.
There are two projects, 'app' and 'utilities', which are built by Gradle. Both of these depend on a library 'list' that is built using CMake.
The 'list' library has a Gradle project that wraps the CMake build and exposes its outputs in a way that other Gradle builds can consume.

The sample packages the CMake integration logic as a 'cmake-library' plugin and applies the plugin to the 'library' project as a source dependency.

To use the sample, first create the Git repository containing the sample plugin:

```
> cd cpp/cmake-library
> ./gradlew -p ../.. generateRepos
```

Now build the application:

```
> ./gradlew assemble

BUILD SUCCESSFUL

> ./app/build/install/main/debug/app
Hello, World!
```

This generates the CMake build for the 'list' library, if required, and builds the libraries and application.