# Task List - Resolve Android 16 Launch Crash & Hilt/KSP

- [/] Verify Hilt Gradle plugin order and KSP configuration in app `build.gradle.kts` and root `build.gradle.kts`
- [ ] Perform clean build with `--no-daemon` to check for `ClassNotFoundException: com.ng.pikop.PikopApp`
- [ ] Inspect APK / build output for DEX inclusion and native library alignment (Android 16 compatibility)
- [ ] Test app launch on device/emulator
- [ ] Create walkthrough artifact summarizing fixes and verification results
