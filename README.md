## Button Kit Integration

This repository contains the [Button](https://www.usebutton.com/) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

### Adding the integration

1. Add the kit dependency to your app's build.gradle:

    ```groovy
    dependencies {
        implementation 'com.mparticle:android-button-kit:5+'
    }
    ```
2. Follow the mParticle Android SDK [quick-start](https://github.com/mParticle/mparticle-android-sdk), then rebuild and launch your app, and verify that you see `"Button detected"` in the output of `adb logcat`.
3. Reference mParticle's integration docs below to enable the integration.

### Documentation

[Button integration](http://docs.mparticle.com/?java#button)

### Further Development

To develop/update the ButtonKit locally, make the following changes to the module's `build.gradle`:
1. Modify the the mParticle dependency classpath to use the latest SDK:
    ```groovy
    dependencies {
        classpath 'com.mparticle:android-kit-plugin:+'
    }
    ```
2. Add a `project.version` variable targeting the latest SDK. For example:
    ```groovy
    project.version = '5.4.0'
    ```

### License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)