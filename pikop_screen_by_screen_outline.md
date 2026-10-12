# Complete Screen-by-Screen Content & Wireframe Outline - Pikop Mobile App

This document provides a granular, screen-by-screen content outline for the entire Pikop Android mobile application. Each entry details the **Screen Title**, **Route Identifier**, **Layout Structure**, **UI Components & Inputs**, **Interactive Buttons/Actions**, **Empty & Error States**, and **Connected Navigation Flows**.

---

## Part 1: Onboarding, Authentication & Profile Setup

### 1.1 Splash Screen
* **Route**: `splash`
* **File**: `feature/auth/SplashScreen.kt`
* **Layout Structure**: Centered vertical column with dark branding background (`PikopNearBlack`).
* **UI Components**:
  * Animated Pikop Logo (`R.drawable.pikop_logo`) with scale and alpha animation.
  * Loading Circular Progress Bar or subtle pulsing indicator.
  * Version text at bottom (`v1.1.7`).
* **User Actions & Logic**:
  * Checks saved JWT tokens in `TokenManager`.
  * If token valid $\rightarrow$ routes to `main` (Home).
  * If token expired/absent $\rightarrow$ routes to `user_type_selection`.

### 1.2 User Type Selection Screen
* **Route**: `user_type_selection`
* **File**: `feature/auth/UserTypeSelectionScreen.kt`
* **Layout Structure**: Top branding header + vertical list of 5 interactive role selection cards.
* **UI Components**:
  * **Customer Card**: Emerald icon, title "Customer / Shopper", subtext "Send packages, order meals & shop local stores".
  * **Dispatch Agent Card**: Gold icon, title "Fulfiller / Agent", subtext "Deliver packages, complete missions & earn 75% fare split".
  * **Merchant Card**: Storefront icon, title "Store Owner / Merchant", subtext "Sell meals, groceries or products & dispatch bulk orders".
  * **Fleet Partner Card**: Truck icon, title "Fleet Manager", subtext "Manage driver rosters, track fleet earnings & vehicle assignments".
  * **Corporate Card**: Business icon, title "Corporate Business", subtext "Manage employee delivery allowances & corporate billing".
* **User Actions**:
  * Tapping any card navigates to the respective signup flow (`signup_customer`, `fulfiller_category_selection`, `signup_merchant`, `signup_fleet`, `signup_corporate`).
  * "Already have an account? Sign In" text button at bottom $\rightarrow$ routes to `login`.

### 1.3 Fulfiller Category Selection Screen
* **Route**: `fulfiller_category_selection`
* **File**: `feature/auth/FulfillerCategorySelectionScreen.kt`
* **Layout Structure**: Header + 3 mobility class cards.
* **UI Components**:
  * **Foot Agent Card**: Walking icon, "Foot Agent / Cyclist" title, "Ideal for short-distance deliveries in dense urban areas".
  * **Rider Card**: Bike icon, "Rider (Bicycle / Motorbike)", "Ideal for fast town-wide parcel & food deliveries".
  * **Driver Card**: Car/Truck icon, "Driver (Car / Van / Truck)", "Ideal for heavy packages, furniture & bulk moves".
* **User Actions**:
  * Selecting a mobility category passes the selected category parameter $\rightarrow$ routes to `signup_fulfiller/{category}`.

### 1.4 Login Screen
* **Route**: `login`
* **File**: `feature/auth/LoginScreen.kt`
* **Layout Structure**: Top logo + Form column + Social/Signup links.
* **UI Components**:
  * Email / Phone Outlined Input Field with leading Mail icon.
  * Password Outlined Input Field with leading Lock icon and Password Visibility toggle.
  * "Forgot Password?" text link.
  * "Sign In" Primary Button (Green, full width).
  * Error Banner (Red container for invalid credentials or network errors).
* **User Actions**:
  * "Sign In" button triggers JWT authentication API call.
  * On success $\rightarrow$ stores JWT tokens and routes to `main`.
  * "Forgot Password?" $\rightarrow$ routes to `forgot_password`.
  * "Don't have an account? Register" $\rightarrow$ routes to `user_type_selection`.

