# Implementation Plan - Expansion to Abuja and Lagos

Enable full service coverage in Abuja and Lagos by updating the app's location defaults, adding a city selector, and ensuring the map picker adapts to the user's selected region.

## User Review Required

> [!IMPORTANT]
> - **Location Agnostic Backend**: Your current backend engine already supports nationwide coverage because it uses coordinates and radius matching (PostGIS). No backend changes are required for this expansion.
> - **Google Maps API**: Ensure your Google Maps API key has no billing restrictions for high-volume geocoding in multiple cities.

## Proposed Changes

### UI & Location Intelligence (Android)

#### [MODIFY] [MapPickerSheet.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/MapPickerSheet.kt)
- **City Selector**: Add a horizontal scrollable row of "Quick City" chips (Port Harcourt, Lagos, Abuja) at the top of the map.
- **Dynamic Camera**: When a city chip is clicked, the map will automatically animate to that city's center point.
- **User Location Integration**: Use the `FusedLocationProviderClient` to try and center the map on the user's actual current location first.

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- Pass the "Preferred City" context to the map picker to ensure it opens in the right region.

---

### Location Constants
I will define the following center points for the expansion:
- **Port Harcourt**: `4.8156, 7.0498`
- **Lagos (Ikeja)**: `6.5244, 3.3792`
- **Abuja (Wuse)**: `9.0578, 7.4951`

---

## Verification Plan

### Manual Verification
- Open the Map Picker and click the **"Lagos"** chip; verify the map moves to Lagos.
- Open the Map Picker and click the **"Abuja"** chip; verify the map moves to Abuja.
- Ensure the **Reverse Geocoding** (finding address from pin) still works accurately in all three cities.
- Verify that the **Address Autocomplete** (suggestions) shows results for Lagos and Abuja when searched.
