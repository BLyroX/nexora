# Nexora

**Nexora** repacks the **CS1.6 Android client (Xash3D)** APK so the patched app bundles
**AMX Mod X (actı 64-bit hücre)** + **Metamod-P** + the AMXX addons, then re-signs it.
No server-side AMXX install is needed — the patched APK is self-contained.

Only **arm64-v8a** builds are supported (that's the ABI Xash3D ships on Android); arm32/x86
APKs are rejected with a clear error.

## Why 64-bit cells?

AMX's cell on this Android build is **64 bits** (`PAWN_CELL_SIZE=64`), so a pointer fits in
one cell. Everything here — compiler, AMXX core, metamod, every module, and the `pawncc`
host compiler — is built with 64-bit cells maturely patched for literal-heavy plugins
(literal pool sized/flushed cell-aware, so compiling `.sma` with many const-strings no longer
aborts with `assert(litidx==0)` on the 64-cell path). 32-bit-cell `.amxx` plugins are rejected
at load time.

## How the build works (CI)

`.github/workflows/…`:

1. **build-amxx** — fetches upstream `alliedmodders/amxmodx` master, applies
   [`patches/`](patches/) **in order** (64-bit cell casts, AMTL 64-bit, pawncc 64-bit
   literal-pool, metamod-p aarch64/Android module loading, HAM/cbase/pev adapters, module
   dlopen), and cross-compiles with the NDK: AMXX core + all 11 modules + metamod-p +
   a 64-cell host `pawncc`.
2. **amxx-bundle** — `gen-bundle.py` produces `amxx-bundle.zip` (libraries + addons +
   bundle.json) and uploads it as a release artifact; the embedded bundle in the APK is the
   offline fallback so patching works even without network.
3. **app-apk** — embeds the bundle into the patcher Android app and assembles + signs the
   patcher APK (`applicationId` and signing keystore are preserved, so the patched CS1.6
   app updates in place).

Plugins (`.sma`) shipped in the bundle are compiled to `.amxx` by that same 64-cell `pawncc`
during CI and land in `addons/amxmodx/plugins` with their configs.

## Repository layout

- `patches/` — the ordered patch set applied to upstream AMXX/pawncc/metamod.
- `android/ci/` — shell build scripts (NDK cross-compile, bundle, patch).
- `android/app/` — the Android patcher (Compose UI + patch/sign pipeline + offline bundle).
- `android/hlsdk/` — vendored Half-Life SDK headers needed by the AMXX build.

## Building locally

Requires the Android NDK; a normal non-release build is just:

```sh
bash android/ci/build-amxx.sh "$PWD" "$NDK_ROOT" out
```

## Status

CI compiles AMXX (64-cell) + a 64-cell `pawncc`, compiles the sample plugins, packs the
bundles and builds + signs the patcher APK. On-device runtime validation of a fully patched
APK is still in progress.
