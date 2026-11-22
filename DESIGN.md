# DESIGN.md — ChatKeep — Naufal

## 1. Filosofi Desain & Aturan Anti-Slop
ChatKeep adalah aplikasi utilitas privasi (*offline-first*). Desain antarmuka menghindari estetika *template* generik:
* **Utilitas Tinggi, Nol Dekorasi Kosong:** Setiap elemen di layar harus berfungsi untuk keterbacaan pesan, pemindaian cepat, dan kejelasan status.
* **Kontras Tajam & Tegas:** Menggunakan permukaan datar (*flat surfaces*) dengan garis batas tipis (`1.dp` hairline border) tanpa bayangan mengambang (elevation 0.dp).
* **Semantik Status Jelas:** Warna merah hanya digunakan untuk indikator pesan yang dihapus/ditarik dan aksi destruktif (hapus riwayat).

---

## 2. Palet Warna (Color Tokens)

### Light Mode (Clean Slate & Deep Teal)
* **Primary (`#0F766E`):** Deep Teal — tombol utama, indikator aktif, aksen merek.
* **OnPrimary (`#FFFFFF`):** Putih bersih untuk teks di atas tombol utama.
* **Background (`#F8FAFC`):** Slate 50 — latar belakang utama agar nyaman dibaca lama.
* **Surface (`#FFFFFF`):** Putih murni untuk kartu dan baris daftar chat.
* **SurfaceVariant (`#F1F5F9`):** Slate 100 — gelembung pesan masuk normal & kolom pencarian.
* **OnSurface (`#0F172A`):** Slate 900 — teks utama dengan kontras tinggi.
* **OnSurfaceVariant (`#475569`):** Slate 600 — pratinjau pesan & stempel waktu.
* **OutlineVariant (`#E2E8F0`):** Slate 200 — garis pemisah antar-item (`0.5.dp` - `1.dp`).
* **Error / Deleted (`#DC2626`):** Crimson Red — penanda pesan yang dihapus pengirim.
* **ErrorContainer (`#FEF2F2`):** Soft Rose — latar belakang gelembung pesan yang terdeteksi ditarik.

### Dark Mode (True Obsidian & Mint Teal)
* **Primary (`#2DD4BF`):** Mint Teal — aksen utama di layar gelap.
* **OnPrimary (`#042F2E`):** Teal sangat gelap untuk kontras teks tombol.
* **Background (`#090D16`):** Deep Obsidian — hemat daya layar AMOLED dan tidak menyilaukan.
* **Surface (`#111827`):** Dark Slate — kontainer kartu dan baris daftar.
* **SurfaceVariant (`#1E293B`):** Slate 800 — gelembung pesan normal & kolom input.
* **OnSurface (`#F8FAFC`):** Putih lembut untuk teks utama.
* **OnSurfaceVariant (`#94A3B8`):** Slate 400 — metadata waktu & teks sekunder.
* **OutlineVariant (`#1E293B`):** Garis pemisah tipis.
* **Error / Deleted (`#F87171`):** Coral Red — penanda pesan dihapus di mode gelap.
* **ErrorContainer (`#450A0A`):** Deep Maroon — latar gelembung pesan dihapus.

---

## 3. Tipografi & Skala Teks
Menggunakan keluarga font sistem sans-serif bersih (`FontFamily.SansSerif`) dengan penekanan pada keterbacaan angka:
* **App Title:** `20.sp`, `FontWeight.Bold`, letter-spacing `-0.3.sp`.
* **Sender Name (List & Header):** `15.sp`, `FontWeight.SemiBold`, maksimal 1 baris (`TextOverflow.Ellipsis`).
* **Message Body (Detail Bubble):** `14.5.sp`, `FontWeight.Normal`, line-height `20.sp`.
* **Message Preview (List):** `13.5.sp`, `FontWeight.Normal`, maksimal 1 baris.
* **Timestamp & Badges:** `11.sp`, `FontWeight.Medium`, `fontFeatureSettings = "tnum"` (angka tabular sejajar rapi).

---

## 4. Layout, Spacing & Shape System
* **Grid Dasar:** Kelipatan `4.dp` (`4.dp`, `8.dp`, `12.dp`, `16.dp`, `24.dp`).
* **Padding Layar Horizontal:** Konsisten `16.dp` di seluruh layar.
* **Radius Sudut (Corner Radius):**
  * Kartu & Kolom Pencarian: `12.dp` (tegas dan modern, bukan kapsul penuh).
  * Gelembung Chat: `12.dp` dengan sudut kiri-atas tajam (`topStart = 2.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 12.dp`) untuk kesan arah pesan masuk yang jelas.
  * Badge Aplikasi (`WA` / `IG`): `4.dp`.
* **Elevasi:** `0.dp` di seluruh komponen. Pemisahan visual menggunakan perbedaan warna permukaan (`Surface` vs `Background`) dan outline hairline (`0.5.dp`).
