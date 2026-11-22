package id.my.naufalputra.chatkeep.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import id.my.naufalputra.chatkeep.R
import id.my.naufalputra.chatkeep.data.ChatDao
import id.my.naufalputra.chatkeep.data.ChatDatabase
import id.my.naufalputra.chatkeep.data.ChatMessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ChatKeepNotificationService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var chatDao: ChatDao

    // Daftar paket aplikasi perpesanan yang didukung secara luas
    private val supportedPackages = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.instagram.android",
        "org.telegram.messenger",
        "org.thunderdog.challegram",
        "com.facebook.orca",
        "jp.naver.line.android",
        "com.twitter.android"
    )

    private val deletedPhrases = listOf(
        "pesan ini telah dihapus",
        "this message was deleted",
        "anda telah menghapus pesan ini",
        "you deleted this message",
        "unsent a message",
        "membatalkan pengiriman pesan",
        "pesan telah ditarik",
        "message was retracted",
        "ditarik oleh pengirim"
    )

    override fun onCreate() {
        super.onCreate()
        chatDao = ChatDatabase.getInstance(applicationContext).chatDao()
        startKeepAliveForeground()
    }

    private fun startKeepAliveForeground() {
        val channelId = "chatkeep_monitor_channel"
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Layanan Pemantau ChatKeep",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Menjaga pencadangan notifikasi lokal dan media tetap aktif di latar belakang"
                setShowBadge(false)
            }
            manager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ChatKeep — Naufal Aktif")
            .setContentText("Mencadangkan chat & media secara lokal di perangkat")
            .setSmallIcon(R.drawable.ic_notif)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1001, notification)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName ?: return

        // 1. Filter aplikasi target (mengizinkan paket yang didukung atau jika opsi 'Pantau Semua' aktif)
        val prefs = getSharedPreferences("chatkeep_prefs", Context.MODE_PRIVATE)
        val monitorAll = prefs.getBoolean("monitor_all_apps", false)
        if (!monitorAll && pkg !in supportedPackages) return

        val notification = sbn.notification ?: return

        // 2. Abaikan notifikasi ringkasan grup ("3 pesan baru")
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val extras = notification.extras ?: return
        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: return

        // Prioritaskan EXTRA_BIG_TEXT untuk pesan panjang
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
        val standardText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        val rawText = if (!bigText.isNullOrEmpty()) bigText else standardText ?: return

        if (rawTitle.isEmpty() || rawText.isEmpty()) return

        // 3. Ekstraksi pengirim dan isi pesan
        val cleanSender = rawTitle.substringBefore(" (").substringBefore(":").trim()
        val displayMessage = if (rawTitle.contains(":")) {
            "${rawTitle.substringAfter(":").trim()}: $rawText"
        } else {
            rawText
        }

        val now = System.currentTimeMillis()

        // 4. Deteksi dan amankan lampiran Media (Foto / Voice / Video) dari notifikasi
        var mediaUri: String? = null
        var mediaType: String? = null

        // Cek apakah ada Bitmap terlampir di notifikasi (misal kiriman foto WA / Telegram)
        val pictureBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_PICTURE, Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(Notification.EXTRA_PICTURE) as? Bitmap
        }

        if (pictureBitmap != null) {
            val savedFile = saveBitmapLocally(pictureBitmap, cleanSender, now)
            if (savedFile != null) {
                mediaUri = savedFile.absolutePath
                mediaType = "IMAGE"
            }
        }

        // Deteksi tipe media dari teks jika tidak ada bitmap langsung
        if (mediaType == null) {
            val lower = displayMessage.lowercase()
            when {
                lower.contains("📷") || lower.contains("foto") || lower.contains("photo") -> mediaType = "IMAGE"
                lower.contains("📹") || lower.contains("video") -> mediaType = "VIDEO"
                lower.contains("🎤") || lower.contains("pesan suara") || lower.contains("voice message") || lower.contains("audio") -> mediaType = "VOICE"
            }
        }

        serviceScope.launch {
            // 5. Cek apakah notifikasi ini merupakan penarikan / penghapusan pesan
            val isDeleteNotice = deletedPhrases.any { phrase ->
                rawText.lowercase().contains(phrase)
            }

            if (isDeleteNotice) {
                chatDao.markLastMessageAsDeleted(sender = cleanSender, pkg = pkg)
            } else {
                // Abaikan status sistem
                if (rawText.contains("Memeriksa pesan baru", ignoreCase = true) ||
                    rawText.contains("Checking for new messages", ignoreCase = true)
                ) {
                    return@launch
                }

                val entity = ChatMessageEntity(
                    packageName = pkg,
                    senderName = cleanSender,
                    messageText = displayMessage,
                    timestamp = now,
                    timestampBucket = now / 2000L,
                    isDeleted = false,
                    mediaUri = mediaUri,
                    mediaType = mediaType,
                    mediaSize = if (mediaUri != null) File(mediaUri).length() else 0L
                )
                chatDao.insertMessage(entity)
            }
        }
    }

    private fun saveBitmapLocally(bitmap: Bitmap, sender: String, timestamp: Long): File? {
        return try {
            val dir = File(filesDir, "recovered_media")
            if (!dir.exists()) dir.mkdirs()
            val safeSender = sender.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val file = File(dir, "IMG_${safeSender}_${timestamp}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
