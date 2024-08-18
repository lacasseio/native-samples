# Application with dependency on upstream branch (dependency-on-upstream-branch)

This sample shows how a source dependency on a particular branch can be used.

To use this sample, create the Git repositories containing the libraries:

```
> cd swift/dependency-on-upstream-branch/app
> ./gradlew -p ../../.. generateRepos
```

Now you can build and run the application:

```
> ./gradlew assemble

BUILD SUCCESSFUL in 1s

> ./build/install/main/debug/App
World!
```

You can see the application's output is incorrect. The build is configured to use the most recent changes from the 'release' branch of the utilities library and this branch contains a bug. Let's fix this.

Edit the source of the utilities library to fix the bug:

```
> cd repos/utilities-library
> git checkout release
> edit src/main/swift/Util.swift # follow the instructions to fix the bug in function join()
> git commit -a -m 'fixed bug'
```

There's no need to create a tag, as Gradle will take care of checking out the new branch tip.

Now build and run the application:

```
> cd ../..
> ./gradlew assemble

BUILD SUCCESSFUL in 1s

> ./build/install/main/debug/App
Hello, World!
```