# Nexora

**Nexora** repacks the CS1.6 Android client APK so it bundles **AMX Mod X** compiled for
**64-bit cells** (`PAWN_CELL_SIZE=64`, ABI `arm64-v8a` only), then re-signs it. No server-side
AMXX install is needed — the patched APK is self-contained and runnable on any aarch64 Android
device.

It is a *companion/cell-sized* sibling of the generic 64-bit AMXXX builds: the core difference is
that here the literal pool is sized for 64-bit cells, so a plugin uses twice the bytes the same
plugin would use on a 32-bit cell build.

## Why 64-bit cells

AMX's cell is `sizeof(cell)` bytes wide — 4 on standard pawn32, **8 on this build**. A pointer
(stored in a cell) needs all 8 bytes on arm64, so every cell-related macro and the literal queue
are resized for 64. A 32-cell `.amxx` plugin is rejected by the 64-core at load time, which is why
the shipped plugins and the CI compiler are also 64-cell.

## What happens

The CI ([`android/ci/build-amxx.sh`](android/ci/build-amxx.sh), [workflow](.github/workflows)):

1. Clones upstream `alliedmodders/amxmodx` master and applies [`patches/`](patches/) **in order**
   (mostly 64-bit-cell adaptations: CDetour cell casts, AMTL 64-bit, pawncc literal pool, Android
   metamod/AMXX loading, floats, pcvar handles, param convert, cbase/pev, and the HAM trampoline
   on arm64).
2. Cross-compiles with the NDK: AMXX core + modules + metamod-p + the **host compiler** `pawncc`
   (also PAWN_CELL_SIZE=64 so it can compile plugin-heavy `.sma` without aborting on the literal
   queue assert).
3. Compiles sample plugins from `.sma` → `.amxx` with that compiler.
4. Packs everything into release artifacts and uploads them; the Android patcher APK embeds them
   so the app works even offline.

## Layout

- `patches/` — the ordered patch set CI applies to upstream amxmodx / metamod / pawncc.
- `android/ci/` — the shell build scripts run by the workflow.
- `android/app/` — the patcher APK (Compose UI pick→patch→re-sign).
- `patches/*.patch` files whose names start with `amxmodx-` are applied to the `amxmodx` source;
   `amxmodx-pawncc-64bit.patch` etc. adapt the compiler.

## Building locally

Requires the Android NDK and a GH token only for the release step; a normal build is:

```sh
bash android/ci/build-amxx.sh "$PWD" "$NDK_ROOT" out
```

## Status

On-device runtime validation (the patched APK actually running an AMXX plugin) is still pending;
compile and packaging are exercised in CI.
