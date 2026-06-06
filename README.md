# OpenPay Companion App

**The Android companion app for OpenPay** — automatically intercept and sync bank payment alerts (UPI, SMS, etc.) with your OpenPay merchant console.

---

## 📱 App Overview

OpenPay Companion is an Android application that acts as a bridge between incoming bank SMS notifications and the OpenPay merchant platform. It intelligently intercepts bank payment alerts from major Indian banks and payment gateways, parses transaction data, and syncs it in real-time to your merchant dashboard.

### Key Features

- **SMS Interception**: Automatically captures incoming payment alerts from banks (HDFC, SBI, ICICI, Axis, etc.)
- **Real-time Sync**: Forwards transactions to the OpenPay backend instantly
- **Smart Parsing**: AI-powered extraction of transaction amounts, UTRs, and sender details
- **Device Pairing**: Secure device registration with merchant accounts
- **Sync History**: Tracks all forwarded transactions with detailed logs
- **Auto-sync Toggle**: Control whether transactions sync automatically or manually
- **Permission Management**: Seamless SMS permission handling with user education
- **Notification Alerts**: Real-time feedback on sync status (success/failure)

---

## 🏗️ Architecture & Code Structure

### Project Layout

```
app/src/main/
├── java/com/example/
│   ├── MainActivity.kt              # Main entry point, handles permission flow & screen routing
│   ├── service/
│   │   └── SmsReceiver.kt          # Broadcast receiver for intercepting SMS messages
│   ├── ui/
│   │   ├── dashboard/
│   │   │   └── DashboardScreen.kt  # Main UI for viewing sync logs & settings
│   │   ├── pairing/
│   │   │   └── PairingScreen.kt    # Device pairing/registration UI
│   │   └── theme/                  # Color & typography theme
│   ├── data/
│   │   ├── ApiClient.kt            # Retrofit API client & data models
│   │   ├── PrefsManager.kt         # SharedPreferences wrapper for local storage
│   │   └── (other data models)
│   └── model/
│       └── SyncLogEntry.kt         # Local data model for transaction logs
├── res/
│   ├── strings.xml
│   ├── colors.xml
│   └── (resources)
└── AndroidManifest.xml
```

---

## 🔑 Key Components

### 1. **MainActivity.kt** — Entry Point
- **Purpose**: App initialization and screen navigation
- **Responsibilities**:
  - Manages permission requests (SMS, Notifications)
  - Routes between PairingScreen (unpaired) and DashboardScreen (paired)
  - Displays permission explanation card when SMS access is denied
  - Handles permission launcher with Android 13+ support

### 2. **SmsReceiver.kt** — SMS Interception Service
- **Purpose**: Receives and forwards bank payment alerts
- **Key Features**:
  - Listens for incoming SMS via `BroadcastReceiver`
  - **Bank Filtering**: Only processes SMS from known banks (HDFC, SBI, ICICI, Axis, Paytm, etc.)
  - **Format Validation**: Validates sender format (e.g., `SB-ABCDEF` for Standard Bank)
  - **Transaction Forwarding**: Sends SMS content to OpenPay backend via `TransactionRequest`
  - **Sync Logging**: Records each forwarding attempt (success/failure/missed)
  - **Notification Alerts**: Shows device notifications for sync status
  - **Auto-sync Control**: Respects user's auto-sync toggle setting

#### How It Works:
1. Android OS delivers SMS to `SmsReceiver`
2. Check if device is paired (via `PrefsManager`)
3. Extract sender and message content
4. Validate sender against bank patterns
5. If auto-sync enabled: Forward to backend; otherwise log locally as "missed"
6. On success: Create sync log entry with `transactionId`
7. Show notification to user
8. Emit event to update dashboard UI

### 3. **PairingScreen.kt** — Device Registration
- **Purpose**: Connect device to merchant account
- **Flow**:
  1. User enters merchant code
  2. App sends `PairRequest` with merchant code, device ID, and device name
  3. Backend validates and returns merchant details
  4. On success: Store pairing data and navigate to Dashboard
  5. On failure: Show error message and allow retry

### 4. **DashboardScreen.kt** — Main UI
- **Purpose**: Display transaction history and app settings
- **Features**:
  - **Sync Log List**: Shows all forwarded transactions with timestamps and status
  - **Settings Panel**:
    - Auto-sync toggle
    - View/change merchant code
    - Clear log history
    - Unlink device
  - **Transaction Details**: Clicking a log entry shows parsed transaction info (if available)
  - **Real-time Updates**: Uses `SmsSyncNotifier` flow to update UI when SMS is synced

