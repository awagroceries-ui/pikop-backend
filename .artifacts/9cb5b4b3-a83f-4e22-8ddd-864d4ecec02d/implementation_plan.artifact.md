# 📋 Implementation Plan: Redesign "How do you want to use Pikop?" Role Selection Screen

Redesign the role-selection screen (`UserTypeSelectionScreen.kt`) to match the approved glowing, 3D dimensional reference design, integrating transparent PNG role assets (`role_icon_send.png`, `role_icon_earn.png`, `role_icon_sell.png`, `role_icon_fleet_partner.png`, `role_icon_business_account.png`).

---

## 🔍 Requirements & Design Reference Alignment

1. **Asset Integration**:
   - Copy the 5 PNG role icon assets from `C:\Users\MOSES\AndroidStudioProjects\Pikop Assets\` into `app/src/main/res/drawable/`.
2. **Preserve Logo**:
   - The top Pikop logo (`R.drawable.pikop_logo`) must remain 100% untouched.
3. **Heading & Subtitle**:
   - Title: "How do you want to use **Pikop**?"
     - `"How do you want to use "` in white (`Color.White`).
     - `"Pikop?"` in brand green (`Color(0xFF00E676)`).
   - Subtitle: `"Choose your experience"` in muted grey (`Color(0xFF9CA3AF)`).
4. **Upgraded Role Cards**:
   - Icons: Render high-resolution PNG assets (`role_icon_*.png`) at prominent `90dp - 100dp` size at the top of each card, with zero extra background circles or tints.
   - Styling: Soft theme-colored borders and faint matching background tints matching each icon's color palette:
     - **Send**: Green border (`#10B981`), dark green background tint (`#052e16`).
     - **Earn**: Gold/Amber border (`#F59E0B`), dark amber background tint (`#451a03`).
     - **Sell**: Orange/Bronze border (`#F97316`), dark orange background tint (`#431407`).
     - **Fleet Partner**: Green border (`#10B981`), dark green background tint (`#052e16`).
     - **Business Account**: Blue border (`#0284C7`), dark blue background tint (`#082f49`).
   - Arrow Affordance: Small circular button in bottom-right corner with `Icons.Default.ArrowForward` tinted with the card's theme color.
   - Layout:
     - Row 1: **Send** (left) & **Earn** (right) - Two-column.
     - Row 2: **Sell** - Full-width.
     - Divider: **FOR ORGANIZATIONS** with horizontal rules.
     - Row 3: **Fleet Partner** (left) & **Business Account** (right) - Two-column.
5. **Organizational Grouping**:
   - Labeled divider between personal and corporate cards: `"FOR ORGANIZATIONS"` with thin horizontal rules.
6. **Supporting Footer Elements**:
   - Restyle `"Already have an account? Log in →"` with brand green underline and trailing arrow.
   - Trust signal line at the bottom: Shield icon + `"Your data is safe with us"` in muted grey.

---

## 🛠️ Proposed Changes

### Component 1: Resource Assets

#### [NEW] [role_icon_send.png](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/res/drawable/role_icon_send.png)
#### [NEW] [role_icon_earn.png](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/res/drawable/role_icon_earn.png)
#### [NEW] [role_icon_sell.png](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/res/drawable/role_icon_sell.png)
#### [NEW] [role_icon_fleet_partner.png](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/res/drawable/role_icon_fleet_partner.png)
#### [NEW] [role_icon_business_account.png](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/res/drawable/role_icon_business_account.png)
- Copy all 5 PNG files into `app/src/main/res/drawable/`.

---

### Component 2: Screen Composable (`UserTypeSelectionScreen.kt`)

#### [MODIFY] [UserTypeSelectionScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/UserTypeSelectionScreen.kt)
- Redesign `UserTypeSelectionScreen` and `RoleCard` composables according to design spec.
- Ensure all click callbacks (`CUSTOMER`, `FULFILLER`, `MERCHANT`, `FLEET_PARTNER`, `CORPORATE`, `LOGIN`) remain intact.

---

## 🧪 Verification Plan

### Automated & Manual Verification
1. Verify logo is unchanged and pixel-identical.
2. Verify all 5 PNG icon assets render crisply at `95dp` size.
3. Test tapping each card -> verify navigation opens the correct onboarding form.
4. Test tapping "Log in →" -> opens login screen.
5. Rebuild debug APK (`gradle_build("app:assembleDebug")`) and deploy to connected **Samsung Galaxy S23 Ultra** (`192.168.1.2:42447`).
6. Stage, commit, and push changes to GitHub `main`.
