package id.my.naufalputra.chatkeep.data

import android.net.Uri

data class StatusModel(
    val uri: Uri,
    val name: String,
    val path: String,
    val isVideo: Boolean,
    val size: Long,
    val timestamp: Long
)
