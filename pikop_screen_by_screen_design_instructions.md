# Pikop Master Screen-by-Screen Content Outline & Design Instructions Blueprint

> **DESIGN SYSTEM OVERVIEW & MASTER STYLE GUIDE**:
> - **Primary Brand Color**: `PikopGreen` (`#008751` / `#388E3C` - Forest/Emerald Green)
> - **Secondary Accent**: `PikopGold` (`#FFC107` / `#FFB300` - Warm Gold)
> - **Dark Surface**: `#121212` (Elevated cards: `#1E1E1E`, Outline: `#2C2C2C`)
> - **Light Surface**: `#F8F9FA` (Cards: `#FFFFFF`, Outline: `#E0E0E0`)
> - **Typography Scale**: Material 3 (Headline Large: 28sp Bold, Title Medium: 16sp SemiBold, Label Small: 11sp ExtraBold)
> - **Global Design Tokens**: Corner Radius: 16.dp, Primary Button Height: 52.dp, Card Elevation: 2.dp, Screen Padding: 20.dp

---

# SECTION 1: ONBOARDING, AUTHENTICATION & VERIFICATION

---

### 1.1 Splash Screen
* **File Path**: `feature/auth/SplashScreen.kt`
* **Target Role**: All Roles (Unauthenticated / Boot)
* **Layout Structure**:
  * **Center Stage**: Pulsing Pikop Emerald Shield Logo (160.dp)
  * **Bottom Anchor**: Minimalist Loading Progress Indicator, App Version String (`v1.1.7`)
* **Copy & Labels**:
  * Tagline: *"Logistics & Commerce Engine"*
  * Version text: *"Pikop Engine v1.1.7"*
* **Design Instructions**:
  * Dark Surface Background (`#121212`).
  * Logo enters with smooth scale-in animation (`scale 0.8f -> 1.0f`, duration 600ms).
  * Auto-evaluates stored JWT token in DataStore: branches to `main` (if logged in) or `user_type_selection` (if new user).

---

### 1.2 User Type Selection Screen
* **File Path**: `feature/auth/UserTypeSelectionScreen.kt`
* **Target Role**: Unauthenticated Users
* **Layout Structure**:
  * **Header**: Pikop Logo (80.dp), Title *"Join Pikop Ecosystem"*, Subtitle *"Select how you want to use Pikop today."*
  * **Body**: Vertical stack of 5 Interactive Role Cards with custom vector icons:
    1. **Customer**: On-Demand Delivery & Shopping
    2. **Dispatch Agent / Fulfiller**: Earn 75% Delivery Share
    3. **Store Merchant**: Sell Food, Groceries & Products
    4. **Fleet Partner**: Manage Driver Rosters & Vehicles
    5. **Corporate Business**: Employee Delivery Allowances
  * **Footer**: Existing Account Login link
* **Copy & Labels**:
  * Title: *"How will you be using Pikop?"*
  * Customer card: *"Send packages, order meals & shop local stores"*
  * Agent card: *"Deliver missions with bike, car, or on foot & earn daily"*
  * Merchant card: *"Set up your digital store & manage sales"*
  * Existing account: *"Already have an account? Sign In"*
* **Design Instructions**:
  * Cards use elevated containers with 1.dp colored borders matching role accents.
  * Tapping a role card triggers subtle haptic feedback and navigates to the respective signup wizard.

---

### 1.3 Fulfiller Category Selection Screen
* **File Path**: `feature/auth/FulfillerCategorySelectionScreen.kt`
* **Target Role**: Fulfiller Applicants
* **Layout Structure**:
  * **Header**: Navigation Arrow Back, Title *"Choose Mobility Category"*
  * **Body**: 3 Hero Mobility Cards:
    1. **Foot Agent / Cyclist**: Local neighborhood deliveries
    2. **Rider (Motorcycle / Tricycle)**: High-speed city delivery
    3. **Driver (Car / Van / Truck)**: Large cargo & heavy items