### 5. **ApiClient.kt** — Backend Communication
- **Purpose**: Define API contracts and handle HTTP requests
- **API Models**:

  ```kotlin
  // Device pairing request/response
  PairRequest(merchantCode, deviceId, deviceName)
  PairResponse(success, merchantId, merchantName, message)
  
  // Transaction forwarding request/response
  TransactionRequest(merchantCode, smsText, senderHeader, timestamp, deviceId, deviceName)
  TransactionResponse(success, transactionId, message, parsing)
  
  // Parsed transaction details
  ParsingDetails(isCredit, parsedAmount, parsedUtr, parsedSender, confidence, reasoning)
  ```

- **Endpoints**:
  - `POST /api/devices/pair` — Pair device with merchant account
  - `POST /api/transactions` — Forward SMS transaction

### 6. **PrefsManager.kt** — Local Data Storage
- **Purpose**: Persist user data using SharedPreferences
- **Stored Data**:
  - `deviceId` — Unique device identifier (Android ID or UUID)
  - `merchantCode` — Merchant's pairing code
  - `merchantName` — Merchant business name
  - `merchantId` — Merchant account ID
  - `baseUrl` — Backend URL (default: `https://open-pay-822w.vercel.app`)
  - `isPaired` — Boolean pairing status
  - `autoSyncEnabled` — Auto-sync toggle
  - `syncLog` — JSON array of last 20 transaction logs

---

## 🔄 Data Flow

### Pairing Flow
```
User Input (Merchant Code)
    ↓
PairingScreen sends PairRequest
    ↓
Backend validates & returns PairResponse
    ↓
Store in PrefsManager (merchantCode, merchantId, merchantName)
    ↓
Set isPaired = true
    ↓
Navigate to DashboardScreen
```

### SMS to Transaction Flow
```
Bank sends SMS to device
    ↓
Android OS delivers to SmsReceiver
    ↓
Check if paired (isPaired == true)
    ↓
Validate sender (against bank patterns)
    ↓
Check auto-sync setting
    ↓
If enabled: Send TransactionRequest to backend
    ↓
Backend parses & returns parsing details + transactionId
    ↓
Log entry created (status: "success" or "failed")
    ↓
SmsSyncNotifier emits event
    ↓
DashboardScreen updates UI with new log entry
    ↓
Show notification to user
```

---

## 📡 Backend Integration

The app connects to the OpenPay backend at **`https://open-pay-822w.vercel.app`** (configurable via `baseUrl`).

