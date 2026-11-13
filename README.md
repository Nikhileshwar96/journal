# Daily Journal

A simple, 100% offline Android journaling app: text entries with optional title, editable date
and multiple photos. Built with Kotlin, Jetpack Compose (Material 3), Room and Coil.

- **Package:** `com.nikhil.journal` · **Version:** 1.0.0 (versionCode 1)
- **minSdk** 26 · **targetSdk** 36 (the level Google Play requires for new apps) · **compileSdk** 37
  (current AndroidX libraries need 37 to compile; this affects build-time APIs only, not runtime behaviour)
- **Permissions:** none. There's no INTERNET permission. Gallery uses the system Photo Picker, and the
  camera goes through the system camera app (`ACTION_IMAGE_CAPTURE`), which needs no CAMERA permission.
- **Storage:** Room database plus downscaled JPEG copies (max 1600 px long edge, 85% quality) in
  `filesDir/images/`. Data is excluded from cloud backup. Device-to-device transfer (setting up a
  new phone) is allowed.

## Requirements

- Android SDK (Android Studio installs it). `local.properties` must point to it (`sdk.dir=...`).
  Android Studio creates this file automatically.
- JDK 17–25. The JDK bundled with Android Studio works:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

## Build & test

```bash
./gradlew assembleDebug
```

```bash
./gradlew testDebugUnitTest
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Release signing (upload key)

### 1. Generate the upload keystore (one time)

Keep it **outside** the repo and back it up somewhere safe. `keytool` does not create missing
folders, so create the folder first:

```bash
mkdir -p ~/keystores
```

```bash
keytool -genkeypair -v -keystore ~/keystores/daily-journal-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

`keytool` asks for a keystore password, a key password and your name/organization. If `keytool`
isn't on your PATH, use `"$JAVA_HOME/bin/keytool"`.

### 2. Create `keystore.properties` in the project root

This file is git-ignored. Never commit it.

```properties
storeFile=/Users/<you>/keystores/daily-journal-upload.jks
storePassword=<keystore password>
keyAlias=upload
keyPassword=<key password>
```

`storeFile` can be absolute or relative to the project root. Without this file, release builds
still succeed but are left unsigned.

### 3. Build the signed App Bundle

```bash
./gradlew bundleRelease
```

The bundle is written to **`app/build/outputs/bundle/release/app-release.aab`**.

The release build uses R8 minification and resource shrinking. The R8 mapping file, which you can
upload to Play Console for readable crash traces, is at
`app/build/outputs/mapping/release/mapping.txt`.

> Use **Play App Signing** (the default for new apps). Google holds the app signing key, and your
> keystore is only the *upload* key. If you lose it, you can request an upload key reset from Play
> Console instead of losing the app.

## Play Console: internal testing checklist

1. **Create the app.** In Play Console, choose *Create app*. Name it "Daily Journal", set the
   default language, choose *App* and *Free*, and accept the declarations.
2. **Set up the store listing** (*Grow → Store presence → Main store listing*):
   - Short description and full description.
   - 512×512 app icon and 1024×500 feature graphic.
   - At least 2 phone screenshots.
3. **Complete App content** (*Policy → App content*):
   - **Privacy policy:** a URL is required. A short page saying the app collects no data and
     stores everything only on the device is enough.
   - **Ads:** No, the app does not contain ads.
   - **App access:** All functionality is available without special access (there's no login).
   - **Content rating:** fill in the questionnaire. Category: *Utility/Productivity*; answer
     "No" to violence, sexual content, gambling and similar. User-generated content isn't shared
     with other users.
   - **Target audience:** 18+ (or 13+). Don't target children unless you mean to, because that
     triggers Families policy requirements.
   - **News app:** No. **Government app:** No. **Financial features:** None. **Health:** None.
   - **Data safety:** see below.
4. **Data safety form:**
   - *Does your app collect or share any of the required user data types?* **No.**
     Data stored only on the device and never sent off it doesn't count as "collected". The app
     has no INTERNET permission and no SDKs that transmit data.
   - Because you answered "No", the encryption-in-transit and data-deletion questions are skipped.
     If asked, there is no account creation.
   - Result: the listing shows **"No data collected"** and **"No data shared with third parties"**.
5. **Create the internal testing release** (*Test and release → Testing → Internal testing*):
   - *Testers* tab: create an email list and add testers (up to 100). Copy the opt-in link.
   - *Create new release*: accept Play App Signing if prompted, then upload `app-release.aab`.
   - Release name: `1.0.0 (1)`. Add release notes, for example "First internal build."
   - *Next*, review any warnings, then *Save and publish*. Internal releases are usually available
     within minutes.
6. **Install as a tester:** testers open the opt-in link, accept, then install from Google Play.
7. **Next uploads:** bump `versionCode` (and `versionName`) in `app/build.gradle.kts` before each
   new bundle. Play rejects a reused versionCode.

## Project layout

```
app/src/main/java/com/nikhil/journal/
  JournalApp.kt            Application + manual DI container
  MainActivity.kt          Single activity (splash, edge-to-edge, Compose)
  data/                    Room entities/DAO/database, ImageStorage, JournalRepository
  ui/                      NavHost, ViewModel factory, theme
    home/ edit/ detail/    Screens + ViewModels (StateFlow / Compose state)
    components/            Fullscreen zoomable image, formatting helpers
app/schemas/               Exported Room schema (keep it; needed for future migrations)
```
