# Privacy Policy

**Effective date:** October 10, 2026

Aarav Khanal built **QR Scanner & Reader [No ADs]** as a free, offline-first app. The app does not use advertising, analytics, tracking SDKs, accounts, or application-operated servers.

## Processing on your device

- Camera frames are processed temporarily on your device to identify QR codes and barcodes. Camera frames are not saved by the app.
- Images selected through the system gallery picker are decoded on your device. The app does not copy the selected image into its private storage or upload it.
- Generated QR images remain in memory until you save or share them. Saving uses Android MediaStore on Android 10 and newer and the system document picker on Android 7–9.
- Browser, map, contact, email, SMS, phone, sharing, and Wi-Fi actions are handed to compatible system or third-party apps through Android intents. Those apps apply their own privacy policies. Modern Android Wi-Fi addition requires system or user confirmation.

## Local history

When **Save to history** is enabled, the app stores non-sensitive scanned and generated values in a local Room database. Consecutive duplicate values are not stored again. History stays on the device until you delete an item, clear all history, uninstall the app, or clear the app's data.

Sensitive payload categories are excluded from history even when the setting is enabled. These categories are Wi-Fi credentials, contacts/vCards, email addresses and messages, telephone numbers, and SMS recipients or messages. When this privacy behavior is first applied, previously stored entries recognized as one of those categories are removed from local history. Full sensitive values are used only transiently to display a result or perform the action you request.

You can disable all future history storage in **Settings → Privacy → Save to history**. Disabling the setting does not remove existing non-sensitive history; use **Clear History** for that.

## Backups

Application backup is disabled. The local history database, preferences, cached share images, and other private app files are not intentionally included in cloud backup.

## Permissions

- **Camera:** scans codes through the camera.
- **Vibration:** provides optional scan feedback.

The app does not request Internet, location, broad storage, or Wi-Fi control permissions. Selecting, saving, opening, sharing, or adding content may display an Android system or another app's confirmation screen.

## Temporary shared files

Sharing a generated QR code creates a uniquely named image in the app's cache and grants the selected receiving app temporary read access. Old share-cache images are periodically removed and are also removable by Android as cache data.

## Changes and contact

This policy may be updated when the app's behavior changes. The effective date above identifies the current version. Questions can be sent to **khanalaarav2063@gmail.com**.
