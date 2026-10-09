# QR Scanner & Reader [No ADs]

A fast, modern, and completely ad-free QR Scanner and Barcode Reader built natively for Android using Jetpack Compose. Designed with a minimal UI and smooth user experience, it features dynamic light and dark modes, seamless Wi-Fi auto-connect, and comprehensive history management.

## 🌟 Features
- **Lightning Fast Scanning**: Instantly decode standard QR codes, barcodes, and URLs.
- **QR Code Generator**: Create custom QR codes for Text, URLs, Wi-Fi, Locations, Contacts (vCard), Email, Phone, SMS, and a powerful "All-in-One" format.
- **Download & Share**: Save generated QR codes directly to your device's Downloads folder for easy sharing.
- **Modern UI/UX**: Clean, minimalist design using Jetpack Compose with beautiful dynamic Light and Dark mode themes.
- **Smart Actions**: 
  - Automatically detect and connect to Wi-Fi networks from QR codes (supports Android 10+ and legacy APIs).
  - Open URLs directly in the browser.
  - Smart copy-to-clipboard functionality.
- **Upload from Gallery**: Scan QR codes from images saved on your device.
- **Torch Control**: Easily toggle the camera flashlight for scanning in low-light environments.
- **History Management**: Automatically save scanned items. Pin important codes to the top, or selectively delete history items with confirmation prompts.
- **No Ads, No Trackers**: 100% privacy-focused.
- **Fully Offline**: Does not require an internet connection to scan and decode.

## 📸 Screenshots

### Light Theme
| Scanner | Result | History | Settings |
| :---: | :---: | :---: | :---: |
| <img src="screenshots/1 white theme.png" width="200" alt="Scanner Light"> | <img src="screenshots/2 white theme.png" width="200" alt="Result Light"> | <img src="screenshots/3 white theme.png" width="200" alt="History Light"> | <img src="screenshots/4 white theme.png" width="200" alt="Settings Light"> |

### Dark Theme
| Scanner | Result | History | Settings |
| :---: | :---: | :---: | :---: |
| <img src="screenshots/1.png" width="200" alt="Scanner Dark"> | <img src="screenshots/2.png" width="200" alt="Result Dark"> | <img src="screenshots/3.png" width="200" alt="History Dark"> | <img src="screenshots/4.png" width="200" alt="Settings Dark"> |

## 💻 Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose & Material Design 3
- **Database**: Room (SQLite) & Jetpack DataStore (Preferences)
- **Architecture**: MVVM, Compose Navigation
- **Image Loading**: Coil
- **QR Core Engine**: ZXing (Zebra Crossing)

## 🛠️ How to Build from Source
Anyone can clone and build this application locally using Android Studio.

### Prerequisites
- **Android Studio** (Jellyfish or newer recommended)
- **JDK 17** or newer
- **Android SDK** API level 36 (Android 16)

### Build Instructions
1. **Clone the repository**:
   ```bash
   git clone https://github.com/Aaravkhanal/QRScannerReader.git
   cd QRScannerReader
   ```
2. **Open the project**:
   Open Android Studio -> `File` -> `Open...` and select the project directory.
3. **Sync Gradle**:
   Allow Android Studio to download dependencies and sync the Gradle files.
4. **Build the APK**:
   ```bash
   # Build a debug APK
   ./gradlew assembleDebug

   # Build a release Android App Bundle (AAB) for Play Store
   ./gradlew bundleRelease
   ```
5. **Install on Device**:
   ```bash
   ./gradlew installDebug
   ```

## 🐛 Bug Fixes & Improvements
- **Wi-Fi Connection Stability**: Fixed issues with legacy parsing and improved connection handling for both Android 10+ (using `WifiNetworkSuggestion`/`WifiNetworkSpecifier`) and older devices (using legacy `WifiManager`).
- **Dark Mode Theming**: Resolved hardcoded colors that broke UI visibility in dark mode. Complete migration to Material Theme ColorSchemes.
- **Manifest Routing**: Fixed `ClassNotFoundException` on app startup caused by namespace refactoring mismatches.
- **UI Enhancements**: Restructured the scanner screen overlay with explicit icons (Torch, Gallery, Switch Camera) matching the modern aesthetic.

## 🔐 App Permissions
To provide its functionality, the app requires the following permissions:
- `android.permission.CAMERA`: Required to scan QR codes and barcodes.
- `android.permission.VIBRATE`: Provides haptic feedback upon successful scan.
- `android.permission.INTERNET`: Used only for opening scanned URLs in the external browser.
- `android.permission.ACCESS_WIFI_STATE` & `CHANGE_WIFI_STATE`: Required to automatically connect to scanned Wi-Fi networks.
- `android.permission.ACCESS_FINE_LOCATION`: Required by Android system for Wi-Fi connection capabilities on certain API levels.
- `android.permission.WRITE_EXTERNAL_STORAGE`: Required to save generated QR codes to the device's storage on older Android versions (API 28 and below).

## 👨‍💻 Developer Details
- **Developer**: Aarav Khanal
- **Contact**: [khanalaarav2063@gmail.com](mailto:khanalaarav2063@gmail.com)
- **GitHub**: [Aaravkhanal](https://github.com/Aaravkhanal)
- **Website**: [github.com/Aaravkhanal](https://github.com/Aaravkhanal/)
- **Privacy Policy**: [Read Here](https://docs.google.com/document/d/1lsuML4n8tc4_V2ltfkkAxb57SFENPnabYSzk9h829mc/edit?usp=sharing)

## 📄 License
© 2026 Aarav Khanal. All rights reserved.
