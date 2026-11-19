# Jalur satu aplikasi dari HP — belum waktunya uninstall

Status: pengguna mengizinkan mulai ulang tanpa koleksi/profil/riwayat lama dan hanya memakai HP. **Belum ada APK 0.1.2 dengan kunci tetap yang lolos seluruh tes dan diterbitkan. Jangan hapus aplikasi sekarang.**

## 1. Selesaikan pengaturan kunci build dahulu

Ikuti [setup secret signing](UPDATE-IDENTITY.md). File JSON privat di Drive adalah cadangan **kunci penandatangan APK**, bukan backup data aplikasi. Jangan kirim isinya ke chat/issue atau membuatnya publik. Tidak memerlukan PC; secret dapat ditambahkan melalui browser HP.

Setelah secret tersimpan, agent harus menjalankan build bertanda tangan, dua suite Android, dan tes pemasangan/reset/update yang sebenarnya. Tes mock atau source-check yang hijau tidak menggantikan tes itu. APK probe versi lebih tinggi hanya untuk CI, bukan untuk diunduh pengguna.

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
- Jangan menganggap hasil emulator sebagai jaminan HiOS, pemulihan sesi atau peningkatan FPS.

## Pembaruan setelah reset ini

Paket tetap `id.veldafelix.spaces.preview2`, kunci tetap yang dicadangkan, dan versionCode meningkat. CI harus membuktikan update mempertahankan data dan tidak menambah paket. Pergantian kunci atau reset berikutnya **tidak** otomatis diizinkan oleh persetujuan kali ini. Jangan menghapus cadangan kunci privat di Drive; simpan juga salinan privat milik sendiri jika memungkinkan.
