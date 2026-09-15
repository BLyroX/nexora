# Nexora

**Nexora** re-packs the **CS 1.6 (Xash3D) Android** client APK so it ships **AMX Mod X**
with **Metamod-P** and a full plugin bundle embedded — one tap, no server-side AMXX setup.
The patched APK is re-signed and installable directly.

Only **arm64-v8a (64-bit)** is supported.

## Patch

1. Pick the **source APK** — the CS 1.6 client `.apk` you downloaded or keep around.
2. Nexora injects **AMXX + Metamod-P + addons**, re-signs, and emits a new APK.
3. **Install** updates the app in place (application id + signature stay the same, your data is kept).

## Compile (plugins)

- Nexora embeds a **64-bit-cell compiler (pawncc)**.
- In **Compile** pick an `.sma` source file → it is built to `.amxx` (native 64-cell binary)
  → added to the **Addons** bundle → applied on the next **Patch**.
- 32-bit-cell `.amxx` plugins are intentionally rejected with a clear error.

## Addons (bundle)

- The app (and CI) packages AMXX modules, metamod, the compiler and sample plugins into a
  **bundle** zip; it is embedded in the APK so it works **offline**.
- The **Addons** tab lists the bundle contents (core, modules, plugins) and fetches the latest
  release from the internet or falls back to the embedded copy.

## Repository layout

- `android/app/` — the patcher APK (Jetpack Compose: Patch · Compile · Addons).
- `android/ci/` — build scripts: AMXX (64-cell) + pawncc + metamod + bundle assembly.
- `patches/` — the ordered 64-bit patch set applied on top of upstream AMXX/pawncc/metamod.
- `android/hlsdk/` — Half-Life SDK headers required by the AMXX build.

## CI / building

`.github/workflows/` first cross-compiles AMXX 64-cell (core + modules + host pawncc), packs the
bundle, then assembles + signs the patcher APK. Locally:

```sh
bash android/ci/build-amxx.sh "$PWD" "$NDK_ROOT" out
```

## Status

- On-device runtime validation of a fully patched APK: pending.
- User changes under `addons/` are overwritten on the next repatch.
