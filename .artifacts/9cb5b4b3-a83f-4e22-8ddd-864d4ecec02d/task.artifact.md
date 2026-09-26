# 📌 Task Checklist: Compact Role Selection Screen (No-Scroll Viewport) & Dual Light/Dark Theme High-Contrast Legibility

- `[/]` Task 1: Compact Layout Sizing in `UserTypeSelectionScreen.kt`
  - `[ ]` Reduce logo size to `60dp` and header typography to `20sp`
  - `[ ]` Reduce 2-column card heights to `115dp` and icon sizes to `58dp`
  - `[ ]` Reduce full-width card height to `68dp` and icon size to `52dp`
  - `[ ]` Compact vertical padding on divider and footer elements (~520dp total height)

- `[ ]` Task 2: Dual Light & Dark Theme High-Contrast Legibility
  - `[ ]` Implement `isSystemInDarkTheme()` color resolution for titles, subtitles, grey text, and card backgrounds
  - `[ ]` Dark mode: Deep dark background, white title, `#CBD5E1` light slate grey text, green accent `#00E676`
  - `[ ]` Light mode: Light background, `#0F172A` title, `#475569` dark slate grey text, green accent `#008751`

- `[ ]` Task 3: Build & Deploy to Device
  - `[ ]` Build debug APK (`app:assembleDebug`)
  - `[ ]` Install and launch on device

- `[ ]` Task 4: Git Automation
  - `[ ]` Stage, commit, and push changes to GitHub `main`
