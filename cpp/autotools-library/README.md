# Libraries build with autotools (autotools-library)

This sample demonstrates using Gradle's dependency management features to coordinate building libraries built by Autotools (i.e. `configure` and `make`).
The sample is composed of an application and a curl library.
The curl library is downloaded from the [Curl home page](https://curl.haxx.se/) and then built using the Autotools configuration provided with the library.
The application is built with Gradle, linking the curl library statically.
When run, the application uses curl to query a REST service to lookup the capital of Sweden and prints this to the console.

To use the sample, create the Git repositories containing the libraries:

```
> cd cpp/autotools-library
> ./gradlew -p ../.. generateRepos
```

Now build the application:

```
> ./gradlew assemble

BUILD SUCCESSFUL

> ./app/build/install/main/debug/app
```