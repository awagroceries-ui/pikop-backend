# Walkthrough - Role-Based Onboarding

I have successfully refactored the onboarding flow to prioritize the user's primary goal—either "Sending/Receiving" as a Customer or "Delivering/Earning" as a Fulfiller. This provides a more tailored and professional first impression for both user groups.

## Changes Made

### 1. New Entry Point: User Type Selection
- **Role Selection Screen**: Created a high-impact screen shown immediately after the splash animation. It features two large branded cards: "I want to Send" and "I want to Earn."
- **Path Specialization**: Selecting a role now customizes the entire signup experience, including screen titles, descriptions, and the eventual landing dashboard.

### 2. Tailored Signup Experience
- **Dynamic Content**: The `SignupScreen.kt` now dynamically updates its UI based on the chosen role (e.g., "Join the Fleet" for Fulfillers).
- **Automated Fulfiller Setup**: When a user signs up as a Fulfiller, the backend now **automatically** initializes their Fulfiller profile and Wallet in a single transaction, eliminating the previous "Switch to Fulfiller" confusion.
- **Improved Persistence**: The app now stores the user's chosen role in the local DataStore, ensuring that every time they reopen the app, they land on the correct dashboard (Customer or Fulfiller) without being asked again.

### 3. Backend & Database Infrastructure
- **User Roles Table**: Added a `role` column to the `users` table to securely track the primary account type.
- **Smart Auth Controller**: Updated the signup and login endpoints to handle and return the role context, ensuring the app always routes to the correct experience.
- **JWT Enhancement**: Included the user role in the JWT payload for more robust security and session management.

### 4. Navigation Refinement
- **MainActivity Update**: Completely refactored the `PikopAppNavigation` to handle the new branching paths.
- **Streamlined Login**: The login screen now detects the user's role from the server response and automatically directs them to their respective command center.

## Verification Results

### Automated Build
- Ran `./gradlew :app:assembleDebug` and the build finished successfully.

### Logic Verification
- Verified that signing up as a Fulfiller automatically creates the necessary database records for KYC and earnings.
- Confirmed that the "About" and "Terms" sections accurately reflect the user's current role.

## Deployment Instructions (VPS)
Run these commands in your VPS terminal to enable the new role-based system:

```bash
# 1. Update the code and apply database changes
cd /var/www/pikop-api
git pull origin main
cd backend
npm run migrate:up

# 2. Restart the server
pm2 restart pikop-api
```

> [!TIP]
> This refactor makes the app feel like two specialized tools (one for customers, one for drivers) while maintaining a single, secure database of users.
