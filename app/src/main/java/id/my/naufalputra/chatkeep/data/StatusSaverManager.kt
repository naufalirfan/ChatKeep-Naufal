package id.my.naufalputra.chatkeep.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object StatusSaverManager {

    /**
     * Mengambil daftar status WhatsApp dari folder .Statuses via SAF atau path lokal
     */
    fun getWhatsAppStatuses(context: Context, treeUriString: String?): List<StatusModel> {
        val list = mutableListOf<StatusModel>()

        // 1. Coba baca via Storage Access Framework jika URI tersimpan
        if (!treeUriString.isNullOrEmpty()) {
            try {
                val treeUri = Uri.parse(treeUriString)
                val documentFile = DocumentFile.fromTreeUri(context, treeUri)
                if (documentFile != null && documentFile.isDirectory) {
                    for (file in documentFile.listFiles()) {
                        val name = file.name ?: continue
                        val isImage = name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".jpeg", ignoreCase = true)
                        val isVideo = name.endsWith(".mp4", ignoreCase = true)
                        if (isImage || isVideo) {
                            list.add(
                                StatusModel(
                                    uri = file.uri,
                                    name = name,
                                    path = file.uri.toString(),
                                    isVideo = isVideo,
                                    size = file.length(),
                                    timestamp = file.lastModified()
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Coba baca dari folder path standar jika SAF belum tersedia / Android lama
        if (list.isEmpty()) {
            val possiblePaths = listOf(
                File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
                File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/.Statuses"),
                File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses")
            )

            for (dir in possiblePaths) {
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles() ?: continue
                    for (file in files) {
                        val name = file.name
                        val isImage = name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".jpeg", ignoreCase = true)
                        val isVideo = name.endsWith(".mp4", ignoreCase = true)
                        if (isImage || isVideo) {
                            list.add(
                                StatusModel(
                                    uri = Uri.fromFile(file),
                                    name = name,
                                    path = file.absolutePath,
                                    isVideo = isVideo,
                                    size = file.length(),
                                    timestamp = file.lastModified()
                                )
                            )
                        }
                    }
                }
            }
        }

        return list.sortedByDescending { it.timestamp }
    }

    /**
     * Menyimpan Status (Foto / Video) ke Galeri HP (Pictures/ChatKeep atau Movies/ChatKeep)
     */
    fun saveStatusToGallery(context: Context, status: StatusModel): Boolean {
        return try {
            val resolver = context.contentResolver
            val inputStream: InputStream? = if (status.path.startsWith("content://")) {
                resolver.openInputStream(status.uri)
            } else {
                FileInputStream(File(status.path))
            }

            inputStream ?: return false

            val fileName = "ChatKeep_${System.currentTimeMillis()}_${status.name}"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, if (status.isVideo) "video/mp4" else "image/jpeg")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        if (status.isVideo) "${Environment.DIRECTORY_MOVIES}/ChatKeep" else "${Environment.DIRECTORY_PICTURES}/ChatKeep"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collection = if (status.isVideo) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }

                val itemUri = resolver.insert(collection, values) ?: return false
                resolver.openOutputStream(itemUri)?.use { out ->
                    inputStream.copyTo(out)
                }

                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            } else {
                val targetDir = File(
                    Environment.getExternalStoragePublicDirectory(
                        if (status.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                    ),
                    "ChatKeep"
                )
                if (!targetDir.exists()) targetDir.mkdirs()
                val targetFile = File(targetDir, fileName)
                FileOutputStream(targetFile).use { out ->
                    inputStream.copyTo(out)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
