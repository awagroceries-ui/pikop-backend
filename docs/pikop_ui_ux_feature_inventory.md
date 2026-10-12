# Comprehensive Feature & UI/UX Component Inventory - Pikop Platform

> **PURPOSE OF THIS INVENTORY**:
> This document provides a complete, exhaustive breakdown of every screen, component, modal dialog, user role, navigation hierarchy, and backend subsystem in the **Pikop Multi-Platform Commerce & Logistics Engine**. It is designed as the master reference blueprint for a full **UI/UX Overhaul and Redesign**.

---

## 1. System Overview & Core Ecosystem Architecture

Pikop is a unified, multi-platform ecosystem servicing on-demand logistics, multi-vendor commerce (Food, Groceries, Shop), and fleet/corporate management across **6 primary user roles**:

- **1. Customer**: Places on-demand delivery orders, explores local storefronts, manages cart, tracks live drivers, pays via Paystack/Wallet/Escrow.
- **2. Fulfiller (Agent/Rider/Driver)**: Accepts/queues delivery missions, tracks GPS routes, captures POD photos/codes, earns 75% fare split.
- **3. Merchant (Vendor/Kitchen)**: Manages store profile, inventory/menu items, bulk CSV dispatch, sales analytics, and operating hours.
- **4. Fleet Partner**: Manages driver rosters, fleet earnings, and vehicle assignments.
- **5. Corporate Business**: Manages employee delivery allowances, department quotas, and corporate billing statements.
- **6. Super Admin (Web Console)**: Oversees system GMV, driver/merchant verification queues, store item regulation, dispute resolution, and promo generation.

---

## 2. Global Navigation & Layout Architecture

### A. Main App Shell (`MainActivity.kt` & `MainAppScaffold`)
* **Root Navigation Host**: Manages full-screen modals, authentication flows, and mission execution screens.
* **Nested Navigation Host (`MainAppScaffold`)**: Manages role-based bottom navigation tabs.
* **Bottom Navigation Bars by Role**:
  * **Customer**: `Home` (Discover) | `Orders` (Logistics & Commerce History) | `Wallet` (Balance & Topup) | `Menu` (Account & Settings)
  * **Fulfiller**: `Home` (Radar & Offers) | `Missions` (Active & History) | `Wallet` (75% Share Earnings) | `Menu` (Profile & Vehicle)
  * **Merchant**: `Sales` (Dashboard) | `Items` (Inventory) | `Insights` | `Returns` | `Promos` | `Bulk`
  * **Fleet Partner**: `Overview` (Fleet Stats) | `Fulfillers` (Driver Roster) | `Earnings` | `Settings`
  * **Corporate**: `Dashboard` (Allowance & Use) | `Missions` | `Employees` | `Billing`

---

## 3. Exhaustive Screen & Component Inventory

### Module 1: Onboarding, Authentication & Verification

