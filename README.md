# Nexora

Nexora, Android'de **CS1.6 (Xash3D)** client APK'sını tek dokunuşla AMX Mod X'li hale
getiren bir **patcher uygulaması**. AMXX çekirdeği + Metamod-P + eklenti paketi APK'nın
**içine gömülü** olarak gelir — sunucuda AMXX kurman gerekmez. Uygulama paketler, imzalar
ve sonucu doğrudan yükleyebilir.

Yalnızca **arm64-v8a (64-bit)** desteklenir.

## Patch (Ana Sekme)

1. **Kaynak APK'yı seç** — depondan indirdiğin/istediğin CS1.6 client `.apk`'sını aç.
2. Nexora, içindeki tipik oyun dosyalarını okur, **AMXX + Metamod-P + eklentileri** ekler,
   yeniden imzalar ve yeni APK'yı üretir.
3. **Kur** butonuyla doğrudan aynı uygulama olarak günceller (uygulama kimliği/imza aynı
   kalır, verilerin korunur).

## Compile (Eklenti Derle)

- Nexora'nın içinde **64-bit cell derleyici (pawncc)** gömülüdür.
- `Compile` sekmesinde bir `.sma` kaynak dosyası seç → Nexora onu `.amxx` (native 64-cell
  binary) yapar → `Addons` paketine ekler → sonraki Patch'te otomatik gelir.
- 32-bit cell `.amxx` yüklemeleri derleme/kurulumda bilinçli reddedilir; uyumsuzluk
  uyarısı verilir.

## Addons (Mod Paketi / Bundle)

- Uygulama + CI, **bundle** adı verilen zip içinde AMXX modülleri, metamod, derleyici ve
  örnek eklentileri paketler; APK'ya gömülü olduğundan **çevrimdışı** da çalışır.
- `Addons` sekmesinde paketin içeriğini (çekirdek, modüller, plugin listesi) görür,
  internetten güncel sürümü çekebilir ya da gömülü olanı kullanırsın.

## Repository Layout

- `android/app/` — patcher APK (Jetpack Compose: Patch · Compile · Addons).
- `android/ci/` — build script'leri: AMXX (64-cell) + pawncc + metamod + bundle paketleme.
- `patches/` — upstream AMXX/pawncc/metamod'a uygulanan sıralı 64-bit patch seti.
- `android/hlsdk/` — AMXX derlemesi için gerekli SDK başlıkları.

## CI / Yapı

`.github/workflows/` dev/alanlarında önce AMXX 64-cell çekirdek+modüller+host pawncc
çapraz derler, bundle'ı paketler, sonra patcher APK'yı kurar+imzalar. Yerel:

```sh
bash android/ci/build-amxx.sh "$PWD" "$NDK_ROOT" out
```

## Durum / İyileştirmeye Açık

- On-cihaz çalışma doğrulaması: bekliyor.
- Kullanıcının `addons/` altında yaptığı değişiklikler sonraki repatch'te üzerine yazılır.
