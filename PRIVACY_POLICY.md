# Privacy Policy & Google Play Data Safety Guide

## Privacy Policy for ChatKeep — Naufal
**Effective Date:** November 22, 2026

### 1. Overview & Local-First Architecture
ChatKeep — Naufal ("the App") is an offline utility application designed to help users archive incoming messaging notifications locally on their Android device. We respect your privacy: the App does not collect, transmit, store, or share any personal data or message logs on external servers.

### 2. Permissions We Use and Why
- **Notification Listener Service (`BIND_NOTIFICATION_LISTENER_SERVICE`):** Required solely to read incoming text notifications from selected messaging applications (such as WhatsApp, WhatsApp Business, and Instagram) in real time and save them into a local SQLite database inside your device's private app storage.
- **Foreground Service & Notifications (`FOREGROUND_SERVICE_SPECIAL_USE` & `POST_NOTIFICATIONS`):** Used strictly to keep the local notification archiving process running reliably in the background without being killed by OS battery optimization.

### 3. Data Storage & Retention
All captured notification texts, sender names, and timestamps remain 100% on your device. You have full control to delete individual threads, clear all logs at any time, or rely on the built-in automatic cleanup schedule (7, 14, 30 days, or never). Uninstalling the App permanently erases all stored records.

### 4. Third-Party Services
The App contains no third-party analytics SDKs, advertising trackers, or cloud backup integrations that access your notification content.

### 5. Contact
If you have any questions regarding this Privacy Policy, please contact the developer at: `admin@naufalputra.my.id`.

---

## Kebijakan Privasi ChatKeep — Naufal (Bahasa Indonesia)
**Tanggal Berlaku:** 22 November 2026

### 1. Gambaran Umum & Arsitektur Lokal (Offline-First)
ChatKeep — Naufal ("Aplikasi") adalah aplikasi utilitas offline yang dirancang untuk membantu pengguna mengarsipkan riwayat notifikasi pesan masuk secara lokal di perangkat Android. Kami sangat menghargai privasi Anda: Aplikasi ini tidak mengumpulkan, mengirimkan, menyimpan, maupun membagikan data pribadi atau teks percakapan Anda ke server eksternal mana pun.

### 2. Izin yang Digunakan dan Tujuannya
- **Akses Notifikasi (`BIND_NOTIFICATION_LISTENER_SERVICE`):** Diperlukan semata-mata untuk membaca teks notifikasi pesan masuk dari aplikasi perpesanan yang dipilih (seperti WhatsApp dan Instagram) secara real-time dan menyimpannya ke database SQLite lokal di penyimpanan internal perangkat Anda.
- **Layanan Latar Depan (`FOREGROUND_SERVICE` & `POST_NOTIFICATIONS`):** Digunakan untuk menjaga proses pencadangan notifikasi lokal tetap berjalan stabil di latar belakang tanpa dimatikan secara sepihak oleh manajemen penghemat daya Android.

### 3. Penyimpanan & Penghapusan Data
Semua data riwayat pesan, nama pengirim, dan waktu pesan tersimpan 100% di perangkat Anda. Pengguna memiliki kendali penuh untuk menghapus percakapan tertentu kapan saja atau mengatur pembersihan berkala otomatis (7, 14, 30 hari). Menghapus (uninstall) aplikasi akan langsung menghapus seluruh catatan riwayat pesan tanpa jejak.

### 4. Layanan Pihak Ketiga
Aplikasi ini tidak menyertakan SDK analitik pihak ketiga, pelacak iklan, atau layanan cloud pihak ketiga yang memiliki akses ke konten notifikasi Anda.

---

## Panduan Pengisian Formulir Data Safety di Google Play Console
1. **Does your app collect or share any of the required user data types?**  
   -> Pilih: **No** (Seluruh data diproses dan disimpan secara lokal di perangkat pengguna, tidak pernah dikirim ke internet).
2. **Is all of the user data collected by your app encrypted in transit?**  
   -> Tidak akan ditanyakan jika poin 1 dijawab No.
3. **Deklarasi Izin (App Content > Sensitive Permissions):**  
   - Saat mengisi deklarasi `FOREGROUND_SERVICE_SPECIAL_USE`, isi keterangan:  
     `Local offline archiving of incoming messaging notifications for user backup.`  
   - Sertakan video singkat demo layar (screen recording) saat aktivasi izin, penerimaan pesan, dan pengecekan pesan terhapus di aplikasi.
