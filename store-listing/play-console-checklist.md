# Checklist Peluncuran ChatKeep di Google Play Console

Ikuti langkah-langkah praktis ini untuk mempublikasikan ChatKeep ke Play Store:

### Langkah 1: Buat Aplikasi Baru di Play Console
1. Buka [Google Play Console](https://play.google.com/console).
2. Klik **Create app**.
3. **App name:** `ChatKeep — Naufal`
4. **Default language:** Indonesian (id) atau English (United States)
5. **App or game:** App
6. **Free or paid:** Free
7. Centang Deklarasi Kebijakan Pengembang & Undang-Undang Ekspor AS, lalu klik **Create app**.

### Langkah 2: Lengkapi Main Store Listing
* **App title:** `ChatKeep — Naufal` (dari `store-listing/app-title.txt`)
* **Short description:** (dari `store-listing/short-description.txt`)
* **Full description:** (dari `store-listing/full-description.txt`)
* **App icon:** Upload `store-listing/graphics/icon-512.png` (512x512)
* **Feature graphic:** Upload `store-listing/graphics/feature-graphic-1024x500.png` (1024x500)
* **Phone screenshots:** Upload minimal 4 screenshot dari folder `store-listing/graphics/` (screenshot 1 s/d 4)

### Langkah 3: Kebijakan Privasi (Privacy Policy)
* Masukkan URL Kebijakan Privasi publik:  
  `https://naufalirfan.github.io/ChatKeep-Naufal/`

### Langkah 4: Kuesioner Konten & Kebijakan Aplikasi
* **Target Audience:** Pilih usia **18 and above** (atau 16+).
* **Ads:** Pilih **No, my app does not contain ads**.
* **Financial Features:** Pilih **My app doesn't provide any financial features**.
* **Government Apps:** Pilih **No**.
* **Content Rating:** Isi kuesioner utilitas (semua pertanyaan kekerasan/konten seksual dijawab No). Rating yang keluar adalah **Everyone 3+ / 12+**.
* **Data Safety:** Isi sesuai panduan di `data-safety-answers.md` (Pilih **No Data Collected**).

### Langkah 5: Unggah Berkas Produksi (Signed AAB)
* Unduh berkas **`app-release.aab`** dari GitHub Actions `Build Android AAB (Release)`.
* Masuk ke menu **Release > Production** (atau Closed Testing).
* Buat rilis baru (**Create new release**), seret file `app-release.aab`.
* Masukkan Release notes:  
  `Rilis perdana ChatKeep v1.1: Pemulihan chat dan media terhapus serta WhatsApp Status Saver lokal.`
* Klik **Review release** lalu klik **Start rollout to Production**!