| Screen Name | File Path | Key UI Elements & UX Components | Interactions & State |
| :--- | :--- | :--- | :--- |
| **Splash Screen** | `feature/auth/SplashScreen.kt` | Animated Pikop Logo, Progress Indicator | Automatic token check, role detection, route branching |
| **User Type Selection** | `feature/auth/UserTypeSelectionScreen.kt` | 5 Color-coded Role Cards (Customer, Agent, Merchant, Fleet, Corporate) | Role selection, navigation to specific signup flow |
| **Fulfiller Category Selection** | `feature/auth/FulfillerCategorySelectionScreen.kt` | Mobility cards: Foot Agent, Rider (Bicycle/Bike), Driver (Car/Van/Truck) | Category selection, passes asset icons & primary class |
| **Login Screen** | `feature/auth/LoginScreen.kt` | Email/Phone input, Password input, Forgot Password link | Form validation, JWT auth, error banners |
| **Forgot Password Screen** | `feature/auth/ForgotPasswordScreen.kt` | 2-Step OTP Verification form, New password inputs | Dispatches SMS (Africa's Talking) and Email OTPs |
| **Signup Customer** | `feature/auth/SignupCustomerScreen.kt` | Full name, Email, Phone, Password, Referral Code input | Terms checkbox, SMS OTP initiation |
| **Signup Fulfiller** | `feature/auth/SignupFulfillerScreen.kt` | Class-specific registration inputs, emergency contact | Class validation (Agent/Rider/Driver) |
| **Signup Merchant** | `feature/auth/SignupMerchantScreen.kt` | Manager contact details, store owner role selection | Onboarding Step 1 |
| **Merchant Business Setup** | `feature/auth/MerchantBusinessSetupScreen.kt` | Store Name, Category Dropdown (with Custom input), Interactive Map Address Picker, CAC/NAFDAC, Paystack Bank resolver | Onboarding Step 2: Auto-resolves Paystack account names, captures exact PostGIS map coordinates |
| **Signup Fleet Partner** | `feature/auth/SignupFleetPartnerScreen.kt` | Fleet size, vehicle tier selector, business details | Fleet partner onboarding Step 1 |
| **Fleet Business Setup** | `feature/auth/FleetPartnerBusinessSetupScreen.kt` | Fleet CAC, bank payout resolver | Fleet partner onboarding Step 2 |
| **Signup Corporate** | `feature/auth/SignupCorporateScreen.kt` | Corporate manager name, business domain | Corporate onboarding Step 1 |
| **Corporate Business Setup** | `feature/auth/CorporateBusinessSetupScreen.kt` | CAC number, employee quota, billing settings | Corporate onboarding Step 2 |
| **Email/SMS OTP Screen** | `feature/auth/EmailOtpScreen.kt` | 6-Digit OTP input field, Resend timer | Auto-advances upon 6 digits, verifies code |

---

### Module 2: Customer Experience & On-Demand Logistics

| Screen Name | File Path | Key UI Elements & UX Components | Interactions & State |
| :--- | :--- | :--- | :--- |
| **Customer Home** | `feature/order/CustomerHomeScreen.kt` | Quick Action Cards (New Delivery, Food, Groceries, Shop), Active Mission Banner, Live City Rule Banner | Weather/Traffic surcharge warnings, quick launcher |
| **Order Quote / Creation** | `feature/order/OrderQuoteScreen.kt` | Pickup & Dropoff Address Pickers, Package Tier Selector (Envelope, Small, Medium, Large, Heavy), Item Description, Photo Capture, Recipient Contact, Promo Code Field, Fee Breakdown | Instant distance & fare calculation, coupon validation, order dispatch creation |
| **Map Address Search** | `feature/order/MapAddressSearchScreen.kt` | Google Map view, Draggable Map Pin, Location Search Bar (Places API), "Use Current GPS Location" FAB | Returns `address`, `lat`, `lng`, `state` via `savedStateHandle` |
| **Saved Addresses** | `feature/order/SavedAddressesScreen.kt` | Address Cards list (Home, Work, Custom), Add Address FAB | Manage pre-saved dispatch addresses |
| **Saved Recipients** | `feature/order/RecipientManagementScreen.kt` | Recipient Contact Cards, Quick Select | Fast address book selection during quote |
| **Track Order** | `feature/order/TrackOrderScreen.kt` | Live Google Map with real-time Fulfiller GPS marker, Status Timeline (Searching, Matched, Picked Up, Delivered), Fulfiller Profile Card, In-App Call/Chat buttons, Proof of Delivery Photo viewer | Real-time Socket.io updates, FCM notifications |
| **Order Acknowledgment** | `feature/order/OrderAcknowledgmentScreen.kt` | Order Summary Card, Confirmation Button | Receiver confirmation of delivered package |
| **Orders Dashboard** | `feature/order/OrdersDashboardScreen.kt` | Tabbed History (Active, Past, Cancelled), Order Status Badges, Re-Order Button | Filterable list of all user missions |

---

### Module 3: Commerce, Storefronts & Multi-Vendor Cart

| Screen Name | File Path | Key UI Elements & UX Components | Interactions & State |
| :--- | :--- | :--- | :--- |
| **Food Storefront** | `feature/commerce/FoodStorefrontScreen.kt` | Cuisine Filter Chips, Search Bar, 2-Column Meal Grid, Bottom Cart Floating Bar | Single-vendor cart enforcement, distance badges |
| **Grocery Storefront** | `feature/commerce/GroceryStorefrontScreen.kt` | Category Filter Chips, Fresh Produce grid, Cart Banner | Single-vendor grocery shopping |
| **Shop Storefront** | `feature/commerce/ShopStorefrontScreen.kt` | Electronics, Beauty, Fashion Grid, Bottom Cart Bar | Merchant product discovery |
| **Unified Storefront** | `feature/commerce/StorefrontScreen.kt` | City Availability Banner, Waitlist Modal Dialog, Discovery Item Cards | Handles coming-soon cities & local merchant discovery |
| **Cart Screen** | `feature/commerce/CartScreen.kt` | Cart Item Rows (Quantity +/- Controls, Remove Item), Total Price Summary, Checkout Button | Modifies `CartManager.items` in real-time |
| **Commerce Checkout** | `feature/commerce/CommerceCheckoutScreen.kt` | Selected Item/Cart Header, Delivery Address Selector, Delivery Schedule (Deliver Now vs Schedule), Payment Method Cards (Pay Now Card, Wallet, Cash on Delivery Escrow), Promo Code Input, Fee Summary | Calculates dynamic delivery fee, platform fee, coupon discount, Paystack redirect |

---

### Module 4: Fulfiller / Agent Console & Mission Execution

| Screen Name | File Path | Key UI Elements & UX Components | Interactions & State |
| :--- | :--- | :--- | :--- |
| **Fulfiller Dashboard** | `feature/fulfiller/FulfillerDashboardScreen.kt` | Live Map Radar, Online/Offline Toggle, Demand Hotspot Heatmap Overlay, Active Mission Banner, Queued Mission Banner, Inbound Offer Cards | Real-time GPS pings, 5-sec offer polling, Socket.io listeners |
| **Active Order / Mission** | `feature/fulfiller/ActiveOrderScreen.kt` | Route Map (Pickup to Dropoff), Stepper Controls (Arrived at Pickup, Item Picked Up, Arrived at Dropoff, Complete Delivery), Proof of Delivery Camera Launcher, 4-Digit Delivery Code Verification, Incident Report Button, Emergency SOS FAB | Process death URI persistence, camera selfie/POD capture, auto-queue promotion |
| **Mission Records** | `feature/fulfiller/FulfillerOrdersScreen.kt` | Tabbed Mission History: `Active`, `Queued`, `Completed`; Lifetime Agent Earnings Banner (75% Share) | Self-healing queue promotion, mission resume |
| **KYC Upload Screen** | `feature/fulfiller/KycUploadScreen.kt` | 7-Step Verification Wizard: 0. Personal, 1. Category, 2. Profile Photo (Front Camera Selfie), 3. Identity Verification, 4. Vehicle, 5. Operational Documents, 6. Bank Payout, 7. Final Submission | Integrated Prembly / Didit inline widget, EXIF auto-rotation camera capture |
| **Agent Insights** | `feature/fulfiller/InsightsScreen.kt` | Weekly Earnings Graph, Completion Rate %, Rating Star breakdown, Peak Hour Bonus tracker | Performance analytics & agent gamification |

---

### Module 5: Merchant Seller Center & Store Management

| Screen Name | File Path | Key UI Elements & UX Components | Interactions & State |
| :--- | :--- | :--- | :--- |
| **Merchant Portal** | `feature/merchant/MerchantPortalScreen.kt` | Seller Center Tabs: Sales, Items, Insights, Returns, Promos, Bulk, Settings; Store Details Card, Operating Hours Form, Promotion Link Share | Complete merchant control center |
| **Add/Edit Item** | `feature/merchant/AddEditProductScreen.kt` | Image Picker, Name, Price, Description, Category Dropdown (`ExposedDropdownMenuBox` with preset categories + custom input), Stock/Unit/NAFDAC inputs | Product & Menu Item listing creation |
| **Bulk Dispatch** | `feature/merchant/BulkDispatchScreen.kt` | CSV / JSON Batch Import, Multi-Delivery Mission List, Mass Dispatch Button | High-volume corporate & merchant delivery creation |
| **Merchant Analytics** | `feature/merchant/MerchantAnalyticsScreen.kt` | Top Selling Items, Sales Revenue Charts, Order Volume Trends | Store financial performance |

---

### Module 6: Fleet Partner & Corporate Business Portals

| Screen Name | File Path | Key UI Elements & UX Components | Interactions & State |
| :--- | :--- | :--- | :--- |
| **Fleet Dashboard** | `feature/fleetpartner/FleetPartnerDashboardScreen.kt` | Fleet Overview Cards (Total Drivers, Active Missions, Total Earnings), Driver Roster Table, Commission Overrides | Fleet management & driver monitoring |
| **Corporate Dashboard** | `feature/order/CorporateDashboardScreen.kt` | Allowance Tracker, Employee Quotas List, Department Expense Charts, Bulk Billing Statements | Corporate logistics allowance management |

---

### Module 7: Financials, Wallet & Payouts

| Screen Name | File Path | Key UI Elements & UX Components | Interactions & State |
| :--- | :--- | :--- | :--- |
| **Wallet Screen** | `feature/wallet/WalletScreen.kt` | Balance Card (Actual vs Escrowed Balance), Quick Actions (Topup Wallet, Withdraw Funds, Transfer), Transaction Ledger History (Filtered by All, Credit, Debit) | Paystack inline topup, instant balance refresh |
| **Withdrawal Screen** | `feature/wallet/WithdrawalScreen.kt` | Saved Bank Account Card, Amount Input, Instant Payout Button | Triggers automated Paystack Transfer payout |
| **Growth & Rewards** | `feature/growth/GrowthRewardsScreen.kt` | Referral Code Share Card, Loyalty Points Ledger, Points-to-Cash Redemption Card | Converts loyalty points to wallet credit |

---

### Module 8: Communication, Support & Safety Hub

| Screen Name | File Path | Key UI Elements & UX Components | Interactions & State |
| :--- | :--- | :--- | :--- |
| **Chat Screen** | `feature/chat/ChatScreen.kt` | Real-time Message Stream, Text Input, Send Button, Quick Presets ("I'm at the location", etc.) | Socket.io real-time chat between Customer, Fulfiller, and Merchant |
| **Support Hub** | `feature/auth/SupportHubScreen.kt` | Knowledge Base FAQ Accordion, Ticket Submission Modal, Support History | In-app customer support |
| **FAQ Detail** | `feature/auth/FaqDetailScreen.kt` | Markdown Article Reader, Helpful Rating Buttons | Self-service help articles |
| **Emergency SOS Dialog** | Built into `feature/fulfiller/ActiveOrderScreen.kt` | Prominent Red SOS Button, Emergency Contacts Alert Trigger | Dispatches immediate location alert & SMS |

---

### Module 9: Account Settings, Legal & System Management

| Screen Name | File Path | Key UI Elements & UX Components | Interactions & State |
| :--- | :--- | :--- | :--- |
| **Account Screen** | `feature/auth/AccountScreen.kt` | User Profile Card, Verified Badge, Referral Code Card, Options List (Edit Profile, Rewards, Manage Shop, Addresses, Recipients, KYC, Notifications, Help), Legal & Password Management | Navigation hub for user account |
| **Profile Edit** | `feature/auth/ProfileEditScreen.kt` | Name, Phone, Emergency Contact inputs, Paystack Bank resolver for Fulfillers | Profile updates & bank verification |
| **Notification Settings** | `feature/auth/NotificationSettingsScreen.kt` | Push Notification Switches (Order Updates, Promos, SMS) | Configures FCM & SMS preferences |
| **Session Management** | `feature/auth/SessionManagementScreen.kt` | Active Device Sessions List, "Revoke Other Sessions" Button | Revokes JWT refresh tokens |
| **Legal Viewer** | `feature/auth/LegalViewerScreen.kt` | Scrollable Terms & Conditions / Privacy Policy text | NDPA compliance & terms viewing |

---

### Module 10: Admin Web Console Inventory (`backend_v3/src/views`)

| Web View Name | File Path | Capabilities & Regulation Features |
| :--- | :--- | :--- |
| **Admin Dashboard** | `views/dashboard.ejs` | System GMV, Active Orders count, Active Fulfillers radar, Quick metrics |
| **Orders Control** | `views/orders.ejs` | Live order monitoring, manual agent dispatch override, refund trigger |
| **Live Map Tracking** | `views/admin_track.ejs` | Real-time agent GPS tracking & active route visualization |
| **Merchant Hub** | `views/merchants.ejs` | Unified seller registry, approve/suspend/delete stores, "VIEW ITEMS" launcher |
| **Store Contents** | `views/merchant_store_items.ejs` | Inspect all products/meals of a store, toggle active/disabled, edit price/name, delete item |
| **Vendor Management** | `views/vendors.ejs` | Manage marketplace shop vendors |
| **Kitchen Management** | `views/kitchens.ejs` | Manage food & meal kitchens |
| **Fulfiller Registry** | `views/fulfillers.ejs` | Agent roster, class filtering, manual activation/suspension |
| **Fulfiller Detail** | `views/fulfiller_detail.ejs` | Inspect driver KYC documents, rating history, vehicle registration |
| **KYC Queue** | `views/kyc_queue.ejs` | Pending identity verification applications |
| **KYC Review** | `views/kyc_review.ejs` | Approve/reject agent identity documents |
| **Customer Registry** | `views/customers.ejs` | Customer accounts & account deletion requests |
| **Coupon Generator** | `views/coupons_admin.ejs` | Create universal/store coupons (`TOTAL_BILL`, `DELIVERY_ONLY`, `MERCHANT_ONLY`) |
| **Dispute Resolution** | `views/disputes.ejs` | Escrow hold release, buyer/seller dispute resolution |
| **Emergency SOS** | `views/emergency_resolution.ejs` | Active panic alert monitoring and emergency response |
| **Financial Overview** | `views/financial_overview.ejs` | Platform fee revenue, 75/25 split ledger, total payouts |
| **Withdrawals** | `views/withdrawals.ejs` | Batch approve agent earnings payouts |
| **Audit Logs** | `views/audit_logs_admin.ejs` | Admin action logs & system safety audit |

---

## 4. Modal Dialogs & Bottom Sheet Inventory

1. **Change Password Dialog** (`feature/auth/ChangePasswordDialog.kt`)
2. **Delete Account Dialog** (3-step NDPA confirmation inside `feature/auth/AccountScreen.kt`)
3. **Fulfiller Profile Dialog** (`feature/fulfiller/FulfillerProfileDialog.kt`)
4. **Rating & Review Dialog** (`feature/fulfiller/RatingDialog.kt`)
5. **Incident Report Dialog** (inside `feature/fulfiller/ActiveOrderScreen.kt`)
6. **Waitlist Dialog** (inside `feature/commerce/StorefrontScreen.kt`)

---

## 5. Recommended UI/UX Overhaul Guidelines

### A. Design System & Color Palette
* **Primary Branding Color**: `PikopGreen` (`#008751` / `#388E3C` - Deep Forest / Emerald)
* **Secondary Accent**: `PikopGold` (`#FFC107` / `#FFB300` - Warm Gold)
* **Background Colors**: Surface `#F8F9FA` (Light) / `#121212` (Dark Theme)
* **Typography**: Expressive Material 3 Scale (Headline Bold, Label Medium, Body Elastic)

### B. UX Guidelines for Redesign
1. **Unified Design Tokens**: Standardize card corner radiuses (16.dp), button heights (52.dp), and padding (16.dp / 24.dp) across all screens.
2. **Micro-Interactions & Motion**:
   - Add smooth Compose shared element transitions between Storefront Item Cards and Checkout Header.
   - Use subtle haptic feedback on cart item increment/decrement and order placement.
3. **Skeleton Loading States**: Replace generic `CircularProgressIndicator` with animated shimmer skeleton loaders on Storefronts, Home Screen, and Wallet Ledger.
4. **Adaptive Scaffolding**: Support multi-window, foldable, and tablet layouts with adaptive navigation rails for tablets.
