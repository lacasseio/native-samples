# Swift and C++ Sample Projects

This repository holds several sample Gradle builds which demonstrate how to
use Gradle to build Swift/C++ libraries and applications as [introduced in our blog post](https://blog.gradle.org/introducing-the-new-cpp-plugins).

Each sample build is listed below with a bit of information related to the
features of Gradle that are demonstrated in that build. Each sample is functionally
the same for both Swift and C++ languages.

Each C++ sample works on macOS, Linux and Windows with GCC, Clang and Visual C++.

Each Swift sample works on macOS and Linux, with Swift 3 and later.

## Getting Started

### Init script

First copy the init script to your user home.
Because of limitation in Gradle, we have to consume sample project as external builds (homologue to included builds).
To ensure correctness, we need to inject additional configuration which we do via the init script.

```
> cp sample.init.gradle ~/.gradle/init.d/
```

### Import in IntelliJ

Importing external builds in IntelliJ is a bit problematic.
First, build the plugins: `./gradlew help`
Then, import into IntelliJ.
You may have some error while importing, just `Reload All Gradle Projects` until everything is green.
During development, prefer reloading each project individually (right-click `Reload Gradle Project`).

## Deeper build insights with Build Scans

You can generate [build-scans](https://gradle.com/build-scans) with these samples by running Gradle with `--scan`.  At the end of the build, you will be prompted to upload build data to [scans.gradle.com](https://scans.gradle.com/get-started).

As an example of adding more data to a build scan, you can also run any sample with `-I ../../build-scan/buildScanUserData.gradle` in combination with `--scan`.  This will add custom values that describe what is being built like [these](https://scans.gradle.com/s/axgvl3hohykbk/custom-values#L1-L7).

### Contributing to these samples

If you want to contribute an improvement to the samples, please refer to the [`samples-dev` subproject](samples-dev/README.md). 

### Suggesting new samples

If you have a use case that isn't covered by an existing sample, open an issue for [gradle-native](https://github.com/gradle/gradle-native/issues). Please describe what you're trying to accomplish so we can help you find a solution.

### Visual Studio support

All of the C++ samples have Visual Studio support, added by applying the `visual-studio` plugin. To open a sample build in Visual Studio:

```
> cd <sample-dir>
> ./gradlew openVisualStudio
```

### Xcode support

All of the samples have Xcode support, added by applying the `xcode` plugin. To open a sample build in Xcode:

```
> cd <sample-dir>
> ./gradlew openXcode
```

### XCTest support

All Swift samples demonstrate XCTest support in Gradle. As a user, you can either interact with the test code as you would usually do through Xcode or you can run the test directly from the command line:

```
> cd <sample-dir>
> ./gradlew test
```

### Google Test support

The C++ sample `simple-library` demonstrates some basic Google test support in Gradle. This is currently not as refined as the XCTest support. 

To run the tests from the command line:

```
> cd <sample-dir>
> ./gradlew test
```

### Incremental Swift compilation

The `swiftc` compiler has a built-in incremental compilation feature that tries to reduce the number of `.swift` files that need to be recompiled on each build by analyzing the dependencies between all files.

Gradle enables Swift incremental compilation by default, so no extra configuration is required to take advantage of this feature with your Swift projects.

### Debug and release variants

The Swift and C++ plugins add a 'debug' and 'release' variant for each library or application. By default, the `assemble` task will build the debug variant only.

You can also use the `assembleDebug` and `assembleRelease` tasks to build a specific variant, or both variants.

At this stage, there are no convenience tasks to build all variants of a library or application.

### Publishing binaries to a Maven repository

Some of the C++ samples are configured to publish binaries to a local Maven repository. For these samples you can run:

```
> cd <sample-dir>
> ./gradle publish
> tree ../repo/
```

This will build and publish the debug and release binaries. The binaries are published to a repository in the `cpp/repo` directory.
