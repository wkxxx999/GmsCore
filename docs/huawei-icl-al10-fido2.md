# Huawei ICL-AL10 (Android 12) microG FIDO2 exploratory patch

## Goal

Improve compatibility of existing microG FIDO2 privileged browser APIs. This is **not** Google Play Store and does not certify a device or implement all proprietary Google Play Services features.

## Changes

- Implement a subset of `getCredentialList(rpId)` for **existing** local screen-lock credentials (known RP ID + valid Android Keystore key + stored user information).
- Only reveal the credentials to a previously-approved browser package/signature (or the service's own UID).
- Keep biometric UI, attestation and WebAuthn signature logic unchanged.
- Add GitHub Actions build of the `hmsHuaweiDebug` app flavor.

## Tests required

1. Verify the GitHub Actions build completes.
2. Verify the debug APK signature and installation compatibility; do not uninstall the production microG app as a shortcut.
3. On the device, verify Android secure lock screen, fingerprint, Chrome version, microG version and signature spoofing support.
4. Test registration and authentication on a non-sensitive WebAuthn demonstration site before attempting a Google Account passkey.
5. Check logcat for service binding, FIDO2 availability callback, pending intent, verification and Keystore failures.
6. Verify unapproved packages receive no credential details.

## Current limitations

- The patch affects listing **already registered** local keys; it does not in itself fix a grayed-out “use this device” option during first passkey registration.
- Privileged browser requests still depend on OS/runtime integration and microG compatibility.
- Debug builds have a different signing key from the official Huawei microG APK, so **do not install over an existing signed production app or erase its data**.
- A successful APK build does not guarantee Google accepts device attestation, Play Integrity, Google Pay, or account passkey registration.
- This is an experimental branch, not a production release.

## Source

`play-services-fido/core/src/main/kotlin/org/microg/gms/fido/core/privileged/Fido2PrivilegedService.kt`

`play-services-fido/src/main/java/com/google/android/gms/fido/fido2/api/common/FidoCredentialDetails.java`

CI: `.github/workflows/huawei-fido2-debug.yml`
