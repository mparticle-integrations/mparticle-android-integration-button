# Button Kit sample app

Sample app that can be used to test mParticle Button integration or for local kit development.

## Getting started

#### 1. Clone the repository

```bash
git clone git@github.com:mparticle-integrations/mparticle-android-integration-button.git
```

#### 2. Configure mParticle key and secret in `~/.gradle/gradle.properties`
```bash
mParticleKey=...
mParticleSecret=...
```

#### 3. Build and install sample app

```bash
./gradlew installDebug
```

***

## Frequently Asked Questions

#### Receiving the following error: `Bad API request - is the correct API key and secret configured?`
Confirm that mParticle SDK credentials are being properly retrieved in the `build.gradle` file as well as if the credentials themselves are correct.
If you recently issued new credentials, try re-installing the sample app as the credentials may have been cached.

#### ButtonKit is not initializing
Confirm the following:
1. The proper package name (e.g. `com.example.buttonkit`) is shown in the mParticle Dashboard.
2. There exists an input (the sample/test app) and an output (Button integration from mParticle marketplace) and that it is active.
3. The Button integration is configured properly with a valid [Application ID](https://app.usebutton.com/account/login/?next=/settings/organization) in the mParticle Dashboard.
4. You are not behind a proxy (e.g. Charles Proxy).
