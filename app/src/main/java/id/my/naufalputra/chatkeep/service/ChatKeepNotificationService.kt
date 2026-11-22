package id.my.naufalputra.chatkeep.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import id.my.naufalputra.chatkeep.data.ChatDao
import id.my.naufalputra.chatkeep.data.ChatDatabase
import id.my.naufalputra.chatkeep.data.ChatMessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ChatKeepNotificationService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var chatDao: ChatDao

    private val targetPackages = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.instagram.android"
    )

    private val deletedPhrases = listOf(
        "pesan ini telah dihapus",
        "this message was deleted",
        "anda telah menghapus pesan ini",
        "you deleted this message",
        "unsent a message",
        "membatalkan pengiriman pesan"
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
                description = "Menjaga pencadangan notifikasi lokal tetap aktif di latar belakang"
                setShowBadge(false)
            }
            manager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ChatKeep — Naufal Aktif")
            .setContentText("Mencadangkan notifikasi secara lokal di perangkat")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
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

        // 1. Filter hanya aplikasi target
        if (pkg !in targetPackages) return
        val notification = sbn.notification ?: return

        // 2. Abaikan notifikasi grup ringkasan (contoh: "3 pesan dari 2 chat")
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val extras = notification.extras ?: return
        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: return

        // Prioritaskan EXTRA_BIG_TEXT untuk menangkap pesan panjang secara utuh
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
        val standardText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        val rawText = if (!bigText.isNullOrEmpty()) bigText else standardText ?: return

        if (rawTitle.isEmpty() || rawText.isEmpty()) return

        // 3. Bersihkan format nama grup WA (contoh: "Grup Kampus (3 pesan): Budi" -> "Grup Kampus")
        val cleanSender = rawTitle.substringBefore(" (").substringBefore(":").trim()
        val displayMessage = if (rawTitle.contains(":")) {
            "${rawTitle.substringAfter(":").trim()}: $rawText"
        } else {
            rawText
        }

        val now = System.currentTimeMillis()

        serviceScope.launch {
            // 4. Cek apakah notifikasi ini merupakan tanda penghapusan pesan
            val isDeleteNotice = deletedPhrases.any { phrase ->
                rawText.lowercase().contains(phrase)
            }

            if (isDeleteNotice) {
                chatDao.markLastMessageAsDeleted(sender = cleanSender, pkg = pkg)
            } else {
                // Abaikan status sistem seperti "Memeriksa pesan baru..."
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
                    timestampBucket = now / 2000L, // Deduplikasi dalam rentang 2 detik
                    isDeleted = false
                )
                chatDao.insertMessage(entity)
            }
        }
    }
}
