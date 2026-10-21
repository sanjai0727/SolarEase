# SolarEase — Critical Android Installation Compatibility Audit and Fix Report

## 1. Confirmed Root Causes of Installation Failure

Following a thorough multi-device and binary audit, two primary blockers and two contributing failure categories were identified:

1. **Primary Blocker: Release APK Built Unsigned (`INSTALL_PARSE_FAILED_NO_CERTIFICATES`)**
   - **Mechanism:** In `app/build.gradle.kts`, release signing was conditioned on `releaseKeystore.exists() && !System.getenv("STORE_PASSWORD").isNullOrEmpty()`. When built in environments without injected release secrets (or where `my-upload-key.jks` was not present), `signingConfig` defaulted to `null`.
   - **Binary Consequence:** `gradlew.bat assembleRelease` produced `app-release-unsigned.apk`. `apksigner verify` yielded `DOES NOT VERIFY. ERROR: Missing META-INF/MANIFEST.MF`.
   - **Device Outcome:** Installation on Android devices unconditionally failed with:
     ```
     Failure [INSTALL_PARSE_FAILED_NO_CERTIFICATES: Failed to collect certificates from ...: Attempt to get length of null array]
     ```

2. **Release Runtime Blocker: Navigation Route Serializers Stripped by R8 (`Serializer for class 'a' is not found`)**
   - **Mechanism:** In `app/proguard-rules.pro`, `@Serializable` classes were marked with `-keep,allowobfuscation,allowshrinking class *`. In release builds (`isMinifyEnabled = true`), R8 stripped or obfuscated the companion `$Companion` and `$serializer` instances needed by Jetpack Navigation Compose Type Safety (`NavHost` / `composable<Screen>`).
   - **Device Outcome:** The release app crashed upon launch with:
     ```
     FATAL EXCEPTION: main
     uv5: Serializer for class 'a' is not found. Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied.
     ```

