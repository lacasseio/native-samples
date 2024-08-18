# Libraries with tests (library-with-tests)

This sample demonstrates some basic Google test support in Gradle, building the GoogleTest library from source.

To run the tests from the command line:

```
> cd cpp/library-with-tests
> ./gradlew -p ../.. generateRepos
> ./gradlew assemble
> ./gradlew test
```