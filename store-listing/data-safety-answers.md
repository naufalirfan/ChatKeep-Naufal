# Panduan Pengisian Formulir Data Safety Google Play Console

Saat Anda mengisi formulir **App Content > Data safety** di Google Play Console, pilih jawaban berikut:

### 1. Data Collection and Security
* **Does your app collect or share any of the required user data types?**  
  👉 **Pilih: NO**  
  *(Alasan: Aplikasi memproses dan menyimpan data 100% secara lokal di dalam SQLite internal perangkat pengguna, tidak pernah diunggah ke internet/server).*

### 2. Is all of the user data collected by your app encrypted in transit?
* Terjawab otomatis **No / Not Applicable** karena di pertanyaan pertama dijawab No.

### 3. Do you provide a way for users to request that their data be deleted?
* 👉 **Pilih: YES**  
  *(Pengguna memiliki tombol Hapus Thread di aplikasi, fitur Auto-Clean berkala, dan uninstall aplikasi akan menghapus seluruh data secara permanen).*
* Masukkan URL Permintaan Hapus: `https://naufalirfan.github.io/ChatKeep-Naufal/`

---

# Deklarasi Izin Sensitif (Sensitive Permissions)

### A. Notification Listener Service (`BIND_NOTIFICATION_LISTENER_SERVICE`)
* **Tujuan Penggunaan:** Mencadangkan teks pesan masuk secara lokal untuk pemulihan pesan yang dihapus pengirim.
* **Tautan Penjelasan:** Tampilkan dialog persetujuan (Prominent Disclosure) di aplikasi sebelum membuka pengaturan.

### B. Layanan Latar Depan (`FOREGROUND_SERVICE_SPECIAL_USE`)
* Saat Play Console meminta deskripsi penggunaan Foreground Service, masukkan teks berikut:
  ```text
  Local offline archiving of incoming messaging notifications and media backup for user's personal recovery of retracted messages.
  ```
