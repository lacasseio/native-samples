# Application with source library dependencies (source-dependencies)

This sample demonstrates using external source dependencies to build Swift and C++ applications that require two libraries. The source for the libraries are hosted in separate Git repositories and declared as 'source dependencies' of the application. When Gradle builds the application, it first checks out a revision of the library source and uses this to build the binaries for the library.

The Git repositories to use are declared in the build's `settings.gradle` and then the libraries are referenced in the same way as binary dependencies in the build files.

To use this sample, build and run the application:

```
> cd swift/source-dependencies
> ./gradlew assemble

BUILD SUCCESSFUL in 1s

> ./build/install/main/debug/App
World!
```

This will clone version `1.0` of the Git repository at `https://github.com/gradle/native-samples-swift-library` and build the library binaries.

You can see the application's output is incorrect. The build is configured to use version `1.0` of the utilities library from this repository and this version contains a bug. Let's fix this.

Version `1.1` of the library contains a bug fix. Update the application to use the new version:

```
> cd ../..
> edit build.gradle # change dependency on org.gradle.swift-samples:utilities:1.0 to org.gradle.swift-samples:utilities:1.1
> ./gradlew assemble

BUILD SUCCESSFUL in 1s

> ./build/install/main/debug/App
Hello, World!
```

Dynamic dependencies are also supported, so you could also use `1.+`, `[1.1,2.0]` or `latest.release`. Gradle matches the tags of the Git repository to determine which Git revision to use. Branches are also supported, but use a different syntax. See the following sample.