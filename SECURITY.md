# Security policy

## Supported version

Security fixes are made on the latest version of the app and the default branch.

## Reporting a vulnerability

Please report vulnerabilities privately through GitHub's **Security → Report a vulnerability**
feature. Do not include scanned QR contents, Wi-Fi credentials, contact details, or other personal
data in a report. If a minimal reproduction needs a payload, use synthetic data.

Reports should include the affected app version, Android version, expected behavior, observed
behavior, and minimal reproduction steps. Confirmed issues will be acknowledged and prioritized
according to their user impact.

## Security model

- Scanning and generation are performed locally.
- The app does not request network access and contains no advertising or analytics SDK.
- Sensitive payload categories are excluded from history.
- App backup is disabled.
- External actions require Android or the destination app to handle a user-visible intent.
