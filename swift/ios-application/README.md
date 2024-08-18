# iOS Application (ios-application)

This sample demonstrates a iOS 11.2 application build for the iPhone simulator, allowing editing of the iOS specific files inside Xcode.

To use the sample, build the application:

```
> cd swift/ios-application
> ./gradlew assemble
```

Now install the application into an iOS simulator running iOS 11.2 by drag and dropping the app into a running simulator.

Finally, you can develop the application using Xcode IDE to edit the storyboard and asset catalog. Note that the sample doesn't allow running the iOS application from the IDE.

```
> ./gradlew openXcode
```