# KYC Infrastructure Migration: DIDIT to DOJAH

This plan outlines the steps to migrate the identity verification system in the Pikop Android app from DIDIT SDK to DOJAH SDK, incorporating modern architecture with Hilt DI and Kotlin Coroutines.

## User Review Required

> [!IMPORTANT]
> The migration involves introducing Hilt Dependency Injection to a project that currently manages dependencies manually (via `remember` blocks in Composables). This is a significant architectural improvement but will change how `ApiService` and `TokenManager` are accessed.

> [!WARNING]
> The DOJAH SDK dependency `com.github.dojah-inc:sdk-kotlin:0.4.1` requires the JitPack repository. I will add this to the `settings.gradle.kts` file.

## Proposed Changes

### 1. Build Configuration & Dependencies

#### [MODIFY] [libs.versions.toml](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/gradle/libs.versions.toml)
- Add Hilt versions (plugin and library).
- Add Dojah SDK version (`0.4.1`).

#### [MODIFY] [build.gradle.kts (Root)](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/build.gradle.kts)
- Add Hilt Gradle plugin.

#### [MODIFY] [build.gradle.kts (:app)](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/build.gradle.kts)
- Apply Hilt plugin.
- Add Hilt dependencies (`hilt-android`, `hilt-compiler`).
- Add Dojah SDK dependency.
- Remove DIDIT SDK dependency.

#### [MODIFY] [settings.gradle.kts](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/settings.gradle.kts)
- Add JitPack repository to `dependencyResolutionManagement`.

### 2. Dependency Injection Setup [NEW]

#### [NEW] [AppModule.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/di/AppModule.kt)
- Provide `TokenManager`.
- Provide `ApiService` (refactored to be injected).
- Provide Dojah Credentials (`App ID`, `Public Key`) using `@Named`.

#### [NEW] [KycModule.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/di/KycModule.kt)
- Bind `KycManager` interface to `DojahKycRepository`.

### 3. Core Logic Refactoring

#### [NEW] [KycManager.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/kyc/KycManager.kt)
- Define interface for starting verification and handling results.

#### [NEW] [DojahKycRepository.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/kyc/DojahKycRepository.kt)
- Implement `KycManager` using Dojah SDK.
- Handle verification launch logic.

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Update `FulfillerProfileResponse` to include Dojah-specific status (if different from DIDIT).
- Remove Didit-specific methods if they are no longer needed (the backend likely needs to provide a Dojah session if applicable).

#### [MODIFY] [PikopApp.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/PikopApp.kt)
- Annotate with `@HiltAndroidApp`.
- Remove DIDIT initialization.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Annotate with `@AndroidEntryPoint`.

### 4. UI Refactoring

#### [NEW] [KycViewModel.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/KycViewModel.kt)
- Orchestrate KYC state and actions using `KycManager` and `ApiService`.

#### [MODIFY] [KycUploadScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/KycUploadScreen.kt)
- Refactor to use `KycViewModel` (via `hiltViewModel()`).
- Replace `DiditSdk` calls with `kycManager.startVerification(...)`.

## Verification Plan

### Automated Tests
- Build the project to ensure Hilt and Dojah dependencies are correctly resolved.
- Unit tests for `DojahKycRepository` (mocking `ApiService` and `DojahSdk`).

### Manual Verification
- Deploy to emulator.
- Navigate to the KYC Upload Screen.
- Verify that clicking "Verify Identity" launches the Dojah widget.
- Verify that the app handles the "approved" status correctly (e.g., advancing to the next step).