### 1.5 Forgot Password Screen
* **Route**: `forgot_password`
* **File**: `feature/auth/ForgotPasswordScreen.kt`
* **Layout Structure**: 2-Step Wizard.
* **Step 1 Components**: Email/Phone input field, "Send Reset OTP" button.
* **Step 2 Components**: 6-digit OTP input field, New Password input field, Confirm Password input field, "Reset Password" button.
* **User Actions**:
  * Dispatches SMS (Africa's Talking) and Email OTPs.
  * Verifies 6-digit code and updates password in backend. On success $\rightarrow$ routes to `login`.

### 1.6 Signup Customer Screen
* **Route**: `signup_customer`
* **File**: `feature/auth/SignupCustomerScreen.kt`
* **Layout Structure**: Onboarding form column + Legal consent checkbox.
* **UI Components**:
  * Full Name Outlined Input Field.
  * Business / Personal Email Input Field.
  * Phone Number Input Field (+234 format).
  * Password & Confirm Password Input Fields.
  * Referral Code Input Field (Optional, e.g. `PIKOP-250`).
  * "I agree to Terms & Conditions and Privacy Policy" Checkbox & Clickable text link.
  * "Create Customer Account" Primary Button.
* **User Actions**:
  * Triggers signup API. On success $\rightarrow$ routes to `email_otp/{email}/CUSTOMER`.

### 1.7 Signup Fulfiller Screen
* **Route**: `signup_fulfiller/{category}`
* **File**: `feature/auth/SignupFulfillerScreen.kt`
* **Layout Structure**: Class-specific onboarding form column.
* **UI Components**:
  * Full Name, Email, Phone, Password fields.
  * Selected Class Badge (`FOOT_AGENT`, `RIDER`, `DRIVER`).
  * Emergency Contact Name & Phone Input Fields.
  * Driving License Number Field (Conditional: Only visible if `DRIVER`).
  * "Create Agent Account" Button.
* **User Actions**:
  * Registers fulfiller profile. On success $\rightarrow$ routes to `email_otp/{email}/FULFILLER`.

### 1.8 Signup Merchant Screen (Step 1)
* **Route**: `signup_merchant`
* **File**: `feature/auth/SignupMerchantScreen.kt`
* **Layout Structure**: Contact Person Onboarding Form.
* **UI Components**:
  * Contact Person Full Name Input.
  * Role Input Field (e.g. Owner, Store Manager).
  * Business Email & Phone Fields.
  * Password & Confirm Password Fields.
  * "Continue to Business Setup" Button.
* **User Actions**:
  * Registers merchant user account. On success $\rightarrow$ routes to `merchant_business_setup`.

### 1.9 Merchant Business Setup Screen (Step 2)
* **Route**: `merchant_business_setup`
* **File**: `feature/auth/MerchantBusinessSetupScreen.kt`
* **Layout Structure**: Business details & payout configuration form.
* **UI Components**:
  * Store / Business Name Input Field.
  * Category Dropdown (`ExposedDropdownMenuBox` with Food, Groceries, Electronics, Fashion, Beauty, Household, Custom option).
  * Store Address Picker Card: Clickable `OutlinedCard` displaying selected map address, location icon, and "Tap to pick exact location on map" subtext.
  * CAC Registration Number Field (Optional).
  * NAFDAC Registration Number Field (Conditional for Food/Groceries).
  * Paystack Payout Bank Dropdown (Searchable bank list).
  * 10-Digit Account Number Field with auto-resolving Account Name indicator badge.
  * "Accept Pay on Delivery" Switch toggle.
  * "Complete Merchant Setup" Button.
* **User Actions**:
  * Tapping Store Address Card $\rightarrow$ launches `map_address_search/Store Location/store`.
  * Auto-resolves Paystack bank account holder name upon typing 10 digits.
  * Submits PostGIS map coordinates and merchant profile $\rightarrow$ routes to `main`.

### 1.10 Email / SMS OTP Verification Screen
* **Route**: `email_otp/{email}/{role}`
* **File**: `feature/auth/EmailOtpScreen.kt`
* **Layout Structure**: Centered OTP input card + Resend timer.
* **UI Components**:
  * Email recipient indicator text.
  * 6-Digit Pin Input Field.
  * "Resend Code" Timer text link.
  * "Verify & Continue" Button.
* **User Actions**:
  * Auto-submits upon typing 6th digit. Verifies OTP and navigates to `main`.

---

## Part 2: Customer On-Demand Logistics & Trackers

### 2.1 Customer Home Screen
* **Route**: `home` (inside `MainAppScaffold`)
* **File**: `feature/order/CustomerHomeScreen.kt`
* **Layout Structure**: Top Header Bar + Banner Carousel + Quick Action Grid + Active Order Card.
* **UI Components**:
  * Top Greeting Header ("Hello, [User Name]", Profile avatar, Notification Bell icon).
  * Active Mission Banner (Card with live status: `SEARCHING`, `MATCHED`, `PICKED_UP`, "TRACK DRIVER" button).
  * Live City Status / Weather Surcharge Banner (Displays active city rules or weather/traffic surcharge notices).
  * **4 Main Quick Action Cards**:
    1. **Send Package** (Truck icon, "On-Demand Delivery"): Launches `order_quote`.
    2. **Order Food** (Bowl icon, "Hot Meals & Kitchens"): Launches `storefront_food`.
    3. **Groceries** (Shopping Basket icon, "Fresh Market"): Launches `storefront_groceries`.
    4. **Shop** (Shopping Bag icon, "Electronics & Fashion"): Launches `storefront_shop`.
* **User Actions**:
  * Tapping any action card routes to the respective logistics quote or commerce storefront.
  * Tapping Active Mission Card $\rightarrow$ routes to `track_order/{orderId}`.

### 2.2 Order Quote & Creation Screen
* **Route**: `order_quote`
* **File**: `feature/order/OrderQuoteScreen.kt`
* **Layout Structure**: Top Bar + Form Column (Addresses, Package Tier, Recipient, Payment, Fare Breakdown) + Sticky Bottom Dispatch FAB.
* **UI Components**:
  * **Pickup Address Card**: Displays selected pickup location, "Select Pickup Location" button.
  * **Delivery Address Card**: Displays selected dropoff location, "Select Dropoff Location" button.
  * **Package Tier Selector**: Horizontal scroll chips:
    * `Envelope` (₦500 base)
    * `Small` (₦800 base)
    * `Medium` (₦1,200 base)
    * `Large` (₦2,000 base)
    * `Heavy` (₦3,500 base)
  * Item Description Outlined Text Field.
  * Package Photo Attachment Box (Camera/Gallery launcher).
  * Recipient Name & Recipient Phone (+234) Input Fields.
  * Saved Recipients Quick Select Chip Row.
  * Promo Code Field & "Apply" Button.
  * **Fare Breakdown Summary Card**:
    * Distance (km)
    * Base Fare (₦)
    * Surcharges (Weather/Traffic if active)
    * Coupon Discount (-₦)
    * Total Payable (₦)
  * Payment Method Cards (Paystack Card, Wallet, Cash on Delivery Escrow).
  * "Request Dispatcher" Primary Button.
* **User Actions**:
  * Tapping Pickup/Delivery Address $\rightarrow$ launches `map_address_search`.
  * Typing promo code validates coupon and updates total fare in real time.
  * Clicking "Request Dispatcher" creates the logistics order and routes to `track_order/{orderId}`.

### 2.3 Map Address Search Screen
* **Route**: `map_address_search/{title}/{type}`
* **File**: `feature/order/MapAddressSearchScreen.kt`
* **Layout Structure**: Top Search Bar overlay + Interactive Google Map view + Bottom Address Confirmation Card.
* **UI Components**:
  * Search Bar with Places API auto-complete suggestions list.
  * Interactive Google Map with center pin marker.
  * "Use My Current GPS Location" FAB.
  * Selected Formatted Address text display.
  * "Confirm Location" Primary Button.
* **User Actions**:
  * Dragging map or typing address updates pin location.
  * Tapping "Confirm Location" returns `address`, `lat`, `lng`, `state` via `savedStateHandle` and pops back stack.

### 2.4 Track Order Screen
* **Route**: `track_order/{orderId}`
* **File**: `feature/order/TrackOrderScreen.kt`
* **Layout Structure**: Live Route Map (Top 50%) + Bottom Mission Sheet (Bottom 50%).
* **UI Components**:
  * Live Google Map rendering pickup marker, delivery marker, and real-time Fulfiller GPS marker.
  * **Status Stepper Timeline**:
    * `SEARCHING`: Finding nearby driver...
    * `MATCHED`: Driver assigned & en route to pickup
    * `PICKED_UP`: Package secured & en route to dropoff
    * `DELIVERED`: Package delivered successfully
  * **Fulfiller Info Card**: Driver photo, Name, Rating stars, Vehicle plate number, In-App Call button, In-App Chat button.
  * **Proof of Delivery Photo Card**: Displays photo captured by agent upon delivery.
  * 4-Digit Delivery Verification Code display.
* **User Actions**:
  * Tapping Chat button $\rightarrow$ routes to `order_chat/{orderId}`.
  * Tapping Call button $\rightarrow$ opens phone dialer with driver's phone number.

### 2.5 Orders Dashboard Screen (Customer)
* **Route**: `history` (inside `MainAppScaffold`)
* **File**: `feature/order/OrdersDashboardScreen.kt`
* **Layout Structure**: Tabbed Navigation (`Active`, `Past`, `Cancelled`).
* **UI Components**:
  * Order Summary Cards: Order ID, Date/Time, Item Description, Fare, Status Badge (`SEARCHING`, `MATCHED`, `DELIVERED`, `CANCELLED`).
  * "Track Mission" button on active cards.
  * "Re-Order" button on past cards.
* **Empty State**: "No orders found in this category." illustration and text.

---

## Part 3: Multi-Vendor Commerce & Storefronts

### 3.1 Food Storefront Screen
* **Route**: `storefront_food?merchantId={merchantId}`
* **File**: `feature/commerce/FoodStorefrontScreen.kt`
* **Layout Structure**: Top Header Banner + Cuisine Filter Chips + 2-Column Meal Grid + Bottom Cart Banner Bar.
* **UI Components**:
  * Search Bar (Search meals or restaurants).
  * Cuisine Filter Chips: `All`, `Local`, `Fast Food`, `Continental`, `Healthy`, `Pastries`.
  * **Meal Cards**: Photo thumbnail, Meal Name, Restaurant Name, Price (₦), Distance Badge (km), Add (`+`) button.
  * **Bottom Cart Floating Banner Bar**: Items count badge, Total Price (₦), "View Cart / Checkout" button.
* **User Actions**:
  * Tapping `+` adds meal to `CartManager` (enforces single-vendor cart).
  * Tapping meal card $\rightarrow$ opens `commerce_checkout?itemId={id}&itemType=meal`.
  * Tapping Bottom Cart Banner $\rightarrow$ opens `commerce_cart`.

### 3.2 Grocery Storefront Screen
* **Route**: `storefront_groceries`
* **File**: `feature/commerce/GroceryStorefrontScreen.kt`
* **Layout Structure**: Category Filter Chips + 2-Column Grocery Grid + Bottom Cart Banner Bar.
* **UI Components**:
  * Category Filter Chips: `All`, `Fresh Produce`, `Dairy & Eggs`, `Beverages`, `Snacks`, `Household`.
  * Grocery Item Cards with stock badges and Add (`+`) button.

### 3.3 Shop Storefront Screen
* **Route**: `storefront_shop?merchantId={merchantId}`
* **File**: `feature/commerce/ShopStorefrontScreen.kt`
* **Layout Structure**: Category Filter Chips + 2-Column Vendor Products Grid + Bottom Cart Banner Bar.
* **UI Components**:
  * Category Filter Chips: `All`, `Electronics`, `Pharmacy`, `Fashion`, `Beauty`, `Home`.
  * Product Cards with NAFDAC/Warranty badges and Add (`+`) button.

### 3.4 Cart Screen
* **Route**: `commerce_cart`
* **File**: `feature/commerce/CartScreen.kt`
* **Layout Structure**: Top Bar + Cart Items List + Bottom Total & Checkout Bar.
* **UI Components**:
  * Merchant Name Header.
  * **Cart Item Rows**: Photo thumbnail, Item Name, Unit Price (₦), Quantity controls (`-` `Qty` `+`), Remove (Trash icon).
  * Total Item Amount (₦).
  * "Proceed to Checkout" Primary Button.
* **Empty State**: "Your cart is empty." centered text and illustration.

### 3.5 Commerce Checkout Screen
* **Route**: `commerce_checkout?itemId={itemId}&itemType={itemType}&useCart={useCart}`
* **File**: `feature/commerce/CommerceCheckoutScreen.kt`
* **Layout Structure**: Item/Cart Summary + Delivery Destination Card + Delivery Schedule + Payment Cards + Price Summary + Pay Button.
* **UI Components**:
  * Item Header Card (Photo, Name, Merchant Name, Unit Price).
  * **Delivery Destination Card**: Location icon, Address string, "Select Delivery Address" button.
  * **Delivery Schedule Chips**: `Deliver Now` vs `Schedule for Later` (DatePicker/TimePicker launcher).
  * **Payment Method Cards**:
    * `Pay Now (Card)` Card
    * `My Wallet` Card (Displays available balance, disables if insufficient)
    * `Cash on Delivery (Escrow)` Card (`Pay in-app upfront`)
  * Promo Code Field & "Apply" Button.
  * **Payment Summary Breakdown**:
    * Item Price (₦)
    * Delivery Fee (₦)
    * Escrow Service Fee (₦)
    * Promo Discount (-₦)
    * Total Payable (₦)
  * "Pay & Complete Order" Button.
* **User Actions**:
  * Triggers `initializeCommerceOrder` API call. Redirects to Paystack URL for card/transfer or completes order via wallet/escrow.

---

## Part 4: Fulfiller / Agent Console & Execution

### 4.1 Fulfiller Dashboard Screen
* **Route**: `home` (when role == `FULFILLER`)
* **File**: `feature/fulfiller/FulfillerDashboardScreen.kt`
* **Layout Structure**: Top Header + Live GPS Map Radar (50% height) + Active/Queued Mission Banners + Inbound Mission Offer Cards.
* **UI Components**:
  * Top Bar with Agent Name, Verified Badge, Online/Offline Switch toggle.
  * Live Google Map Radar with agent marker and "Demand Hotspots" toggle overlay.
  * **Active Mission Banner** (Green Card: Order ID, Status, "RESUME" button).
  * **Queued Mission Banner** (Gold Card: Order ID, "START" button).
  * **Inbound Offer Cards**: Pickup Address, Dropoff Address, Distance (km), Fare Earnings Share (75% in ₦), Expiry Countdown Timer, "ACCEPT OFFER" button.
* **User Actions**:
  * Tapping Online/Offline toggle updates agent location pings.
  * Tapping "ACCEPT OFFER" claims mission or queues it if busy $\rightarrow$ opens `active_order/{orderId}`.

### 4.2 Active Order / Mission Execution Screen
* **Route**: `active_order/{orderId}`
* **File**: `feature/fulfiller/ActiveOrderScreen.kt`
* **Layout Structure**: Route Map (Top 40%) + Mission Stepper Controls (Bottom 60%) + Emergency SOS FAB.
* **UI Components**:
  * Google Map rendering agent location, pickup location, and dropoff location.
  * Order Summary Card (Order ID, Customer/Merchant Name, Pickup/Dropoff addresses).
  * **Mission Stepper Controls**:
    * `Step 1`: "ARRIVED AT PICKUP" Button
    * `Step 2`: "PACKAGE PICKED UP" Button
    * `Step 3`: "ARRIVED AT DROPOFF" Button
    * `Step 4`: "COMPLETE DELIVERY" Button
  * **Proof of Delivery Box**:
    * Camera launcher button ("Capture Proof of Delivery")
    * Captured Photo Preview Box
  * 4-Digit Delivery Code Entry Field.
  * "Report Incident" Button (Opens Incident Dialog: Breakdown, Security, Accident).
  * Emergency SOS FAB (Red button).
* **User Actions**:
  * Camera captures POD selfie/package.
  * Typing 4-digit code verifies delivery with backend, triggers automated queue promotion, and credits agent wallet (75% split).

### 4.3 Fulfiller Mission Records Screen
* **Route**: `history` (when role == `FULFILLER`)
* **File**: `feature/fulfiller/FulfillerOrdersScreen.kt`
* **Layout Structure**: Lifetime Earnings Banner + Tabbed Mission Records (`Active`, `Queued`, `Completed`).
* **UI Components**:
  * Lifetime Earnings Banner Card (75% Fare Share in ₦).
  * Mission Cards List: Order ID, Pickup/Dropoff, Earnings Share, Status Badge, "RESUME" / "START" button.

### 4.4 Fulfiller KYC Upload Screen
* **Route**: `kyc_upload`
* **File**: `feature/fulfiller/KycUploadScreen.kt`
* **Layout Structure**: Step Indicator (0-7) + Current Verification Step Form + Navigation Buttons.
* **Steps**:
  * `Step 0`: Personal Details (Date of Birth, Gender, Home Address).
  * `Step 1`: Category Confirmation (Agent, Rider, Driver).
  * `Step 2`: Profile Selfie (Front Camera Launcher with EXIF auto-rotation).
  * `Step 3`: Identity Verification (Prembly / Didit inline widget dialog).
  * `Step 4`: Vehicle Details (Plate Number, Vehicle Type).
  * `Step 5`: Operational Documents File Picker (Driver License, Insurance).
  * `Step 6`: Bank Payout Account (Paystack resolver).
  * `Step 7`: Final Submission & Review Status (`NOT_SUBMITTED`, `PENDING_REVIEW`, `VERIFIED`).

### 4.5 Agent Insights Screen
* **Route**: `insights`
* **File**: `feature/fulfiller/InsightsScreen.kt`
* **Layout Structure**: Earnings Bar Chart + Performance Stat Cards.
* **UI Components**:
  * 7-Day Earnings Trend Bar Chart.
  * Completion Rate Gauge (%).
  * Rating Stars Average (out of 5.0).
  * Peak Hour Bonus Tracker Card.

---

## Part 5: Merchant Seller Center & Store Management

### 5.1 Merchant Portal Screen
* **Route**: `merchant_portal`
* **File**: `feature/merchant/MerchantPortalScreen.kt`
* **Layout Structure**: Top Bar + Verification Banner + TabRow + Tab Content View.
* **Seller Center Tabs**:
  1. **Sales Tab**: Revenue Summary, Recent Completed Orders List.
  2. **Items Tab**: Grid of listed Products / Menu Items, "Add Item" FAB, Edit & Delete buttons.
  3. **Insights Tab**: Analytics Charts (Top Selling Items, Peak Hours).
  4. **Returns Tab**: Customer Return Requests List, Approve/Reject controls.
  5. **Promos Tab**: Merchant Coupon Generator & Active Discounts List.
  6. **Bulk Tab**: CSV/JSON Bulk Dispatch Batch List.
  7. **Settings Tab**: Store Details Card (Store Name, Category Dropdown, Interactive Map Address Picker), Operating Hours Form (Opening/Closing Times), Store Share Link Card.

### 5.2 Add / Edit Product Screen
* **Route**: `add_edit_product/{merchantType}/{merchantId}?productId={productId}`
* **File**: `feature/merchant/AddEditProductScreen.kt`
* **Layout Structure**: Photo Upload Box + Item Details Form + Save Button.
* **UI Components**:
  * Photo Upload Box (Camera/Gallery picker with preview).
  * Item Name Input Field.
  * Price (₦) Input Field.
  * Category Dropdown (`ExposedDropdownMenuBox` with preset categories + Custom Input Field).
  * Description Text Area.
  * Stock Quantity & Unit Input Fields (Vendor specific).
  * NAFDAC Registration Number Field (Optional).
  * Prep Time Minutes Input Field (Kitchen specific).
  * "Available for Order" Checkbox toggle.
  * "Save Listing" Primary Button.

### 5.3 Bulk Dispatch Screen
* **Route**: `bulk_dispatch`
* **File**: `feature/merchant/BulkDispatchScreen.kt`
* **Layout Structure**: File Import Box + Mission Batch List + Dispatch Button.
* **UI Components**:
  * CSV / JSON File Import Box.
  * Batch Name Input Field.
  * Parsed Missions List (Recipient Name, Address, Fare).
  * "Dispatch All Missions" Primary Button.

---

## Part 6: Fleet Partner & Corporate Business Portals

### 6.1 Fleet Partner Dashboard Screen
* **Route**: `fleet_dashboard`
* **File**: `feature/fleetpartner/FleetPartnerDashboardScreen.kt`
* **Layout Structure**: Fleet Overview Cards + Driver Roster Table.
* **UI Components**:
  * Total Fleet Drivers Card.
  * Active Fleet Missions Card.
  * Total Fleet Earnings Card.
  * **Driver Roster Table**: Driver Name, Vehicle Plate, Status (`ONLINE`, `OFFLINE`, `IN_MISSION`), Commission Override (%), Action buttons.

### 6.2 Corporate Dashboard Screen
* **Route**: `corporate_dashboard`
* **File**: `feature/order/CorporateDashboardScreen.kt`
* **Layout Structure**: Monthly Allowance Card + Department Quotas + Employee Roster.
* **UI Components**:
  * Total Company Allowance Balance (₦).
  * Employee Delivery Quota Table (Employee Name, Used Allowance, Remaining Quota, Edit Limit button).
  * Corporate Billing Statements download button.

---

## Part 7: Financials, Wallet & Payouts

### 7.1 Wallet Screen
* **Route**: `wallet`
* **File**: `feature/wallet/WalletScreen.kt`
* **Layout Structure**: Balance Card + Quick Action Buttons + Transaction History Ledger.
* **UI Components**:
  * **Wallet Balance Card**: Available Balance (₦), Escrowed Balance (₦) with help tooltip.
  * **Quick Actions Row**:
    * `Topup Wallet` Button (Launches Paystack inline topup modal).
    * `Withdraw Funds` Button (Launches `withdrawal`).
    * `Transfer Credit` Button.
  * **Transaction History Ledger**: Filter Chips (`All`, `Credits`, `Debits`), Transaction Item Rows (Title, Date, Amount ₦ with Green/Red color coding, Reference Code, Status Badge).

### 7.2 Withdrawal Screen
* **Route**: `withdrawal`
* **File**: `feature/wallet/WithdrawalScreen.kt`
* **Layout Structure**: Saved Payout Bank Card + Amount Input + Payout Button.
* **UI Components**:
  * Saved Payout Bank Card (Bank Name, Account Number, Verified Account Name).
  * Amount Input Field (₦).
  * "Withdraw to Bank Account" Primary Button.
* **Logic**: Triggers automated Paystack Transfer payout API call.

### 7.3 Growth & Rewards Screen
* **Route**: `growth_rewards`
* **File**: `feature/growth/GrowthRewardsScreen.kt`
* **Layout Structure**: Referral Share Card + Loyalty Points Card + Points-to-Cash Converter.
* **UI Components**:
  * Referral Code Card (Code text, Copy button, "Share Code" button).
  * Loyalty Points Balance Card (e.g. `1,250 Points`).
  * "Redeem Points for Cash" Form (Points Input, 1 Point = ₦1 conversion rate, "Redeem to Wallet" button).

---

## Part 8: Communication, Support & Safety Hub

### 8.1 Chat Screen
* **Route**: `order_chat/{orderId}` or `chat/{conversationId}`
* **File**: `feature/chat/ChatScreen.kt`
* **Layout Structure**: Top Participant Bar + Message Stream + Quick Preset Chips + Input Bar.
* **UI Components**:
  * Participant Header (Name, Role: Customer/Agent/Store, Phone button).
  * Message Bubble Stream (Timestamp, Sender alignment, Read indicators).
  * Quick Response Preset Chips ("I'm at the location", "Arriving in 5 mins", "Where should I drop the item?").
  * Text Outlined Input Field + Attachment Icon + "Send" Button.
* **Logic**: Socket.io real-time message stream.

### 8.2 Support Hub Screen
* **Route**: `support_hub`
* **File**: `feature/auth/SupportHubScreen.kt`
* **Layout Structure**: Knowledge Base Search + FAQ Category Accordion + Submit Ticket Button.
* **UI Components**:
  * Search Bar (Search help articles).
  * FAQ Category Accordion (General, Payments, Deliveries, Merchant, Safety).
  * "Submit Support Ticket" Button (Opens Ticket Modal).

### 8.3 Emergency SOS System
* **File**: Embedded inside `feature/fulfiller/ActiveOrderScreen.kt`
* **UI Components**:
  * Prominent Red SOS Button.
  * Confirmation Dialog ("Trigger Emergency SOS Alert?").
  * Immediate Location Alert Dispatcher (Sends GPS coordinates to emergency contacts & backend admin portal via SMS).

---

## Part 9: Account Settings, Legal & System Management

### 9.1 Account Screen
* **Route**: `account`
* **File**: `feature/auth/AccountScreen.kt`
* **Layout Structure**: User Profile Card + Referral Code Card + Options List + Legal Section + Sign Out Button.
* **UI Components**:
  * User Avatar, Name, Email, Role, Verified Badge.
  * Referral Code Share Card.
  * **Option Rows**:
    * `Edit Profile` $\rightarrow$ `profile_edit`
    * `Rewards & Referrals` $\rightarrow$ `growth_rewards`
    * `Manage My Shop` / `Add Merchant Profile` $\rightarrow$ `merchant_portal`
    * `Saved Addresses` $\rightarrow$ `saved_addresses`
    * `Saved Recipients` $\rightarrow$ `recipients_mgmt`
    * `Verification Documents (KYC)` $\rightarrow$ `kyc_upload`
    * `Notifications` $\rightarrow$ `notifications_settings`
    * `Active Sessions` $\rightarrow$ `session_mgmt`
    * `Support & Help Center` $\rightarrow$ `support_hub`
    * `Terms & Conditions` $\rightarrow$ `terms_conditions`
    * `Privacy Policy` $\rightarrow$ `privacy_policy`
    * `Change Password` (Opens Dialog)
    * `Delete Account` (Opens 3-Step NDPA Dialog)
  * "Sign Out" Red Button.

### 9.2 Profile Edit Screen
* **Route**: `profile_edit`
* **File**: `feature/auth/ProfileEditScreen.kt`
* **Layout Structure**: Full Name, Phone Number, Bank Payout Account (for Fulfillers), Emergency Contact Name & Phone.

---

## Part 10: Admin Web Console Overview (`backend_v3/src/views`)

| View Name | Route Path | Core Controls & Features |
| :--- | :--- | :--- |
| `dashboard.ejs` | `/admin/dashboard` | System GMV, Total Orders, Active Drivers Map, Quick Stats |
| `orders.ejs` | `/admin/orders` | All system orders, manual driver override, refund trigger |
| `admin_track.ejs` | `/admin/track` | Live GPS tracking of all online drivers & active routes |
| `merchants.ejs` | `/admin/merchants` | Unified merchant list, Approve, Suspend, Ban, "VIEW ITEMS" button |
| `merchant_store_items.ejs` | `/admin/merchants/:type/:id/items` | Inspect store listings, Enable/Disable items, Edit price/name, Delete item |
| `vendors.ejs` | `/admin/vendors` | Marketplace shop vendor stores |
| `kitchens.ejs` | `/admin/kitchens` | Food kitchen stores |
| `fulfillers.ejs` | `/admin/fulfillers` | Driver roster, mobility class filter, activate/suspend |
| `kyc_review.ejs` | `/admin/kyc/:id` | Review driver license, vehicle, and selfie documents |
| `coupons_admin.ejs` | `/admin/coupons` | Generate coupons (`TOTAL_BILL`, `DELIVERY_ONLY`, `MERCHANT_ONLY`) |
| `disputes.ejs` | `/admin/disputes` | Escrow hold releases and buyer/seller dispute resolution |
| `emergency_resolution.ejs` | `/admin/emergency` | Panic alert monitoring & emergency dispatch |
| `financial_overview.ejs` | `/admin/financials` | Revenue analytics, 75/25 split ledger, payout reports |
