# Nexora

**Nexora** patches the CS 1.6 client APK on your Android device to bundle
**AMX Mod X** and your plugins, so AMXX runs without ever touching the source
app twice. Pick the APK, hit **Patch**, install the output — done.

## How it works

1. **Patch** — Select the CS 1.6 client `.apk` on your device. Nexora injects the
   AMXX core, Metamod-P and the plugin set, re-signs it)Skip, and produces a ready
   to-install APK.
2. **Install** — Install the generated APK. The app identity and signature are kept,
   so your account and game data survive.
3. **Compile** — Drop `.sma` sources into `addons/amxmodx/scripting/`. Nexora compiles
   them to `.amxx` and includes them at the next Patch.
4. **Addons** — The full AMXX package is embedded in the app, so it works offline.

## Install

- Download the latest `Nexora-*.apk` from the **Releases** page.
- Allow "install from unknown sources" and install the APK.
- Open the app and pick your CS 1.6 APK from the **Patch** tab.

## Requirements

- **arm64 (64-bit)** device — every Android phone from 2015 onward.
- Android 8.0+ (API 26).
- A CS 1.6 client APK (Google Play, APKMirror, or your own backup).

## Build (for developers)

The CI under `.github/workflows/`:
1. Cross-compiles AMXX with **64-cell** integers and Metamod-P,
2. Bundles the plugin compiler (pawncc) and sample `.sma` → `.amxx` outputs,
3. Patches the launcher APK and signs it.

Local build:

```sh
bash android/ci/build-amxx.sh "$PWD" "$NDK_ROOT" out
```
