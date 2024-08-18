# Provisioning tool chains from within Gradle (provisionable-tool-chains)

This sample shows how to provision tool chains used by a Gradle build instead of the system tool chains. Please note the sample doesn't provision any tool chain for Windows yet. The sample can only provision tool chain at configuration. We use the `buildSrc` included build to use tasks for the provisioning.

This sample demonstrates a Swift tool chain provisioning under Linux.

```
> cd swift/provisionable-tool-chains
> ./gradlew assemble

BUILD SUCCESSFUL in 1s

> ./build/install/main/debug/App
```