# Product Requirements Document (PRD)

**Nama Produk:** ChatKeep — Naufal  
**Platform:** Android (Minimum SDK 26 / Android 8.0 Oreo, Target SDK 35 / Android 15)  
**Tipe Aplikasi:** Utilitas / Produktivitas (Offline-First Notification & Deleted Message Logger)  
**Status Dokumen:** Draft v1.0  

---

## 1. Ringkasan Eksekutif & Tujuan Produk
ChatKeep — Naufal adalah aplikasi utilitas Android berkonsep *offline-first* yang berfungsi menangkap, mengarsipkan, dan menampilkan kembali riwayat notifikasi pesan masuk dari aplikasi perpesanan (fokus utama: WhatsApp, WhatsApp Business, dan Instagram) secara real-time.

Tujuan utamanya adalah memungkinkan pengguna tetap dapat membaca isi pesan yang telah ditarik atau dihapus oleh pengirim ("Pesan ini telah dihapus"), serta membaca pesan tanpa memicu tanda baca (centang biru / seen), dengan jaminan privasi 100% lokal di perangkat tanpa mengirim data ke server eksternal.

---

## 2. Target Pengguna & Use Cases
- **Pengguna Harian WA & IG:** Ingin mengetahui isi pesan yang ditarik oleh pengirim karena rasa penasaran atau kebutuhan bukti percakapan.
- **Pengguna Profesional / Admin Bisnis:** Membutuhkan cadangan log notifikasi orderan atau komplain pelanggan yang terlanjur dihapus oleh pengirim.
- **Pengguna Sadar Privasi:** Menginginkan alternatif aplikasi sejenis WAMR yang ringan, bebas iklan mengganggu, dan tidak mengumpulkan data chat ke server pihak ketiga.

---

## 3. Ruang Lingkup Fitur (Scope)

### A. Fitur Utama (MVP — V1.0)
| Kode | Fitur | Deskripsi Fungsional | Prioritas |
|---|---|---|---|
| **F-01** | Onboarding & Izin Akses | Panduan langkah demi langkah bagi pengguna untuk mengaktifkan Notification Access (`BIND_NOTIFICATION_LISTENER_SERVICE`) dan pengecualian Battery Optimization agar service tidak dimatikan sistem. | P0 (Wajib) |
| **F-02** | Real-time Notification Listener | Menangkap StatusBarNotification secara otomatis di latar belakang untuk paket target: `com.whatsapp`, `com.whatsapp.w4b`, dan `com.instagram.android`. | P0 (Wajib) |
| **F-03** | Pengelompokan Chat (Thread View) | Mengelompokkan pesan berdasarkan nama pengirim/grup dan sumber aplikasi (tab/filter WA, WA Business, IG), menyerupai antarmuka ruang obrolan. | P0 (Wajib) |
| **F-04** | Deteksi & Penanda Pesan Dihapus | Mendeteksi notifikasi sistem penghapusan pesan (misal: *"Pesan ini telah dihapus"* / *"This message was deleted"*) dan memberi badge khusus (**"Dihapus"**) pada log pesan terakhir di thread terkait. | P0 (Wajib) |
| **F-05** | Pencarian & Filter Riwayat | Kolom pencarian kata kunci pesan, pencarian nama kontak, serta filter berdasarkan aplikasi atau status pesan (Semua / Hanya yang Dihapus). | P1 (Tinggi) |
| **F-06** | Manajemen Penyimpanan & Auto-Clean | Opsi hapus per chat, hapus semua riwayat, serta pengaturan otomatis menghapus log yang lebih tua dari X hari (7, 14, 30 hari, atau selamanya) agar memori tetap ringan. | P1 (Tinggi) |
| **F-07** | Keamanan Kunci Aplikasi | Proteksi opsional menggunakan Biometrik (Sidik Jari / Face Unlock) atau PIN saat membuka aplikasi agar arsip pesan tidak dibaca orang lain. | P2 (Sedang) |

