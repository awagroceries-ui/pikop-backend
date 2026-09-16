# Implementation Plan - Brand Color Restore & Module Icon Redesign

This plan restores the Pikop brand identity by applying the official palette (Green, Lemon Green, Gold, Orange) and redesigning the home screen module icons for better visual clarity and appeal.

## User Review Required

> [!IMPORTANT]
> **Extracted Brand Palette**
> I have confirmed the following hex values from the brand assets:
> - **Primary Green:** `#008751`
> - **Lemon Green:** `#B2D732`
> - **Brand Gold:** `#FFC618`
> - **Brand Orange:** `#FF6900`
>
> I will apply these across the Material 3 theme.

## Proposed Changes

### 1. Brand Palette & Theming

#### [MODIFY] [Color.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/ui/theme/Color.kt)
- Add `PikopLemonGreen` (`0xFFB2D732`).
- Ensure all official colors are correctly defined.

#### [MODIFY] [Theme.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/ui/theme/Theme.kt)
- Update `BrandColorScheme` to use:
    - `primary`: `PikopGreen`
    - `secondary`: `PikopGold`
    - `tertiary`: `PikopOrange`
    - `surfaceVariant`: `PikopLemonGreen` (light alpha)

### 2. Home Screen Module Redesign

#### [MODIFY] [CustomerHomeScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/CustomerHomeScreen.kt)
- **Redesign `PrimaryModuleCard`**:
    - Update background colors to use the brand palette (Green for Dispatch, Orange for Food, Gold for Shop, Lemon for Groceries).
    - Replace generic icons with high-quality Material equivalents:
        - **Dispatch:** `DeliveryDining` (Express Rider)
        - **Food:** `Restaurant` (Meal/Plate)
        - **Groceries:** `LocalGroceryStore` (Basket)
        - **Shop:** `ShoppingBag` (Marketplace)
    - **Visual Polish:** Add subtle gradients and ensure icons are large, padded, and not clipped.
    - **Full Color Icons:** Use multi-colored icon rendering by layering or applying distinct tints to the icon and its container.

### 3. Admin Dashboard Consistency

#### [MODIFY] [layout.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/layout.ejs)
- Update CSS variables (`--green`, `--gold`, `--orange`) to match the exact extracted hex values.
- Add `--lemon-green` for consistent use in the admin portal.

## Verification Plan

### Automated/Code Verification
- Verify successful Gradle build of the Android app.
- Check accessibility contrast ratios programmatically (or via Lint).

### Manual Verification
1.  **Side-by-Side Check:** Compare the app colors against `pikop_logo.png` to ensure a perfect match.
2.  **Home Screen Audit:** Confirm the four module buttons are vibrant, colorful, and clearly identifiable.
3.  **UI Consistency:** Navigate through the app (Wallet, Settings, Orders) to confirm the "grey drift" has been replaced by the brand palette.
