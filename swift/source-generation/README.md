# Source generation (source-generation)

This sample demonstrates using a task to generate source code before building a Swift application.

```
> cd swift/source-generation
> ./gradlew assemble

BUILD SUCCESSFUL

> ./build/install/main/debug/App
Hello, World!
```

Generated sources will be under `build/generated`.