### B. Fitur Pengembangan Lanjutan (V1.1+)
- **Penangkapan Media (Gambar/Voice Note):** Mendeteksi file media yang masuk ke folder cache/media WhatsApp sebelum dihapus (memerlukan izin akses penyimpanan sesuai aturan Android terbaru).
- **Ekspor Riwayat Chat:** Mengekspor log percakapan tertentu ke format `.txt` atau `.json` secara lokal.
- **Dukungan Aplikasi Tambahan:** Opsi kustomisasi untuk menangkap notifikasi dari Telegram, Messenger, atau aplikasi lainnya.

---

## 4. Spesifikasi Teknis & Arsitektur
1. **Tech Stack:**
   - **Bahasa:** Kotlin
   - **UI Framework:** Jetpack Compose (Material Design 3) dengan dukungan penuh Dark Mode & Light Mode.
   - **Arsitektur:** MVVM (Model-View-ViewModel) + Clean Architecture.
   - **Database Lokal:** Room Database (SQLite) untuk penyimpanan terstruktur (`AppSource`, `SenderName`, `MessageContent`, `Timestamp`, `IsDeletedFlag`, `NotificationKey`).
   - **Background Execution:** `NotificationListenerService` + `ForegroundService` (notifikasi persisten berprioritas rendah untuk mencegah kill oleh ROM agresif seperti Xiaomi/MIUI, Realme/ColorOS, Vivo/FuntouchOS).
2. **Aturan Deduplikasi Data:**
   - Sistem Android sering memicu pembaruan notifikasi bertumpuk (*summary notification* seperti "3 pesan baru"). Aplikasi wajib melakukan filter `Notification.FLAG_GROUP_SUMMARY` dan pengecekan bucket timestamp 2 detik agar pesan yang sama tidak tercatat ganda.

---

## 5. Kepatuhan Kebijakan Google Play & Privasi
- **Deklarasi Privasi Data (Data Safety):** Menyatakan secara eksplisit bahwa aplikasi tidak mengumpulkan, mengirim, atau membagikan data pengguna ke server mana pun (100% penyimpanan lokal).
- **Transparansi Izin (Prominent Disclosure):** Menampilkan dialog penjelasan yang jelas sebelum mengarahkan pengguna ke halaman izin Notification Listener, menjelaskan bahwa izin hanya dipakai untuk mencatat riwayat pesan secara lokal.
- **Kepatuhan Akses Penyimpanan:** Menghindari penggunaan `MANAGE_EXTERNAL_STORAGE` pada V1.0 (fokus pada teks notifikasi terlebih dahulu) agar proses review Play Store berjalan cepat dan aman dari penolakan kebijakan.

---

## 6. Alur Pengguna (User Flow)
1. **Buka Aplikasi Pertama Kali:** Layar Welcome -> Penjelasan cara kerja & privasi -> Tombol "Aktifkan Akses Notifikasi" (dengan Prominent Disclosure Dialog).
2. **Pengaturan Sistem:** Pengguna mengaktifkan toggle untuk ChatKeep — Naufal -> Kembali ke aplikasi -> Pop-up rekomendasi menonaktifkan penghemat baterai untuk aplikasi.
3. **Layar Utama (Dashboard):**
   - Header: Logo ChatKeep — Naufal, ikon pencarian, dan ikon pengaturan.
   - Filter Chips: Semua | WhatsApp | Instagram | Pesan Dihapus.
   - Daftar Chat: Menampilkan avatar inisial, nama pengirim, cuplikan pesan terakhir, waktu, dan indikator jumlah pesan yang ditarik.
4. **Detail Ruang Obrolan:** Menampilkan gelembung chat kronologis; pesan yang terdeteksi ditarik ditandai dengan bingkai merah halus dan label "Dihapus oleh pengirim".

---

## 7. Metrik Keberhasilan (KPIs)
- **Reliability Rate:** >98% notifikasi teks yang muncul di status bar berhasil tercatat ke Room Database tanpa duplikasi.
- **Background Survival Rate:** Service tetap aktif >24 jam di latar belakang setelah perangkat dalam mode *idle/doze*.
- **App Size & Performance:** Ukuran APK/AAB rilis di bawah 10 MB dengan konsumsi RAM latar belakang di bawah 40 MB.
