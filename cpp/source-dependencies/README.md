# Application with source library dependencies (source-dependencies)

This sample demonstrates using external source dependencies to build Swift and C++ applications that require two libraries. The source for the libraries are hosted in separate Git repositories and declared as 'source dependencies' of the application. When Gradle builds the application, it first checks out a revision of the library source and uses this to build the binaries for the library.

The Git repositories to use are declared in the build's `settings.gradle` and then the libraries are referenced in the same way as binary dependencies in the build files.

To use this sample, build and run the application:

```
> cd cpp/source-dependencies
> ./gradlew assemble

BUILD SUCCESSFUL in 1s

> ./build/install/main/debug/app
World!
```

This will clone version `1.0` of the Git repository at `https://github.com/gradle/native-samples-cpp-library` and build the library binaries.

The application output is incorrect because of a bug in the utilities library.

Update the application to use a new version that contains a fix:

```
> cd ../..
> edit build.gradle # change dependency on org.gradle.cpp-samples:utilities:1.0 to org.gradle.cpp-samples:utilities:1.1
> ./gradlew assemble

BUILD SUCCESSFUL in 1s

> ./build/install/main/debug/app
Hello, World!
```