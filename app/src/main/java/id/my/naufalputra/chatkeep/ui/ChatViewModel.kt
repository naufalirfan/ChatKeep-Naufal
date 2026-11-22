package id.my.naufalputra.chatkeep.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.my.naufalputra.chatkeep.data.ChatDatabase
import id.my.naufalputra.chatkeep.data.ChatMessageEntity
import id.my.naufalputra.chatkeep.data.StatusModel
import id.my.naufalputra.chatkeep.data.StatusSaverManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val chatDao = ChatDatabase.getInstance(context).chatDao()
    private val prefs = context.getSharedPreferences("chatkeep_prefs", Context.MODE_PRIVATE)

    // Seluruh pesan untuk Dashboard
    val allMessages: StateFlow<List<ChatMessageEntity>> = chatDao.getAllMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Seluruh media yang berhasil diselamatkan (Foto, Video, Suara)
    val allMediaMessages: StateFlow<List<ChatMessageEntity>> = chatDao.getAllMediaMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Daftar status WhatsApp yang terdeteksi
    private val _statuses = MutableStateFlow<List<StatusModel>>(emptyList())
    val statuses: StateFlow<List<StatusModel>> = _statuses.asStateFlow()

    private val _isLoadingStatuses = MutableStateFlow(false)
    val isLoadingStatuses: StateFlow<Boolean> = _isLoadingStatuses.asStateFlow()

    init {
        loadStatuses()
    }

    fun getThreadMessages(senderName: String, packageName: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesByThread(senderName, packageName)
    }

    fun deleteThread(senderName: String, packageName: String) {
        viewModelScope.launch {
            chatDao.deleteThread(senderName, packageName)
        }
    }

    // === STATUS SAVER LOGIC ===
    fun loadStatuses() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingStatuses.value = true
            val savedUri = prefs.getString("wa_status_tree_uri", null)
            val list = StatusSaverManager.getWhatsAppStatuses(context, savedUri)
            _statuses.value = list
            _isLoadingStatuses.value = false
        }
    }

    fun saveStatus(status: StatusModel, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = StatusSaverManager.saveStatusToGallery(context, status)
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    fun saveStatusTreeUri(uriString: String) {
        prefs.edit().putString("wa_status_tree_uri", uriString).apply()
        loadStatuses()
    }

    fun getStatusTreeUri(): String? {
        return prefs.getString("wa_status_tree_uri", null)
    }
}
