# ChatKeep — Naufal v1.1

**Aplikasi Android Pemulih Chat, Media Terhapus & WhatsApp Status Saver (Offline-First)**  
*Package:* `id.my.naufalputra.chatkeep`  
*Platform:* Android (Min SDK 26 / Android 8.0 Oreo, Target SDK 35 / Android 15)  

---

## 🌟 Fitur Utama (v1.1)

### 1. 💬 Perekaman Chat & Notifikasi Multi-Aplikasi Real-Time
- Menangkap dan mencadangkan pesan masuk sebelum ditarik/dihapus oleh pengirim.
- **Aplikasi yang Didukung:**
  - WhatsApp & WhatsApp Business
  - Instagram Direct
  - Telegram & Telegram X
  - Facebook Messenger
  - LINE
  - X / Twitter
  - *Opsi toggle di Pengaturan:* **"Pantau Semua Aplikasi Pesan"**
- **Deteksi Pesan Dihapus Multibahasa:** Menandai otomatis pesan yang ditarik (*"Pesan ini telah dihapus"*, *"This message was deleted"*, dll.) dengan bingkai merah halus dan badge indikator.

### 2. 🖼️ Pemulihan Media Terhapus (Foto, Video & Voice Note)
- **Ekstraksi Gambar:** Menangkap lampiran gambar dari notifikasi secara otomatis dan menyimpannya ke penyimpanan privat aplikasi.
- **Deteksi Pesan Suara / Voice Note:** Mendeteksi rekaman audio / VN yang masuk, dilengkapi pemutar audio bawaan (*built-in audio player*) dengan tombol Play / Pause.
- **Tab Galeri Media:** Filter instan berdasarkan *Semua*, *Foto*, *Video*, atau *Suara*.

### 3. 📥 WhatsApp Status / Story Saver (1-Klik ke Galeri)
- Membaca dan menampilkan status WhatsApp kontak (Foto & Video) secara otomatis.
- **Kompatibel Android 11 s/d Android 15:** Menggunakan *Storage Access Framework (SAF)* untuk mengakses folder `.Statuses` dengan aman.
- **Simpan ke Galeri HP:** 1-klik untuk mengekspor status favorit langsung ke folder `Pictures/ChatKeep` atau `Movies/ChatKeep` di memori publik HP.

### 4. 🔒 Privasi 100% Offline-First
- Semua data chat dan media tersimpan secara lokal di database Room (SQLite) perangkat.
- **Nol Server Eksternal, Nol Analitik, Bebas Pelacak Iklan.**
- Prominent Disclosure Dialog & kepatuhan penuh terhadap kebijakan Google Play Store.

### 5. 🎨 Desain Bersih Anti-Slop (Jetpack Compose Material 3)
- Palet warna kustom: **Deep Teal** & **Obsidian Dark Mode**.
- Antarmuka 4 Tab Modern: **Pesan**, **Media**, **Status**, dan **Pengaturan**.
- Tampilan flat surface dengan outline hairline (`0.5.dp`), elevasi `0.dp`, dan tipografi tabular (`tnum`).

---

## 📁 Struktur Direktori Proyek
```text
ChatKeep-Naufal/
├── app/
│   ├── build.gradle.kts         # Jetpack Compose, Room + KSP, Coil, DocumentFile
│   └── src/main/
│       ├── AndroidManifest.xml  # BIND_NOTIFICATION_LISTENER_SERVICE, Foreground specialUse, SAF
│       ├── java/id/my/naufalputra/chatkeep/
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── ChatDao.kt
│       │   │   ├── ChatDatabase.kt
│       │   │   ├── ChatMessageEntity.kt
│       │   │   ├── StatusModel.kt
│       │   │   └── StatusSaverManager.kt
│       │   ├── service/
│       │   │   └── ChatKeepNotificationService.kt (Multi-App & Media Capture)
│       │   ├── ui/
│       │   │   ├── ChatKeepScreen.kt (4 Tabs: Chat, Media, Status, Settings)
│       │   │   ├── ChatViewModel.kt
│       │   │   └── theme/
│       │   │       ├── Theme.kt
│       │   │       └── Type.kt
│       │   └── worker/
│       │       └── AutoCleanWorker.kt
│       └── res/
│           ├── drawable/        # ic_notif, ic_launcher_background & foreground
│           ├── mipmap-anydpi-v26/
│           ├── mipmap-*/        # PNG icons (mdpi to xxxhdpi)
│           ├── values/          # strings.xml, themes.xml
│           └── xml/             # file_paths.xml (FileProvider)
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

## 🚀 Panduan Kompilasi & Menjalankan di Android Studio
1. Buka **Android Studio**.
2. Pilih **File > Open** lalu pilih folder `F:\Naufal\AI\ChatKeep-Naufal`.
3. Tunggu proses **Gradle Sync** selesai.
4. Klik tombol **Run 'app'** (Shift+F10) ke HP fisik atau emulator.
5. Saat pertama kali membuka aplikasi:
   - Izinkan Notifikasi & Akses Media.
   - Tekan **Aktifkan** pada banner Akses Notifikasi, setujui dialog transparansi privasi, dan centang toggle ChatKeep.
   - Buka tab **Status**, klik **Hubungkan Folder WhatsApp** jika memakai Android 11+ untuk memilih folder status.