### API Documentation
For complete backend documentation and endpoint details, visit:
👉 **[OpenPay Dashboard Docs](https://open-pay-822w.vercel.app/dashboard/docs)**

### Required Backend Endpoints

1. **Pair Device**
   - `POST /api/devices/pair`
   - Request: `PairRequest`
   - Response: `PairResponse` (includes merchantId, merchantName)

2. **Forward Transaction**
   - `POST /api/transactions`
   - Request: `TransactionRequest` (includes full SMS text)
   - Response: `TransactionResponse` (includes parsed transaction details & transactionId)

### Backend Capabilities
- Validates merchant code and device credentials
- Parses SMS text using NLP/regex to extract:
  - Transaction amount
  - UTR (Unique Transaction Reference)
  - Sender bank/payment gateway
  - Transaction type (credit/debit)
  - Confidence score
- Stores transactions in merchant dashboard
- Provides sync history and reporting

---

## 🛠️ Development & Setup

### Prerequisites
- **[Android Studio](https://developer.android.com/studio)** (latest version)
- **Kotlin** (via Android Studio)
- **Gradle** (bundled)
- **Java 11+**

### Run Locally

1. **Open Android Studio**
   - Launch Android Studio on your machine

2. **Open the Project**
   - Select **File → Open**
   - Navigate to and select the `openpay_companion_app` directory
   - Click "Open"

3. **Allow Project Sync**
   - Android Studio will automatically detect the project structure
   - Allow it to fix any incompatibilities as it imports the project
   - Wait for Gradle sync to complete

4. **Configure Environment Variables**
   - Create a file named `.env` in the project root directory:
     ```bash
     touch .env
     ```
   - Add your Gemini API key (or use the default from `.env.example`):
     ```env
     GEMINI_API_KEY=your_gemini_api_key_here
     ```
   - See `.env.example` for reference values

5. **Fix Build Configuration**
   - Open `app/build.gradle.kts`
   - Locate the `debug` section under `buildTypes`
   - **Remove** this line:
     ```gradle
     signingConfig = signingConfigs.getByName("debugConfig")
     ```
   - This prevents debug keystore signing conflicts during development

6. **Build the Project**
   - Select **Build → Make Project** or press `Ctrl+F9` (Windows/Linux) / `Cmd+F9` (Mac)
   - Wait for the build to complete successfully

7. **Run the App**
   - Select **Run → Run 'app'** or press `Shift+F10` (Windows/Linux) / `Ctrl+R` (Mac)
   - Choose an Android emulator or connected physical device
   - Emulator requirements: **API 24+** (Android 7.0 or higher)

8. **Grant Permissions**
   - On first launch, the app will request SMS and notification permissions
   - Grant these permissions to enable full functionality

### Targeting Android Versions
- **minSdk**: 24 (Android 7.0)
- **targetSdk**: 36 (Android 15)
- **compileSdk**: 36

---

## 📝 Permissions

The app requires the following permissions:

| Permission | Purpose |
|-----------|---------|
| `RECEIVE_SMS` | Intercept incoming bank payment alerts |
| `READ_SMS` | Parse and extract transaction data from SMS |
| `INTERNET` | Send transactions to OpenPay backend |
| `POST_NOTIFICATIONS` | Show sync status notifications (Android 13+) |

Users are prompted to grant permissions on first pairing, with a clear explanation card.

---

## 🧪 Testing

### Local Testing Checklist
- [ ] Device pairs successfully with valid merchant code
- [ ] SMS from known banks triggers forwarding
- [ ] Sync log updates in real-time
- [ ] Failed transactions show error status
- [ ] Auto-sync toggle prevents/enables forwarding
- [ ] Permissions are requested and honored
- [ ] Clearing log removes all entries
- [ ] Unlinking device resets pairing status

### Debug Logging
Check Logcat for debug messages:
```bash
adb logcat | grep "SmsReceiver\|MainActivity\|DashboardScreen"
```

---

## 📦 Dependencies

Key dependencies (see `app/build.gradle.kts`):

- **Jetpack Compose**: Modern UI framework
- **Retrofit**: HTTP client for API communication
- **Moshi**: JSON serialization/deserialization
- **Room**: (Commented; for future use) Local database
- **Coroutines**: Async operations in SMS receiver
- **OkHttp**: HTTP logging interceptor

---

## 🔐 Security Considerations

- **Device Pairing**: Merchant code validated server-side
- **HTTPS**: All API calls use HTTPS to backend
- **Permissions**: SMS permissions explicitly requested and explained
- **Data Storage**: Merchant details stored in SharedPreferences (encrypted on modern Android)
- **SMS Filtering**: Only processes messages from known banks/providers

---

## 🚀 Deployment

### Building for Release

1. **Generate or Import Release Keystore**
   ```bash
   keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```

2. **Set Environment Variables**
   ```bash
   export KEYSTORE_PATH=/path/to/my-upload-key.jks
   export STORE_PASSWORD=your_password
   export KEY_PASSWORD=your_password
   ```

3. **Build Release APK**
   ```bash
   ./gradlew assembleRelease
   ```

4. **Output Location**
   - APK: `app/build/outputs/apk/release/app-release.apk`

---

## 📱 App Workflow

### First-Time User
1. Launch app → See PairingScreen
2. Enter merchant code → Tap "Pair Device"
3. App sends device info to backend
4. On success → Redirected to DashboardScreen
5. Grant SMS/Notification permissions when prompted
6. Ready to receive payment alerts!

### Merchant
1. Opens app → Sees DashboardScreen with sync history
2. Incoming bank alert → SMS intercepted automatically
3. Backend parses transaction details
4. Log entry appears in dashboard in real-time
5. Can view details, toggle auto-sync, or unlink device

---

## 🤝 Contributing

1. Create a feature branch
2. Make changes and test thoroughly
3. Push to GitHub
4. Open a pull request with detailed description

---

## 📄 License

This project is part of the OpenPay ecosystem. See repository for license details.

---

## 💬 Support

For issues or questions:
- Check [OpenPay Docs](https://open-pay-822w.vercel.app/dashboard/docs)
- Review code comments for implementation details
- Open an issue on GitHub

---

**Version**: 1.0  
**Last Updated**: June 2026  
**Built With**: Kotlin, Jetpack Compose, Retrofit
