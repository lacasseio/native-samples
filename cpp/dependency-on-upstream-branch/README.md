# Application with dependency on upstream branch (dependency-on-upstream-branch)

This sample shows how a source dependency on a particular branch can be used.

To use this sample, create the Git repositories containing the libraries:

```
> cd cpp/dependency-on-upstream-branch/app
> ./gradlew -p ../../.. generateRepos
```

Build and run the application:

```
> ./gradlew assemble

BUILD SUCCESSFUL in 1s

> ./build/install/main/debug/app
World!
```

Edit the source of the utilities library to fix the bug:

```
> cd repos/utilities-library
> git checkout release
> edit src/main/cpp/join.cpp # follow the instructions to fix the bug in function join()
> git commit -a -m 'fixed bug'
```

Now build the application:

```
> cd ../..
> ./gradlew assemble

BUILD SUCCESSFUL in 1s

> ./build/install/main/debug/app
Hello, World!
```