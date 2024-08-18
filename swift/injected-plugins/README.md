# Application uses libraries that are not built by Gradle (injected-plugins)

Gradle can also consume source dependencies that come from repositories without Gradle builds. When declaring a source dependency's repository information, you can instruct Gradle to inject plugins into the source dependency. These plugins can configure a Gradle build based on the contents of the repository.

To use the sample, create the Git repositories containing the libraries:

```
> cd swift/injected-plugins
> ./gradlew -p ../.. generateRepos
```

Now build the application:

```
> ./gradlew assemble

BUILD SUCCESSFUL in 1s

> ./build/install/main/debug/App
Hello, from Gradle build
Hello, World!
```

In the "repos" directory, you can find the source code without any Gradle configuration. The `utilities` and `list` builds are configured with the `utilities-build` and `list-build` plugins.