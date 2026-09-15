# Nexora

**Nexora**, Android'deki **CS 1.6** istemci uygulamasına, uygulamayı yeniden kurmadan
**AMX Mod X** ve **eklentileri** yerleştiren bir araçtır. Uygulamayı seç, **Patch**'e bas,
yeni APK'yı yükle — oyunun içinde AMXX çalışmaya başlar.

## Nasıl çalışır

1. **Patch** — Cihazındaki CS 1.6 istemci `.apk`'sını seç. Nexora, içine AMXX çekirdeğini,
   Metamod-P'yi ve eklenti paketlerini ekler, yeniden imzalar ve hazır APK'yı üretir.
2. **Install** — Üretilen APK'yı doğrudan yükle. Uygulama kimliği ve imza korunduğu için
   mevcut hesap/oyun verilerin silinmez.
3. **Compile** — `addons/amxmodx/scripting/` klasörüne `.sma` kaynak kodu koyduysan,
   Nexora onları .amxx eklentisine derler ve bir sonraki Patch'e dahil eder.
4. **Addons** — Tüm AMXX paketi uygulamanın içine gömülüdür; internet olmadan da çalışır.

## Kurulum

- İndir: **Releases** sayfasından son `Nexora-*.apk`'yı indir.
- Yükle: "Bilinmeyen kaynaklar" iznini ver ve APK'yı kur.
- Uygulamayı aç, **Patch** sekmesinden cihazındaki CS 1.6 APK'sını seç.

## Gereksinimler

- Cihaz **arm64 (64-bit)** desteklemeli (2015 sonrası Android telefonların tamamı).
- Android 8.0+ (API 26+).
- Kaynak CS 1.6 istemci APK'sı (Google Play / APKMirror / kendi yedeğin).

## Build (geliştiriciler için)

CI, `.github/workflows/` altında:
1. AMXX'ı **64-cell hücre** yapısıyla ve Metamod-P ile çapraz derler,
2. eklenti derleyicisini (pawncc) ve örnek `.sma` → `.amxx` derlemelerini paketler,
3. patcher APK'sını kurup imzalar.

Yerel derleme:

```sh
bash android/ci/build-amxx.sh "$PWD" "$NDK_ROOT" out
```

## Sorun giderme

- **"assert(litidx==0)" hatası** — Nexora, çok sayıda dize içeren eklentilerin 64-cell
  ortamda derlenmesini güvenli hale getiren bir düzeltme içerir. Plugin derlemesi sırasında
  bu hatayı görürsen Nexora güncel olduğundan emin ol.
- **32-bit eklenti uyarısı** — 64-cell ortamda 32-bit `.amxx` dosyaları çalışmaz;
  eklentiyi kaynaktan (`.sma`) yeniden derle.

## Katkı

PR'ler ve fikirler açık. Lütfen değişikliklerini açıklayan bir özetle gel.
