# Application and libraries built by CMake (cmake-source-dependencies)

This sample demonstrates using Gradle's dependency management features to coordinate building an application and libraries built by CMake. The sample is composed of an application and two libraries. Each of these is hosted in a separate Git repository and connected together using source dependencies.

The sample packages the CMake integration logic as a 'cmake-application' and 'cmake-library' plugin and applies these to the different builds.

To use the sample, first create the Git repository containing the sample plugin:

```
> cd cpp/cmake-source-dependencies/app
> ./gradlew -p ../../.. generateRepos
```

Now build the application:

```
> ./gradlew assemble

BUILD SUCCESSFUL in 1s

> ./build/debug/app
Hello, World!
```

This generates the CMake builds for each of the libraries and the application, then builds and links the libraries and applications.