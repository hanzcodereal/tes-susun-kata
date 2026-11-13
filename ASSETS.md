# Daftar aset

Semua aset dibuat dari kode di `tools/` (tanpa gambar, suara, atau model pihak ketiga).
Tidak ada aset hasil AI generatif; tidak ada musik.

| Aset | Berkas | Sumber | Lisensi |
|------|--------|--------|---------|
| Latar 6 tema (sawah, pantai, gunung, hutan, senja, malam), 720x1280 JPG | `res/drawable-nodpi/bg_1..6.jpg` | `tools/generate_assets.py` (prosedural) | milik proyek |
| Ikon aplikasi (adaptif + lawas) | `res/mipmap-*`, `drawable-nodpi/ic_launcher_*.png` | `tools/generate_assets.py`; bentuk huruf dari font Inter Black | proyek; Inter: SIL OFL 1.1 |
| 15 efek suara WAV 22,05 kHz mono | `res/raw/snd_*.wav` | `tools/generate_assets.py` (sintesis gelombang) | milik proyek |
| Ikon tombol (gerigi, bohlam, hadiah, dll.) | digambar di `Icons.kt` | kode | milik proyek |
| Daftar kata | `tools/kata.txt` | disusun manual dari kosakata umum | milik proyek |

Cek teknis: latar dilihat langsung dalam lembar kontak, ikon dites di ukuran 48 px sampai 512 px,
efek suara dinormalisasi dengan headroom sekitar -1 dBFS.
