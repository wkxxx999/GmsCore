# Claude Code — Huawei / microG FIDO2 workstream

This repository is a fork of microG GmsCore. Your assigned target is HUAWEI ICL-AL10, Android 12 (API 31), arm64-v8a, Huawei microG v0.3.17.252432-hw.

## User's actual goals
1. Make the Google Account **"use this device"** passkey-registration option usable with the phone's existing fingerprint / secure screen lock, if achievable within the microG APIs and Google's server-side policy.
2. Improve Google Play compatibility without breaking the installed microG, Google Services Framework (GSF), and genuine Play Store.
3. Identify the documented prerequisites and limits of Play Protect device certification and Play Integrity. Do not claim that patching microG can confer Google official certification.

## What is known
- Chrome WebAuthn.io registration and authentication using a **platform authenticator** and fingerprint prompt both succeed on the user's current device.
- The Google Account passkey creation **"use this device"** option remains grey/unavailable.
- This means the fingerprint hardware and a basic platform WebAuthn path work; it does **not** prove Google Password Manager support, account acceptance, or that microG handled the requests.
- Existing feature PR #1 added a **getCredentialList** implementation for *existing* local screenlock credentials in `play-services-fido/core/src/main/kotlin/org/microg/gms/fido/core/privileged/Fido2PrivilegedService.kt`. This is not a complete Google Account passkey fix.
- The built modified GmsCore APK uses an **Android Debug certificate** and **cannot be installed over** the production-signed `com.google.android.gms` package. Prior debug APK success does not prove runtime compatibility.
- The user explicitly rejects further rounds of generic diagnostics, installations, screenshots or test requests.

## Engineering priorities
1. Inspect the exact **Android 12 / Chrome / Google account passkey registration** call path in the repository. Distinguish the legacy FIDO2 privileged Binder API from the Android API 34+ CredentialProviderService (the latter does not run as a system credential provider on API 31).
2. Find and fix an evidenced GmsCore implementation gap that can affect the device's ability to offer a platform authenticator for Google passkey creation. Investigate FIDO2 capability responses, browser privilege, attestation, Google Password Manager integration, and API entry points only as supported by actual code. Do not infer a working repair from WebAuthn.io alone.
3. Implement focused unit-level tests (or other safe local tests) and compile with existing CI. A successful build is only a code-build result, not an attestation of Google acceptance.
4. Preserve existing signing identities/data. Any upgrade proposal must explicitly address signature matching, versionCode monotonicity, and secure backup/rollback; do not tell the user to uninstall existing microG to test.
5. Keep any new credentials, keys and logs out of Git, issue comments and PR descriptions. Never solicit user account passwords, passkeys, Android keystore values, secret tokens, or fingerprints.

## Relevant paths
- `play-services-fido/core/src/main/kotlin/org/microg/gms/fido/core/privileged/Fido2PrivilegedService.kt`
- `play-services-fido/core/src/main/kotlin/org/microg/gms/fido/core/regular/Fido2AppService.kt`
- `play-services-fido/core/src/main/kotlin/org/microg/gms/fido/core/ui/AuthenticatorActivity.kt`
- `play-services-fido/core/src/main/AndroidManifest.xml`
- `play-services-core/src/main/kotlin/org/microg/gms/auth/credentials/provider/` (API 34+ only)
- `.github/workflows/huawei-fido2-debug.yml`

## Delivery standards
- Small, reviewable, focused commits on the feature branch and PR #1.
- Document the specific code path, the precise deficiency, changed behavior, automated test coverage and unresolved Google/device-policy dependencies.
- Never claim Google official certification, Play Integrity success or working Google passkey creation without evidence.
- Respond to the repository owner in concise Chinese.