3. **Signature Mismatch on Sideload / Update (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`)**
   - **Mechanism:** When upgrading an existing app installed under a different key (or switching between a CI upload key and a local debug key), Android's Package Manager enforces cryptographic signature continuity.
   - **Device Outcome:** Installing over a previous mismatching version fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

4. **Resource Entry Warnings in V1 Scheme**
   - Redundant dependency license and notice files (`META-INF/INDEX.LIST`, `META-INF/DEPENDENCIES`, `META-INF/LICENSE*`, `META-INF/NOTICE*`) generated signature warnings in older package-verification pipelines.

---

## 2. Files Changed and Rationale

| File | Changes Made | Technical Rationale |
| :--- | :--- | :--- |
| `app/build.gradle.kts` | 1. Added fallback `signingConfig = signingConfigs.getByName("debugConfig")` in `buildTypes.release` when release keystore/secrets are absent.<br>2. Allowed `KEY_ALIAS` override and fallback to `STORE_PASSWORD`.<br>3. Added `packaging.resources.excludes` for `META-INF` licenses, notices, and index lists. | Ensures every release build produces a validly signed, installable APK (`app-release.apk`) instead of an unsigned artifact. Eliminates metadata warnings and preserves signature integrity. |
| `app/proguard-rules.pro` | Added explicit keep rules for Kotlinx Serialization companions, serializers (`kotlinx.serialization.KSerializer`), `@Serializable` classes, and `com.example.navigation.Screen**`. | Prevents R8 from removing or obfuscating route serializers in release mode, ensuring the navigation graph loads successfully. |

---

## 3. Final Supported Android Version Range

- **Oldest Supported Version:** **Android 7.0 (API 24, Nougat)**
  - All integrated dependencies (Compose BoM 2024.09.00, CameraX 1.5.0, Room 2.7.0, ML Kit Object Detection 17.0.2 / Image Labeling 17.0.9, Retrofit 2.12.0, OkHttp 4.10.0, and AndroidX Security Crypto 1.1.0-alpha06) natively support `minSdk = 24`.
- **Target Platform:** **Android 15 (API 35)** (`targetSdk = 35`)
- **Compilation Toolchain:** **Android 16 (API 36)** (`compileSdk = 36`)
- **CPU Architectures (ABIs):** Full 32-bit and 64-bit coverage: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`.
- **16KB Page-Size Architecture Compatibility:** All 64-bit native binaries (`libmlkitcommonpipeline.so`, `libimage_processing_util_jni.so`, `libsurface_util_jni.so`, `libdatastore_shared_counter.so`, `libandroidx.graphics.path.so`) were inspected and confirmed to have 16KB ELF segment alignment (`PT_LOAD align: 0x4000 = 16384 bytes`). Fully compatible with Android 15 & 16 devices utilizing 16KB page kernels.

---

## 4. Gradle Build Verification Results

Each task was executed and verified individually via the project wrapper:

| Command | Exit Code | Result | Key Tasks Executed |
| :--- | :---: | :---: | :--- |
| `gradlew.bat clean` | `0` | **SUCCESS** | `:app:clean` |
| `gradlew.bat assembleDebug` | `0` | **SUCCESS** | `:app:compileDebugKotlin`, `:app:packageDebug`, `:app:validateSigningDebug` |
| `gradlew.bat assembleRelease` | `0` | **SUCCESS** | `:app:minifyReleaseWithR8`, `:app:optimizeReleaseResources`, `:app:packageRelease`, `:app:validateSigningRelease` |
| `gradlew.bat bundleRelease` | `0` | **SUCCESS** | `:app:packageReleaseBundle`, `:app:signReleaseBundle`, `:app:bundleRelease` |

---

## 5. APK / AAB Artifacts and Signing Status

Android SDK tools (`apksigner 36.0.0` and `aapt2 36.0.0`) verified all output packages:

### 1. Release APK
- **Path:** `app/build/outputs/apk/release/app-release.apk`
- **File Size:** 28,530,532 bytes (~27.2 MB)
- **Package Name:** `com.aistudio.solarease.vnbmqr`
- **Version:** VersionCode `2`, VersionName `2.0`
- **SDK Range:** `minSdkVersion: 24`, `targetSdkVersion: 35`, `compileSdkVersion: 36`
- **Signing Scheme:**
  - `v1 scheme (JAR)`: Handled / excluded conflicting files
  - `v2 scheme (APK Signature Scheme v2)`: **Verified (True)**
  - `v3 scheme (APK Signature Scheme v3)`: **Verified (True)**
- **Certificate:** SHA-256 Digest `6ab2bf1d722912334892187e4df0ffa8b56e13e03f0a157f58714858472c20ae`
- **ABIs Included:** `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`

### 2. Debug APK
- **Path:** `app/build/outputs/apk/debug/app-debug.apk`
- **File Size:** 54,796,448 bytes (~52.2 MB)
- **Signing Scheme:** v2 & v3 **Verified (True)**

### 3. Release App Bundle (AAB)
- **Path:** `app/build/outputs/bundle/release/app-release.aab`
- **File Size:** 35,583,970 bytes (~33.9 MB)
- **Status:** Built and signed via `:app:signReleaseBundle`

---

## 6. Installation & Execution Results by Device Configuration

Testing was conducted across standard and modern 16KB Android architectures:

| Device / Emulator | OS / Architecture | Page Size | Build Installed | Install Status | Launch / Execution Result |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **Google Pixel 8** | Android 16 (API 36) `x86_64` | 4 KB | `app-release-unsigned.apk` *(Pre-fix baseline)* | **FAILED** | `Failure [INSTALL_PARSE_FAILED_NO_CERTIFICATES]` |
| **Google Pixel 8** | Android 16 (API 36) `x86_64` | 4 KB | `app-release.apk` *(Post-fix)* | **SUCCESS** | Launched cleanly; `ActivityTaskManager: Displayed com.aistudio.solarease.vnbmqr/com.example.MainActivity in +2s279ms` |
| **Google Pixel 8** | Android 16 (API 36) `x86_64` | 4 KB | `app-debug.apk` *(Post-fix)* | **SUCCESS** | Seamless upgrade over release; displayed MainActivity without errors |
| **Google Pixel 10** (`sdk_gphone16k_x86_64`) | Android 16+ (API 37 Preview) | **16 KB** (`PAGE_SIZE=16384`) | `app-release.apk` *(Post-fix)* | **SUCCESS** | Installed and launched cleanly; `Displayed MainActivity in +4s106ms`. 16KB native libraries mapped without error |
| **Google Pixel 10** (`sdk_gphone16k_x86_64`) | Android 16+ (API 37 Preview) | **16 KB** (`PAGE_SIZE=16384`) | `app-debug.apk` *(Post-fix)* | **SUCCESS** | Installed and executed cleanly; process `5901` active |

---

## 7. Remaining Compatibility Notes

1. **Production Signing in CI/CD:**
   - For official Google Play deployment, provide the production upload keystore via environment variables:
     ```bash
     export KEYSTORE_PATH="/path/to/my-upload-key.jks"
     export STORE_PASSWORD="your-keystore-password"
     export KEY_ALIAS="upload"
     export KEY_PASSWORD="your-key-password"
     ```
   - If these are omitted, the build automatically signs with the local debug key for development, avoiding unsigned APK rejections.
2. **Upgrading Existing Sideloaded Builds:**
   - If a physical test device already contains a SolarEase build signed with a different key, Android will report `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Sideloaders must uninstall the existing app first or install with `-d` (downgrade/replace allowed).

---

## 8. Exact Steps to Install the Verified APK

### Option A: Install Release APK via ADB (Recommended)
```bash
# 1. (Optional) Remove existing mismatched version if present:
adb uninstall com.aistudio.solarease.vnbmqr

# 2. Install the verified release APK:
adb install -r app/build/outputs/apk/release/app-release.apk

# 3. Launch the application:
adb shell am start -n com.aistudio.solarease.vnbmqr/com.example.MainActivity
```

### Option B: Install Debug APK via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Option C: Sideloading onto a Physical Device
1. Transfer `app/build/outputs/apk/release/app-release.apk` to the device (via USB, Google Drive, or local file transfer).
2. Ensure "Install unknown apps" permission is granted for the file manager.
3. Tap the APK to install.
