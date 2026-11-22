# ChatKeep — Naufal

**Aplikasi Android Pemulih Chat & Notifikasi Terhapus (Offline-First)**  
*Package:* `id.my.naufalputra.chatkeep`  
*Platform:* Android (Min SDK 26 / Android 8.0 Oreo, Target SDK 35 / Android 15)  

---

## 🌟 Fitur Utama
1. **Perekaman Notifikasi Real-Time:** Menangkap notifikasi pesan masuk dari WhatsApp, WhatsApp Business, dan Instagram secara instan sebelum ditarik oleh pengirim.
2. **Deteksi Pesan Dihapus:** Secara otomatis menandai pesan yang ditarik ("Pesan ini telah dihapus" / "This message was deleted") dengan badge merah dan bingkai khusus.
3. **Penyimpanan 100% Offline (Room Database):** Semua teks tersimpan di database SQLite lokal perangkat. Tidak ada server eksternal, tidak ada analitik, tidak ada pelacakan data.
4. **Anti-Slop Design System (Jetpack Compose Material 3):**
   - Palet warna kustom: **Deep Teal** & **Obsidian Dark Mode**.
   - Huruf sans-serif dengan angka tabular (`tnum`).
   - Flat surface dengan hairline outline (`0.5.dp`), elevasi `0.dp`.
5. **Dukungan Teks Panjang:** Menangani `EXTRA_BIG_TEXT` agar pesan multi-baris tidak terpotong.
6. **Layanan Latar Belakang Tangguh:**
   - `NotificationListenerService` terintegrasi mode Foreground Service berprioritas rendah (`specialUse` Android 14/15) agar tidak dimatikan paksa oleh OS.
   - Tombol pintasan menuju pengaturan pengabaian optimasi baterai (Ignore Battery Optimization).
7. **Pembersihan Otomatis (WorkManager):** Penjadwalan pembersihan pesan lama secara otomatis (pilihan: 7, 14, 30 hari, atau simpan selamanya).
8. **Kepatuhan Google Play Store:**
   - Prominent Disclosure dialog sebelum mengarahkan ke halaman izin.
   - Izin runtime `POST_NOTIFICATIONS` untuk Android 13+.
   - Draf lengkap Privacy Policy (ID & EN) dan panduan pengisian Data Safety.

---

## 📁 Struktur Direktori
```text
ChatKeep-Naufal/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/id/my/naufalputra/chatkeep/
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── ChatDao.kt
│       │   │   ├── ChatDatabase.kt
│       │   │   └── ChatMessageEntity.kt
│       │   ├── service/
│       │   │   └── ChatKeepNotificationService.kt
│       │   ├── ui/
│       │   │   ├── ChatKeepScreen.kt
│       │   │   ├── ChatViewModel.kt
│       │   │   └── theme/
│       │   │       ├── Theme.kt
│       │   │       └── Type.kt
│       │   └── worker/
│       │       └── AutoCleanWorker.kt
│       └── res/
│           ├── drawable/
│           ├── mipmap-anydpi-v26/
│           └── values/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── PRD.md
├── DESIGN.md
├── PRIVACY_POLICY.md
├── icon.svg
└── README.md
```

---

## 🚀 Panduan Membuka di Android Studio
1. Buka **Android Studio**.
2. Pilih **Open** lalu arahkan ke folder: `F:\Naufal\AI\ChatKeep-Naufal`.
3. Biarkan Android Studio melakukan sinkronisasi Gradle (**Sync Project with Gradle Files**).
4. Hubungkan HP Android fisik atau jalankan emulator (Android 8.0 ke atas, disarankan Android 13/14/15).
5. Klik **Run 'app'** (tombol Play hijau atau Shift+F10).
6. Saat aplikasi pertama kali terbuka:
   - Izinkan notifikasi runtime (Android 13+).
   - Tekan **Aktifkan** pada banner akses notifikasi.
   - Baca dialog Prominent Disclosure, tekan **Setuju & Lanjutkan**.
   - Aktifkan toggle **ChatKeep — Naufal** di halaman pengaturan Notification Access Android.
   - Buka menu **Pengaturan** di aplikasi lalu atur **Abaikan Optimasi Baterai**.
