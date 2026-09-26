# 📋 Implementation Plan: Restore Original Sample Design Proportions & Dual Light/Dark High Contrast

Restore the spacious, premium proportions and card heights from the original design sample (`Design sample.png`), while ensuring all text (especially grey text) is optimized for 100% high-contrast legibility in both light and dark theme modes.

---

## 🔍 Design Reference Proportions (Matching `Design sample.png`)

1. **Logo & Header**:
   - Logo: Prominent `90dp` height.
   - Heading: `26sp` ExtraBold title ("How do you want to\nuse **Pikop?**").
   - Subtitle: `14sp` ("Choose your experience").
   - Spacing: `16dp - 24dp` breathable vertical margins.

2. **Glow Cards (Role Cards)**:
   - **2-Column Cards (Send, Earn, Fleet Partner, Business Account)**:
     - Card height: `155dp` (spacious, rounded `20.dp` shape).
     - PNG icon size: `80dp` large transparent 3D badge.
     - Title `17sp` ExtraBold, Subtitle `11sp` / `12sp`.
     - Bottom-right circular arrow button: `28dp` diameter.
   - **Full-Width Card (Sell)**:
     - Card height: `90dp`.
     - PNG icon size: `72dp` transparent 3D badge.
     - Title `18sp` ExtraBold, Subtitle `12sp`.
     - Center-right circular arrow button: `32dp` diameter.

3. **Organizational Grouping Divider**:
   - `"FOR ORGANIZATIONS"` with thin horizontal rules, padded `vertical = 16.dp`.

4. **Footer & Trust Signal**:
   - Log In Link: `"Already have an account? Log in →"` (`14sp`).
   - Trust Signal: Shield icon + `"Your data is safe with us"` (`12sp`).

5. **Dual Light/Dark Theme Contrast Optimization**:
   - **Dark Mode**: Background `Color(0xFF0D0E11)`, titles `Color.White`, subtitles/grey text `Color(0xFFCBD5E1)` (light slate, 100% readable).
   - **Light Mode**: Background `Color(0xFFF8FAFC)`, titles `Color(0xFF0F172A)`, subtitles/grey text `Color(0xFF475569)` (dark slate, 100% readable).

---

## 🛠️ Proposed Changes

### Component 1: Screen Composable (`UserTypeSelectionScreen.kt`)

#### [MODIFY] [UserTypeSelectionScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/UserTypeSelectionScreen.kt)
- Restore spacious sample proportions (card heights `155dp` / `90dp`, logo `90dp`, icon sizes `80dp` / `72dp`).
- Apply theme-aware high-contrast color palette (`#CBD5E1` in dark mode, `#475569` in light mode).

---

## 🧪 Verification Plan

### Automated & Manual Verification
1. Test on connected **Samsung Galaxy S23 Ultra** (`192.168.1.2:42447`).
2. Compare with `Design sample.png` -> verify card proportions, icon sizes, and typography match the reference sample image.
3. Test in Dark Theme & Light Theme -> verify all text (especially grey text) is crisp, bold, and 100% legible.
4. Rebuild debug APK (`gradle_build("app:assembleDebug")`) and deploy to device.
5. Stage, commit, and push changes to GitHub `main`.