* **Copy & Labels**:
  * Title: *"Select Your Delivery Class"*
  * Foot Agent: *"No vehicle required • Neighborhood missions"*
  * Rider: *"Bicycle, Bike, or Tricycle • Express delivery"*
  * Driver: *"Cars, Vans, Heavy Trucks • Bulk cargo"*
* **Design Instructions**:
  * Each card features custom PNG graphics (`agent_icon_foot.png`, `agent_icon_bike.png`, `agent_icon_truck.png`).
  * Selected card highlights with `PikopGreen` 2.dp border and checkmark badge.

---

### 1.4 Login Screen
* **File Path**: `feature/auth/LoginScreen.kt`
* **Target Role**: Returning Users (All Roles)
* **Layout Structure**:
  * **Header**: Pikop Logo (120.dp), Headline *"Welcome Back"*
  * **Form Fields**:
    * Email or Phone Number OutlinedTextField (Leading icon: Email/Phone)
    * Password OutlinedTextField (Leading icon: Lock, Trailing icon: Password Visibility Toggle)
  * **Actions**: *"Forgot Password?"* text link, *"Sign In"* Primary Button (52.dp)
  * **Footer**: *"Don't have an account? Sign Up"* link
* **Design Instructions**:
  * Clean form layout with single-line inputs and automatic keyboard focus advancement.
  * Shows inline error banners if credentials fail or rate-limit warnings appear.

---

### 1.5 Forgot Password Screen
* **File Path**: `feature/auth/ForgotPasswordScreen.kt`
* **Target Role**: All Roles
* **Layout Structure**:
  * **Step 1 View**: Email/Phone input field + *"Send OTP"* button.
  * **Step 2 View**: 6-digit OTP Input Box, Countdown Resend Timer, New Password & Confirm Password input fields, *"Reset Password"* button.
* **Copy & Labels**:
  * Step 1: *"Enter your email or phone number to receive a 6-digit reset code."*
  * Step 2: *"Code sent! Check your SMS or Email inbox."*
* **Design Instructions**:
  * Smooth cross-fade transition between Step 1 and Step 2 views.
  * Dispatches real-time SMS via Africa's Talking Gateway.

---

### 1.6 Signup Customer Screen
* **File Path**: `feature/auth/SignupCustomerScreen.kt`
* **Target Role**: Customer Applicants
* **Layout Structure**:
  * **Header**: Title *"Create Customer Account"*
  * **Form Fields**: Full Name, Email, Phone (+234...), Password, Confirm Password, Referral Code (Optional)
  * **Terms Agreement**: Checkbox with clickable *"Terms & Conditions"* and *"Privacy Policy"* links.
  * **Action**: *"Create Account"* Primary Button (52.dp)
* **Design Instructions**:
  * Form validates passwords match and phone format starts with valid Nigerian prefix (`+234` / `0`).

---

### 1.7 Signup Fulfiller Screen
* **File Path**: `feature/auth/SignupFulfillerScreen.kt`
* **Target Role**: Fulfiller Applicants
* **Layout Structure**:
  * **Header**: Category Badge (Foot Agent / Rider / Driver), Title *"Agent Registration"*
  * **Form Fields**: Contact Name, Email, Phone, Password, Emergency Contact Person, Emergency Phone Number
  * **Action**: *"Continue to Verification"* Button
* **Design Instructions**:
  * Pre-sets `primary_class` based on category selection in the previous step.

---

### 1.8 Signup Merchant Screen (Step 1)
* **File Path**: `feature/auth/SignupMerchantScreen.kt`
* **Target Role**: Merchant Applicants
* **Layout Structure**:
  * **Header**: Title *"Merchant Registration - Step 1"*
  * **Form Fields**: Contact Manager Name, Role (Owner/Manager), Business Email, Phone, Password
  * **Action**: *"Next: Business Details"* Button

---

