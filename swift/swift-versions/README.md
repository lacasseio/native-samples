# Supporting multiple Swift versions (swift-versions)

This sample demonstrates using multiple versions of Swift in a single build. There are two projects that build identical applications. One is written in Swift 3 compatible code (`swift3-app`) and one is written with Swift 4 compatible code (`swift4-app`). When running the application, it will print a message about which version of Swift was used.

### Swift 4

If you have the Swift 4 compiler installed, you can build both applications:

```
# NOTE: Needs Swift 4 tool chain
> cd swift/swift-versions
> ./gradlew assemble

BUILD SUCCESSFUL in 1s

> ./swift4-app/build/install/main/debug/App
Built for Swift 4
Hello, World!
> ./swift3-app/build/install/main/debug/App
Built for Swift 3
Hello, World!
```

By default, the tests for a given Swift production component will be compiled for the same version of Swift. For instance, in `swift3-app`, the production and test code will be built with Swift 3 source compatibility.

### Swift 3

If you have the Swift 3 compiler installed, you can only build the Swift 3 application. Attempting to build the Swift 4 application will fail.

```
> cd swift/swift-versions
> ./gradlew swift3-app:assemble

BUILD SUCCESSFUL in 1s

> ./swift3-app/build/install/main/debug/app
Built for Swift 3
Hello, World!
```

Currently, Gradle does not offer a convenience to ignore projects that are not buildable due to missing or incompatible tool chains.