# Susun Kata

Game susun kata (gaya Word Connect) berbahasa Indonesia untuk Android, ditulis dengan Kotlin murni
(tanpa library tambahan). Seluruh tampilan digambar dengan Canvas, aset dibuat prosedural lewat skrip.

- **Developer:** Hanz Dev Official
- **Kontak:** hanzzcodee@gmail.com
- **Dukung:** https://saweria.co/hanzreally

## Cara main
Geser jari melewati huruf di roda untuk menyusun kata, lalu angkat jari. Kata yang cocok mengisi kotak.
Kata valid lain menjadi **kata bonus** (+5 koin). Petunjuk: target (50), bohlam (25), tiga bohlam (100),
hadiah harian (+50), dan tombol acak.

## Build release di GitHub
1. Buat repo GitHub, lalu push seluruh isi folder ini ke branch `main`.
2. Buka tab **Actions** → workflow **Build Release** berjalan otomatis.
3. Setelah hijau, unduh APK/AAB dari **Artifacts** (`SusunKata-release`).
4. Untuk membuat halaman Release: `git tag v1.0.0 && git push origin v1.0.0`.

Build memakai `assembleRelease` + `bundleRelease` (R8 aktif, resource dikecilkan), bukan debug.

### Keystore tetap (disarankan)
Tanpa secret, CI membuat keystore sementara sehingga tiap build bertanda tangan berbeda
(tidak bisa meng-update aplikasi yang sudah terpasang). Buat sekali di komputer:

```bash
keytool -genkeypair -v -keystore susunkata.jks -storetype PKCS12 -alias susunkata \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 susunkata.jks > keystore.b64
```

Lalu di GitHub: **Settings → Secrets and variables → Actions** tambahkan
`KEYSTORE_BASE64` (isi `keystore.b64`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
Simpan file `.jks` di tempat aman; jangan di-commit (sudah ada di `.gitignore`).

## Struktur
- `app/src/main/java/com/hanzdev/susunkata/` — kode game (Kotlin)
  - `GameState.kt` model + simpan progres, `GameView.kt` tampilan, `Icons.kt` ikon,
    `Fx.kt` suara/getar/pengaturan, `Dialogs.kt` dialog, `LevelRepo.kt` pemuat level,
    `MainActivity.kt` menu, `GameActivity.kt` layar game
- `app/src/main/assets/levels.json` — 105 level (dibuat dan diverifikasi oleh `tools/generate_levels.py`)
- `tools/generate_levels.py` — pembuat level dari `tools/kata.txt` (tambah kata di sana, jalankan ulang)
- `tools/generate_assets.py` — pembuat latar, ikon, dan efek suara (butuh Pillow dan numpy)
- `ASSETS.md` — daftar aset dan asal-usulnya

## Menambah level
Tambahkan kata baku ke `tools/kata.txt`, jalankan `python3 tools/generate_levels.py`, commit
`levels.json`. Generator memverifikasi tiap level (kata valid, huruf cukup, grid tersambung, tanpa kata liar);
CI menjalankannya lagi sebelum build.
