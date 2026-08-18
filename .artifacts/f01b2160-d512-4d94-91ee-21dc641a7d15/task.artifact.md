# Tasks: DIDIT to DOJAH Migration

- [ ] Build Configuration & Dependencies
    - [ ] Update `libs.versions.toml` with Hilt and Dojah
    - [ ] Update Root `build.gradle.kts`
    - [ ] Update App `build.gradle.kts`
    - [ ] Update `settings.gradle.kts` (Add JitPack, Remove DIDIT repo)
- [ ] Dependency Injection Setup
    - [ ] Create `AppModule.kt`
    - [ ] Create `KycModule.kt`
- [ ] Core Logic Implementation
    - [ ] Create `KycManager.kt` interface
    - [ ] Create `DojahKycRepository.kt`
    - [ ] Update `PikopApp.kt` (@HiltAndroidApp)
    - [ ] Update `MainActivity.kt` (@AndroidEntryPoint)
- [ ] UI Refactoring
    - [ ] Create `KycViewModel.kt`
    - [ ] Refactor `KycUploadScreen.kt` to use ViewModel and KycManager
- [ ] Cleanup
    - [ ] Verify build and functionality
