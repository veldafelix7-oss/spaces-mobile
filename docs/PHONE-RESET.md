# Pembaruan tanpa reset — kandidat 0.1.4

Jika sudah memakai **0.1.2 atau 0.1.3**, revisi 0.1.4 disiapkan sebagai **update langsung** dengan package dan signing key yang sama, versionCode 5. **Jangan uninstall, hapus data, atau mengulangi reset.** Akhiri/pulihkan sesi lebih dulu dan pasang hanya setelah link test preview yang lolos seluruh pemeriksaan tersedia. Riwayat lama, profil dan draft sesi harus tetap terbaca; tidak ada penggabungan otomatis dengan paket 0.1.0 yang berbeda.

Bagian di bawah adalah **arsip perpindahan sekali ke 0.1.2**, bukan instruksi reset untuk update 0.1.4.

---

# Jalur satu aplikasi dari HP — test preview 0.1.2 tersedia

Status: pengguna mengizinkan mulai ulang tanpa koleksi/profil/riwayat lama dan hanya memakai HP. **Test preview 0.1.2 sudah lolos tes otomatis dan tersedia**, bukan aplikasi final atau jaminan masalah HiOS sudah hilang. [Unduh APK](https://github.com/veldafelix7-oss/spaces-mobile/releases/download/preview-0.1.2-8830907/spaces-mobile-preview.apk) · [hasil tes](TEST-RESULTS.md). **Jangan uninstall sebelum langkah pemulihan di bawah selesai dan APK tersimpan di HP.**

## 1. Kunci build dan revisi sudah lolos tes otomatis

Secret signing sudah dikonfigurasi dan berhasil dipakai dalam tes nyata. Tidak perlu mengaturnya ulang. [Bukti dan kebijakan](UPDATE-IDENTITY.md). File JSON privat di Drive adalah cadangan **kunci penandatangan APK**, bukan backup data aplikasi; simpan tetap privat.

Revisi sidebar pada sumber `8830907` sudah lolos build bertanda tangan, 64 tes unit, dua suite Android 31/31, dan tes pemasangan/reset/update yang sebenarnya dalam run `44447692351`. Tes mock atau source-check saja tidak menggantikan tes itu. APK probe versi lebih tinggi hanya untuk CI, bukan untuk diunduh pengguna.

## 2. Hanya setelah APK baru siap dan unduhannya tersedia

1. Buka masing-masing aplikasi lama yang pernah dipakai menjalankan sesi. Akhiri sesi melalui **Akhiri sesi & pulihkan**. Jika muncul pemulihan tertunda, jangan uninstall: selesaikan dahulu izin/Shizuku/pemulihannya dan periksa pengaturan sistem. Mengizinkan hilangnya riwayat tidak berarti boleh kehilangan jurnal pemulihan ketika pengaturan sesi masih aktif.
2. Pastikan tidak ada sesi/notifikasi layanan yang masih berjalan. Periksa DND/refresh/mode yang sempat diubah; pemulihan tidak dijamin bisa mengatasi setiap perubahan eksternal/OEM.
3. Setelah agent memastikan APK 0.1.2 lolos tes, unduh APK yang benar **sebelum** menghapus instalasi lama.
4. Instalasi berlabel **spaces : mobile · 0.1.1** harus dihapus sekali untuk mengganti kuncinya. Uninstall menghapus koleksi, profil, riwayat dan izin lokal instalasi itu. Android tidak akan menerima APK dengan kunci berbeda sebagai update langsung. Jangan mengaktifkan opsi mempertahankan data lama jika ditawarkan.
5. Jika ingin menyisakan satu aplikasi, instalasi 0.1.0 **spaces : mobile** yang berbeda juga dapat dihapus setelah sesi/pemulihannya selesai. Ini turut menghapus data lokal instalasi tersebut; tidak ada penggabungan otomatis.
6. Pasang APK 0.1.2 baru. Koleksi mulai kosong. Atur ulang koleksi, izin opsional dan koneksi Shizuku seperlunya. Jangan membuat sesi baru sebelum langkah pemulihan aplikasi lama selesai.

Agent tidak memiliki akses untuk menekan uninstall atau mengubah data di HP. Langkah ini dilakukan pengguna sendiri hanya setelah APK siap.

## 3. Verifikasi singkat setelah pemasangan

- Konfirmasi petunjuk fullscreen Android jika muncul.
- Coba sidebar atas: posisi lebih rapat saat status bar tersembunyi, tap/swipe pertama, tahan-geser, dan tutup lewat chevron/Back.
- Pastikan kontrol/fokus game kembali normal setelah panel ditutup; cek kedua arah landscape dan saat status bar tampil sementara.
- Setelah bermain, gunakan **Akhiri & pulihkan**: sesi kini manual secara bawaan. Jika sidebar hilang, periksa notifikasi lalu **Perangkat → Detail sesi terakhir / sidebar hilang**; saat sesi masih hidup tersedia **Tampilkan ulang sidebar**.
- Jangan menganggap hasil emulator sebagai jaminan HiOS, pemulihan sesi atau peningkatan FPS.

## Pembaruan setelah reset ini

Paket tetap `id.veldafelix.spaces.preview2`, kunci tetap yang dicadangkan, dan versionCode meningkat. CI harus membuktikan update mempertahankan data dan tidak menambah paket. Pergantian kunci atau reset berikutnya **tidak** otomatis diizinkan oleh persetujuan kali ini. Jangan menghapus cadangan kunci privat di Drive; simpan juga salinan privat milik sendiri jika memungkinkan.
