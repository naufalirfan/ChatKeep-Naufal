package id.my.naufalputra.chatkeep.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.my.naufalputra.chatkeep.data.ChatDatabase
import id.my.naufalputra.chatkeep.data.ChatMessageEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val chatDao = ChatDatabase.getInstance(application).chatDao()

    // Seluruh pesan untuk ditampilkan di Dashboard (dikelompokkan per thread di UI)
    val allMessages: StateFlow<List<ChatMessageEntity>> = chatDao.getAllMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Mengambil pesan spesifik untuk satu kontak/grup di layar Detail
    fun getThreadMessages(senderName: String, packageName: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesByThread(senderName, packageName)
    }

    // Menghapus seluruh riwayat chat dari satu pengirim tertentu
    fun deleteThread(senderName: String, packageName: String) {
        viewModelScope.launch {
            chatDao.deleteThread(senderName, packageName)
        }
    }
}
