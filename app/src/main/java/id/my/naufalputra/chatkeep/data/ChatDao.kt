package id.my.naufalputra.chatkeep.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE senderName = :sender AND packageName = :pkg ORDER BY timestamp ASC")
    fun getMessagesByThread(sender: String, pkg: String): Flow<List<ChatMessageEntity>>

    // Mengambil semua pesan yang memiliki lampiran media (foto, video, suara)
    @Query("SELECT * FROM chat_messages WHERE mediaUri IS NOT NULL ORDER BY timestamp DESC")
    fun getAllMediaMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE mediaType = :type ORDER BY timestamp DESC")
    fun getMediaByType(type: String): Flow<List<ChatMessageEntity>>

    // Menandai pesan terakhir dari pengirim sebagai "Dihapus"
    @Query("""
        UPDATE chat_messages 
        SET isDeleted = 1 
        WHERE id = (
            SELECT id FROM chat_messages 
            WHERE senderName = :sender AND packageName = :pkg AND isDeleted = 0 
            ORDER BY timestamp DESC LIMIT 1
        )
    """)
    suspend fun markLastMessageAsDeleted(sender: String, pkg: String)

    @Query("DELETE FROM chat_messages WHERE senderName = :sender AND packageName = :pkg")
    suspend fun deleteThread(sender: String, pkg: String)

    @Query("DELETE FROM chat_messages WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)
}