### 1.9 Merchant Business Setup Screen (Step 2)
* **File Path**: `feature/auth/MerchantBusinessSetupScreen.kt`
* **Target Role**: Merchant Applicants
* **Layout Structure**:
  * **Header**: Title *"Business Verification - Step 2"*
  * **Form Fields**:
    * Business Name OutlinedTextField
    * Category Dropdown (`ExposedDropdownMenuBox`: Food, Groceries, Shop, + Custom input)
    * Store Location Address Card (Clickable -> opens Map Address Picker)
    * CAC Registration Number (Optional)
    * NAFDAC Number (Conditional: Food/Groceries)
  * **Payout Bank Section**: Paystack Bank Dropdown with real-time account name auto-resolution (triggers when 10 digits entered).
  * **Action**: *"Submit Business Verification"* Primary Button
* **Design Instructions**:
  * Store Address card displays map pin icon and exact PostGIS location string.

---

### 1.10 Signup Fleet Partner Screen & Setup (Steps 1 & 2)
* **File Paths**: `SignupFleetPartnerScreen.kt` & `FleetPartnerBusinessSetupScreen.kt`
* **Target Role**: Fleet Partners
* **Layout Structure**:
  * Manager details, Fleet size input (1-5, 5-20, 20+), Vehicle Tier checkboxes, CAC registration number, Bank Payout resolver.

---

### 1.11 Signup Corporate Screen & Setup (Steps 1 & 2)
* **File Paths**: `SignupCorporateScreen.kt` & `CorporateBusinessSetupScreen.kt`
* **Target Role**: Corporate Businesses
* **Layout Structure**:
  * Corporate Manager name, Business domain, Corporate CAC, Estimated monthly delivery quota, Billing preferences.

---

### 1.12 Email / SMS OTP Screen
* **File Path**: `feature/auth/EmailOtpScreen.kt`
* **Target Role**: Unverified Users
* **Layout Structure**:
  * **Center Stage**: 6 Individual Digits Input Boxes with automatic focus progression.
  * **Timer**: *"Resend code in 00:45"*.
* **Design Instructions**:
  * Automatically submits the form upon typing the 6th digit without needing to tap submit.

---

# SECTION 2: CUSTOMER LOGISTICS & ON-DEMAND DELIVERY

---

