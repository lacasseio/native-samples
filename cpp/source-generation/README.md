# Source generation (source-generation)

This sample demonstrates using a task to generate source code before building a C++ application.

```
> cd cpp/source-generation
> ./gradlew assemble

BUILD SUCCESSFUL

> ./build/install/main/debug/app
Hello, World!
```

Generated sources will be under `build/generated`.