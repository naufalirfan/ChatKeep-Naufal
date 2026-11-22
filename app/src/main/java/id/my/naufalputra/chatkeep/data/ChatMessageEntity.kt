package id.my.naufalputra.chatkeep.data

import androidx.room.*

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["packageName", "senderName", "messageText", "timestampBucket"], unique = true)]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,       // com.whatsapp, com.whatsapp.w4b, com.instagram.android, org.telegram.messenger, etc.
    val senderName: String,        // Nama pengirim atau nama grup
    val messageText: String,       // Isi pesan yang tertangkap
    val timestamp: Long,           // Waktu pesan masuk (ms)
    val timestampBucket: Long,     // timestamp / 2000 (untuk mencegah duplikasi notifikasi dalam 2 detik)
    val isDeleted: Boolean = false, // True jika terdeteksi ditarik/dihapus
    val mediaUri: String? = null,   // Path file media lokal (foto, video, suara)
    val mediaType: String? = null,  // "IMAGE", "VIDEO", "AUDIO", "VOICE"
    val mediaSize: Long = 0L
)