### 2.1 Customer Home Screen
* **File Path**: `feature/order/CustomerHomeScreen.kt`
* **Target Role**: Customer
* **Layout Structure**:
  * **Header Bar**: User Greeting *"Hello, [Name] 👋"*, Address Chip (Clickable -> open address book), Notification Bell Icon.
  * **Quick Action Grid (2x2 Hero Cards)**:
    1. **Send Package** (Logistics Quote) - Emerald card with delivery van icon
    2. **Food Delivery** (Restaurants) - Gold card with meal icon
    3. **Groceries** (Fresh Market) - Purple card with grocery basket icon
    4. **Shops & Stores** (Electronics/Fashion) - Blue card with store icon
  * **Active Mission Live Banner**: Displayed when a live order is in progress (Shows Order #, Status, View Map CTA).
  * **Live City Rules Banner**: Displays city-specific operating status and weather/traffic surcharge notices.
* **Design Instructions**:
  * Hero cards use 16.dp corner radius with subtle gradient fills and elevated shadow.

---

### 2.2 Order Quote & Creation Screen
* **File Path**: `feature/order/OrderQuoteScreen.kt`
* **Target Role**: Customer
* **Layout Structure**:
  * **Pickup Address Card**: Clickable card showing current pickup location + "Change" CTA.
  * **Dropoff Address Card**: Clickable card showing delivery destination + "Select" CTA.
  * **Package Tier Selector (Horizontal Carousel)**:
    * Envelope / Small / Medium / Large / Heavy Cargo cards with dimension & weight limits.
  * **Package Details Input**: Item Description, Upload Photo Card (Camera / Gallery picker).
  * **Recipient Information**: Name, Phone Number, "Select from Contacts" button.
  * **Promo Code Field**: OutlinedTextField + "Apply" button (supports `TEST100`, `TESTER100`, store coupons).
  * **Fare Breakdown Card**: Item Price, Delivery Fare, Distance (km), Platform Escrow Fee, SMS Charge, Total Amount.
  * **Action Bar**: "Dispatch Delivery" Primary Button (52.dp).
* **Design Instructions**:
  * Recalculates distance and fare dynamically whenever pickup/dropoff or package tier changes.

---

### 2.3 Map Address Search Screen
* **File Path**: `feature/order/MapAddressSearchScreen.kt`
* **Target Role**: All Roles
* **Layout Structure**:
  * **Full-Screen Google Map**: Center draggable map pin with pulsing shadow.
  * **Search Header**: Places API Search Bar with auto-complete drop-down list.
  * **Bottom Sheet Card**: Formatted Address String, Landmark Input, "Confirm Location" Button.
  * **FAB**: "Use Current GPS Location" button.
* **Design Instructions**:
  * Returns exact `address`, `lat`, `lng`, and `state` to the calling screen via `savedStateHandle`.

---

### 2.4 Saved Addresses & Saved Recipients Screens
* **File Paths**: `SavedAddressesScreen.kt` & `RecipientManagementScreen.kt`
* **Layout Structure**:
  * List of pre-saved address cards (Home, Office, Warehouse) and contact cards with quick-select actions.

---

### 2.5 Track Order Screen
* **File Path**: `feature/order/TrackOrderScreen.kt`
* **Target Role**: Customer & Guest Receivers
* **Layout Structure**:
  * **Top 60% Screen**: Real-time Google Map showing route polyline, Pickup pin, Dropoff pin, and animated Fulfiller GPS marker.
  * **Bottom Sheet (Draggable)**:
    * **Status Stepper Timeline**: Searching 🔍 -> Matched 🤝 -> Picked Up 📦 -> Delivered ✅
    * **Fulfiller Profile Card**: Photo, Name, Vehicle Plate Number, Rating Stars.
    * **Communication Bar**: In-App Call Button, In-App Chat Button.
    * **Proof of Delivery Section**: Photo Evidence viewer, 4-Digit Delivery Verification Code badge.
* **Design Instructions**:
  * Connected to real-time Socket.io socket channels (`order_${id}`) for instant marker position animation.

---

### 2.6 Orders Dashboard Screen
* **File Path**: `feature/order/OrdersDashboardScreen.kt`
* **Target Role**: Customer
* **Layout Structure**:
  * **Header**: Tabbed Filter Bar (`Active`, `Past`, `Cancelled`).
  * **Body**: Vertical list of Order Cards: Order ID, Merchant/Recipient Name, Status Badge, Total Fare, "Track Order" or "Re-Order" CTAs.

---

# SECTION 3: COMMERCE, STOREFRONTS & MULTI-VENDOR CART

---

### 3.1 Food Storefront Screen
* **File Path**: `feature/commerce/FoodStorefrontScreen.kt`
* **Target Role**: Customer
* **Layout Structure**:
  * **Top Surface (Emerald / Tertiary Header)**: "Order Food" Title, Search Bar, Header Cart Icon with item count badge.
  * **Cuisine Filter Chips (Horizontal Scroll)**: All, Local, Fast Food, Continental, Healthy, Pastries.
  * **2-Column Meal Grid**:
    * **Discovery Item Card**: Photo (AsyncImage), "MEAL" Badge, Name, Restaurant Name, Price (₦), Distance (km), "+" Add to Cart Floating Button.
  * **Bottom Cart Floating Banner Bar**: Displays item count, total price, and "Checkout" arrow.
* **Design Instructions**:
  * Bottom padding `90.dp` ensures content is never obstructed by the bottom cart banner.

---

### 3.2 Grocery & Shop Storefront Screens
* **File Paths**: `GroceryStorefrontScreen.kt` & `ShopStorefrontScreen.kt`
* **Layout Structure**:
  * Identical grid layout as Food Storefront customized with Fresh Produce/Dairy/Snacks chips for Groceries and Electronics/Fashion/Beauty chips for Shops.

---

### 3.3 Cart Screen
* **File Path**: `feature/commerce/CartScreen.kt`
* **Target Role**: Customer
* **Layout Structure**:
  * **Header**: TopAppBar *"Your Cart"*.
  * **Body**: Vertical List of Cart Item Rows:
    * Item Name, Vendor Name, Price per unit.
    * Quantity Controls: `[-]` Button, Quantity Text, `[+]` Button, Trash Delete Icon.
  * **Bottom Bar**: Total Price Summary, *"Proceed to Checkout"* Button (56.dp).
* **Design Instructions**:
  * Single-vendor cart enforcement: Warns user if attempting to add items from a second vendor simultaneously.

---

### 3.4 Commerce Checkout Screen
* **File Path**: `feature/commerce/CommerceCheckoutScreen.kt`
* **Target Role**: Customer
* **Layout Structure**:
  * **Item Summary Header**: Photo, Item/Store Name, Price.
  * **Delivery Destination Card**: Clickable card opening Map Address Picker.
  * **Delivery Schedule Selector**: Filter chips for *"Deliver Now"* vs *"Schedule for Later"* (DatePicker + TimePicker dialogs).
  * **Payment Method Cards (Horizontal Scroll)**:
    1. **Pay Now (Card)** - Paystack online redirect
    2. **My Wallet** - Deducts from actual balance
    3. **Cash on Delivery (Escrow)** - In-app upfront payment held in escrow
  * **Promo Code Row**: OutlinedTextField + "Apply" button.
  * **Payment Summary Card**: Item Price, Delivery Fee, Escrow Service Fee, Total Payable (₦).
  * **Action**: *"Pay & Complete Order"* Primary Button.

---

# SECTION 4: FULFILLER / AGENT CONSOLE & MISSION EXECUTION

---

### 4.1 Fulfiller Dashboard Screen
* **File Path**: `feature/fulfiller/FulfillerDashboardScreen.kt`
* **Target Role**: Fulfiller (Agent / Rider / Driver)
* **Layout Structure**:
  * **Top Surface**: Verified Agent Badge, Online/Offline Toggle Switch.
  * **Map Section**: Live GPS Radar Map with "Demand Hotspots" heatmap overlay toggle.
  * **Active Mission Resume Card**: Prominent Emerald banner displayed if an active mission is in progress ("ACTIVE MISSION IN PROGRESS 🚀 - Order #XX - RESUME").
  * **Queued Mission Card**: Gold banner displayed if a second mission is queued ("QUEUED MISSION WAITING ⏳ - Order #XX - START").
  * **Inbound Offer Stream**: Cards showing Pickup/Dropoff addresses, Estimated Distance, 75% Fare Share, "Accept Offer" / "Decline" buttons.
* **Design Instructions**:
  * Polls offers every 5 seconds and listens to Socket.io for instant mission dispatch.

---

### 4.2 Active Order / Mission Execution Screen
* **File Path**: `feature/fulfiller/ActiveOrderScreen.kt`
* **Target Role**: Fulfiller
* **Layout Structure**:
  * **Top 50% Map**: Route polyline from agent -> pickup -> dropoff.
  * **Mission Stepper Controls**:
    * **Stage 1**: *"Arrived at Pickup Location"*
    * **Stage 2**: *"Confirm Item Picked Up"*
    * **Stage 3**: *"Arrived at Dropoff Location"*
    * **Stage 4**: *"Complete Delivery"*
  * **Proof of Delivery Section**: Camera button for taking POD evidence photo + 4-Digit Delivery Code verification box.
  * **Floating Safety Tools**: Red "SOS Emergency" FAB, "File Incident" button.
* **Design Instructions**:
  * Camera captures POD photos with automatic EXIF orientation rotation. Auto-promotes queued missions upon delivery completion.

---

### 4.3 Mission Records Screen
* **File Path**: `feature/fulfiller/FulfillerOrdersScreen.kt`
* **Target Role**: Fulfiller
* **Layout Structure**:
  * **Header**: Tabbed Filter Bar (`Active`, `Queued`, `Completed`).
  * **Completed Tab Banner**: Displays *"Completed Lifetime Earnings (75% Share)"* in Naira.
  * **Mission Cards**: Order #, Pickup/Dropoff text, 75% Earnings share, "RESUME" or "START QUEUED" buttons.

---

### 4.4 KYC Upload Screen (7-Step Wizard)
* **File Path**: `feature/fulfiller/KycUploadScreen.kt`
* **Target Role**: Fulfiller
* **Layout Structure**:
  * **Step Indicator Bar**: Steps 0 to 7.
  * **Step 0**: Personal Details (DOB, Gender, Home Address).
  * **Step 1**: Mobility Class Selection.
  * **Step 2**: Profile Photo (Front Camera Selfie with EXIF rotation).
  * **Step 3**: Identity Verification (Prembly / Didit inline widget dialog).
  * **Step 4**: Vehicle Registration (Plate #, Driver License).
  * **Step 5**: Operational Documents upload.
  * **Step 6**: Bank Payout details.
  * **Step 7**: Submission Review & Approval Status.

---

### 4.5 Agent Insights Screen
* **File Path**: `feature/fulfiller/InsightsScreen.kt`
* **Layout Structure**:
  * Weekly Earnings Chart, Completion Rate %, Rating Breakdown (1-5 Stars), Peak Hour Bonus Tracker.

---

# SECTION 5: MERCHANT SELLER CENTER & STORE MANAGEMENT

---

### 5.1 Merchant Portal Screen (Seller Center)
* **File Path**: `feature/merchant/MerchantPortalScreen.kt`
* **Target Role**: Merchant (Vendor / Kitchen)
* **Layout Structure**:
  * **Header**: "Seller Center" Title, Settings Gear Icon (Opens Settings Tab), Refresh Icon.
  * **Top Banner**: Verification Status (Active vs Pending Review).
  * **Tab Row**: `Sales` | `Items` | `Insights` | `Returns` | `Promos` | `Bulk`.
  * **Settings Tab View (Gear Icon)**:
    * **Promote Store Card**: Share Store Link button (`pikop://store/slug`).
    * **Operating Hours Card**: Opening & Closing Time inputs + Save button.
    * **Store Details Card**: Business Name, Category Dropdown, Store Location Map Picker ("Tap to pick location on map").
    * **Store Preferences**: "Accept Pay on Delivery (Escrow)" switch, "Allow Returns" switch.

---

### 5.2 Add / Edit Product & Menu Item Screen
* **File Path**: `feature/merchant/AddEditProductScreen.kt`
* **Target Role**: Merchant
* **Layout Structure**:
  * **Photo Upload Box**: Upload Photo button / AsyncImage preview.
  * **Form Fields**:
    * Item Name, Price (₦), Description.
    * Category Dropdown (`ExposedDropdownMenuBox`: Preset options + "Custom / Other..." text input).
    * Vendor Fields: Stock Quantity, Unit (kg, item, pack), NAFDAC Reg No.
    * Kitchen Fields: Prep Time (Mins), "Available for Order" Checkbox.
  * **Action**: *"Save Listing"* Primary Button.

---

### 5.3 Bulk Dispatch Screen
* **File Path**: `feature/merchant/BulkDispatchScreen.kt`
* **Target Role**: Merchant & Corporate
* **Layout Structure**:
  * CSV/JSON Import button, Batch Name input, Delivery Destinations Table, "Mass Dispatch Missions" button.

---

# SECTION 6: FLEET PARTNER & CORPORATE PORTALS

---

### 6.1 Fleet Partner Dashboard Screen
* **File Path**: `feature/fleetpartner/FleetPartnerDashboardScreen.kt`
* **Layout Structure**:
  * Fleet Stats Cards (Total Drivers, Active Missions, Total Earnings), Driver Roster Table (Name, Status, Commission Override, Action Buttons).

---

### 6.2 Corporate Dashboard Screen
* **File Path**: `feature/order/CorporateDashboardScreen.kt`
* **Layout Structure**:
  * Allowance Balance, Employee Quota Table, Department Delivery Spend Chart, Corporate Billing History.

---

# SECTION 7: FINANCIALS, WALLET & PAYOUTS

---

### 7.1 Wallet Screen
* **File Path**: `feature/wallet/WalletScreen.kt`
* **Target Role**: All Roles
* **Layout Structure**:
  * **Balance Card**: Actual Available Balance (₦) + Escrow Held Balance (₦).
  * **Quick Action Buttons**: `[Topup Wallet]` | `[Withdraw]` | `[Transfer]`.
  * **Transaction History**: Filter chips (`All`, `Credit`, `Debit`), Transaction Rows (Title, Date, Amount in Green/Red, Status Badge).
* **Design Instructions**:
  * Topup button triggers inline Paystack webview modal or card payment.

---

### 7.2 Withdrawal Screen
* **File Path**: `feature/wallet/WithdrawalScreen.kt`
* **Target Role**: Fulfiller, Merchant, Fleet Partner
* **Layout Structure**:
  * Saved Payout Bank Account Card, Amount Input, *"Withdraw Funds"* Button (Triggers instant Paystack Transfer).

---

### 7.3 Growth & Rewards Screen
* **File Path**: `feature/growth/GrowthRewardsScreen.kt`
* **Target Role**: Customer & Fulfiller
* **Layout Structure**:
  * Referral Code Share Card + Share Button, Loyalty Points Ledger, Points-to-Cash Redemption Form (500 pts = NGN 500).

---

# SECTION 8: COMMUNICATION, SUPPORT & SAFETY HUB

---

### 8.1 Chat Screen
* **File Path**: `feature/chat/ChatScreen.kt`
* **Target Role**: Customer, Fulfiller, Merchant
* **Layout Structure**:
  * Header: Recipient Name, Role Badge, Order ID.
  * Message Bubble Stream (Customer bubbles right, Fulfiller/Merchant left).
  * Quick Reply Chips: *"I've arrived"*, *"Where are you?"*, *"Package is ready"*.
  * Input Bar: Text input + Send FAB. Connected to Socket.io.

---

### 8.2 Support Hub & FAQ Detail
* **File Paths**: `SupportHubScreen.kt` & `FaqDetailScreen.kt`
* **Layout Structure**:
  * Category Accordions, Ticket Submission Modal, Markdown FAQ Reader.

---

# SECTION 9: ACCOUNT SETTINGS & SYSTEM MANAGEMENT

---

### 9.1 Account Screen
* **File Path**: `feature/auth/AccountScreen.kt`
* **Target Role**: All Roles
* **Layout Structure**:
  * User Header Card (Photo, Name, Email, Role, Verified Badge).
  * Referral Code Card + Share Button.
  * Options List Cards: Edit Profile, Rewards, Manage Shop, Addresses, Recipients, KYC, Notifications, Support, Terms, Privacy, Session Management, Change Password, Delete Account (3-step NDPA confirmation dialog), Sign Out Button.

---

# SECTION 10: ADMIN WEB CONSOLE INVENTORY (`backend_v3/src/views`)

---

| View Name | Route | Key Capabilities & Features |
| :--- | :--- | :--- |
| **Admin Dashboard** | `/admin/dashboard` | System GMV, Active Orders, Active Fulfillers, Sales Charts |
| **Orders Control** | `/admin/orders` | Live order monitoring, manual agent override, refund trigger |
| **Live Map Tracking** | `/admin/track` | Real-time agent GPS map tracking |
| **Merchant Hub** | `/admin/merchants` | Unified seller registry, approve/suspend/delete stores, "VIEW ITEMS" button |
| **Store Contents** | `/admin/merchants/:type/:id/items` | Inspect store products/meals, toggle active/disabled, edit price/name, delete item |
| **Fulfiller Registry** | `/admin/fulfillers` | Agent roster, class filtering, manual activation |
| **KYC Review** | `/admin/kyc/:id` | Review agent identity documents & approve/reject |
| **Coupon Generator** | `/admin/coupons` | Create universal/store coupons (`TOTAL_BILL`, `DELIVERY_ONLY`, `MERCHANT_ONLY`) |
| **Dispute Resolution** | `/admin/disputes` | Escrow hold release, buyer/seller dispute resolution |
| **Emergency SOS** | `/admin/emergency` | Active panic alert monitoring and emergency response |
| **Financial Overview** | `/admin/financials` | Revenue charts, 75/25 split ledger, total payouts |
| **Withdrawals** | `/admin/withdrawals` | Batch approve agent earnings payouts |
