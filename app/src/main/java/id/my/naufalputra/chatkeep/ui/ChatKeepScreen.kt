package id.my.naufalputra.chatkeep.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.WorkManager
import coil.compose.AsyncImage
import id.my.naufalputra.chatkeep.data.ChatMessageEntity
import id.my.naufalputra.chatkeep.data.StatusModel
import id.my.naufalputra.chatkeep.service.ChatKeepNotificationService
import id.my.naufalputra.chatkeep.worker.AutoCleanWorker
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class NavigationTab {
    CHATS, MEDIA, STATUS, SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabScaffold(
    viewModel: ChatViewModel,
    onThreadClick: (senderName: String, packageName: String) -> Unit
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(NavigationTab.CHATS) }
    val allMessages by viewModel.allMessages.collectAsState()
    val allMedia by viewModel.allMediaMessages.collectAsState()
    val statuses by viewModel.statuses.collectAsState()
    val isLoadingStatuses by viewModel.isLoadingStatuses.collectAsState()

    // SAF Picker untuk izin baca folder .Statuses WhatsApp di Android 11+
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            viewModel.saveStatusTreeUri(uri.toString())
            Toast.makeText(context, "Folder WhatsApp berhasil dihubungkan!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ChatKeep — Naufal", fontWeight = FontWeight.Bold)
                        Text(
                            when (currentTab) {
                                NavigationTab.CHATS -> "Arsip Percakapan & Deteksi Hapus"
                                NavigationTab.MEDIA -> "Galeri Media Terpulihkan"
                                NavigationTab.STATUS -> "Pengunduh Status WhatsApp"
                                NavigationTab.SETTINGS -> "Pengaturan & Preferensi"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (currentTab == NavigationTab.STATUS) {
                        IconButton(onClick = { viewModel.loadStatuses() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang Status")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == NavigationTab.CHATS,
                    onClick = { currentTab = NavigationTab.CHATS },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Pesan") },
                    label = { Text("Pesan") }
                )
                NavigationBarItem(
                    selected = currentTab == NavigationTab.MEDIA,
                    onClick = { currentTab = NavigationTab.MEDIA },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Media") },
                    label = { Text("Media") }
                )
                NavigationBarItem(
                    selected = currentTab == NavigationTab.STATUS,
                    onClick = { currentTab = NavigationTab.STATUS },
                    icon = { Icon(Icons.Default.Download, contentDescription = "Status") },
                    label = { Text("Status") }
                )
                NavigationBarItem(
                    selected = currentTab == NavigationTab.SETTINGS,
                    onClick = { currentTab = NavigationTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
                    label = { Text("Pengaturan") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentTab) {
                NavigationTab.CHATS -> ChatListTab(messages = allMessages, onThreadClick = onThreadClick)
                NavigationTab.MEDIA -> MediaGalleryTab(mediaMessages = allMedia, onThreadClick = onThreadClick)
                NavigationTab.STATUS -> StatusSaverTab(
                    statuses = statuses,
                    isLoading = isLoadingStatuses,
                    hasTreeUri = viewModel.getStatusTreeUri() != null,
                    onPickFolder = { folderPicker.launch(null) },
                    onSaveStatus = { status ->
                        viewModel.saveStatus(status) { ok ->
                            Toast.makeText(
                                context,
                                if (ok) "Tersimpan ke Galeri HP!" else "Gagal menyimpan status",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
                NavigationTab.SETTINGS -> SettingsScreen(
                    onPickFolder = { folderPicker.launch(null) },
                    hasFolderUri = viewModel.getStatusTreeUri() != null
                )
            }
        }
    }
}

// ==========================================
// 1. TAB DAFTAR PESAN & CHAT
// ==========================================
@Composable
fun ChatListTab(
    messages: List<ChatMessageEntity>,
    onThreadClick: (senderName: String, packageName: String) -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("Semua") }
    var searchQuery by remember { mutableStateOf("") }
    var showDisclosureDialog by remember { mutableStateOf(false) }
    val isPermissionGranted = remember { isNotificationServiceEnabled(context) }
    val filters = listOf("Semua", "WhatsApp", "Instagram", "Telegram", "Dihapus")

    if (showDisclosureDialog) {
        AlertDialog(
            onDismissRequest = { showDisclosureDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null) },
            title = { Text("Pengungkapan Akses Notifikasi") },
            text = {
                Text(
                    "ChatKeep — Naufal memerlukan izin Akses Notifikasi untuk membaca pesan masuk dari WhatsApp, Instagram, Telegram, dll.\n\n" +
                    "• Tujuan: Mengarsipkan pesan & mendeteksi pesan yang dihapus pengirim.\n" +
                    "• Privasi: 100% data tersimpan lokal di perangkat Anda dan tidak dikirim ke internet."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showDisclosureDialog = false
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }) {
                    Text("Setuju & Lanjutkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosureDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    val threads = remember(messages, selectedFilter, searchQuery) {
        messages
            .filter { msg ->
                val matchesFilter = when (selectedFilter) {
                    "WhatsApp" -> msg.packageName.startsWith("com.whatsapp")
                    "Instagram" -> msg.packageName == "com.instagram.android"
                    "Telegram" -> msg.packageName.contains("telegram")
                    "Dihapus" -> msg.isDeleted
                    else -> true
                }
                val matchesSearch = searchQuery.isBlank() ||
                        msg.senderName.contains(searchQuery, ignoreCase = true) ||
                        msg.messageText.contains(searchQuery, ignoreCase = true)
                matchesFilter && matchesSearch
            }
            .groupBy { it.senderName to it.packageName }
            .map { (_, list) ->
                val latest = list.maxByOrNull { it.timestamp }!!
                val deletedCount = list.count { it.isDeleted }
                val mediaCount = list.count { it.mediaUri != null }
                Quadruple(latest, deletedCount, mediaCount, list.size)
            }
            .sortedByDescending { it.first.timestamp }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!isPermissionGranted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Akses Notifikasi Belum Aktif", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Aktifkan izin agar ChatKeep dapat mencadangkan pesan masuk.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    Button(onClick = { showDisclosureDialog = true }) {
                        Text("Aktifkan")
                    }
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text("Cari pesan, kontak, atau media...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) }
                )
            }
        }

        if (threads.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Belum ada pesan yang tercatat.\nPesan masuk dari WA, IG, Telegram akan otomatis terekam di sini.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(threads, key = { "${it.first.senderName}_${it.first.packageName}" }) { (latest, deletedCount, mediaCount, _) ->
                    ThreadListItem(
                        latestMessage = latest,
                        deletedCount = deletedCount,
                        mediaCount = mediaCount,
                        onClick = { onThreadClick(latest.senderName, latest.packageName) }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun ThreadListItem(
    latestMessage: ChatMessageEntity,
    deletedCount: Int,
    mediaCount: Int,
    onClick: () -> Unit
) {
    val appBadge = when {
        latestMessage.packageName.startsWith("com.whatsapp") -> "WA"
        latestMessage.packageName == "com.instagram.android" -> "IG"
        latestMessage.packageName.contains("telegram") -> "TG"
        latestMessage.packageName.contains("orca") -> "FB"
        latestMessage.packageName.contains("line") -> "LINE"
        else -> "APP"
    }

    val badgeColor = when (appBadge) {
        "WA" -> Color(0xFF25D366)
        "IG" -> Color(0xFFE1306C)
        "TG" -> Color(0xFF229ED9)
        "FB" -> Color(0xFF0084FF)
        "LINE" -> Color(0xFF06C755)
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = latestMessage.senderName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = latestMessage.senderName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(6.dp))
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = appBadge,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (latestMessage.mediaUri != null) {
                    Icon(
                        Icons.Default.Photo,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = latestMessage.messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (latestMessage.isDeleted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatTime(latestMessage.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row {
                if (deletedCount > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                        Text("$deletedCount ditarik", color = MaterialTheme.colorScheme.onError)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                if (mediaCount > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        Text("$mediaCount media", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. TAB MEDIA YANG DISELAMATKAN
// ==========================================
@Composable
fun MediaGalleryTab(
    mediaMessages: List<ChatMessageEntity>,
    onThreadClick: (senderName: String, packageName: String) -> Unit
) {
    var selectedMediaType by remember { mutableStateOf("Semua") }
    val filters = listOf("Semua", "Foto", "Video", "Suara")

    val filtered = remember(mediaMessages, selectedMediaType) {
        mediaMessages.filter {
            when (selectedMediaType) {
                "Foto" -> it.mediaType == "IMAGE"
                "Video" -> it.mediaType == "VIDEO"
                "Suara" -> it.mediaType == "AUDIO" || it.mediaType == "VOICE"
                else -> true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilterMediaType(selectedMediaType, filter),
                    onClick = { selectedMediaType = filter },
                    label = { Text(filter) }
                )
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Belum ada media (foto/suara) yang tertangkap dari notifikasi.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { msg ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clickable { onThreadClick(msg.senderName, msg.packageName) }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (msg.mediaUri != null && File(msg.mediaUri).exists()) {
                                AsyncImage(
                                    model = File(msg.mediaUri),
                                    contentDescription = msg.messageText,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        when (msg.mediaType) {
                                            "VOICE", "AUDIO" -> Icons.Default.Audiotrack
                                            "VIDEO" -> Icons.Default.Videocam
                                            else -> Icons.Default.Image
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Tag Pengirim di bagian bawah gambar
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth(),
                                color = Color.Black.copy(alpha = 0.65f)
                            ) {
                                Text(
                                    text = msg.senderName,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun selectedFilterMediaType(current: String, target: String): Boolean = current == target

// ==========================================
// 3. TAB STATUS SAVER WHATSAPP
// ==========================================
@Composable
fun StatusSaverTab(
    statuses: List<StatusModel>,
    isLoading: Boolean,
    hasTreeUri: Boolean,
    onPickFolder: () -> Unit,
    onSaveStatus: (StatusModel) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Banner Hubungkan Folder jika Android 11+ belum terhubung
        if (!hasTreeUri) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hubungkan Folder WhatsApp", fontWeight = FontWeight.Bold)
                        Text("Diperlukan di Android 11+ agar ChatKeep dapat membaca folder status secara otomatis.", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = onPickFolder) {
                        Text("Pilih")
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (statuses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Tidak ada status ditemukan.\n\nTips:\n1. Buka WhatsApp & tonton status kontak Anda terlebih dahulu.\n2. Jika memakai Android 11 ke atas, klik 'Pilih' di atas lalu arahkan ke folder: Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(statuses, key = { it.path }) { status ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = status.uri,
                                contentDescription = status.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Tanda Video jika berkas MP4
                            if (status.isVideo) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp),
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.6f)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Video",
                                        tint = Color.White,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }

                            // Tombol Download ke Galeri
                            IconButton(
                                onClick = { onSaveStatus(status) },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Simpan ke Galeri",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. TAB PENGATURAN LENGKAP
// ==========================================
@SuppressLint("BatteryLife")
@Composable
fun SettingsScreen(
    onPickFolder: () -> Unit,
    hasFolderUri: Boolean
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("chatkeep_prefs", Context.MODE_PRIVATE) }
    var selectedDays by remember { mutableIntStateOf(prefs.getInt("auto_clean_days", 30)) }
    var monitorAllApps by remember { mutableStateOf(prefs.getBoolean("monitor_all_apps", false)) }
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isIgnoringBattery = remember {
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    val retentionOptions = listOf(
        7 to "7 Hari",
        14 to "14 Hari",
        30 to "30 Hari",
        0 to "Simpan Selamanya"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Multi-App Monitoring
        Text("Dukungan Aplikasi Pesan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pantau Semua Aplikasi Pesan", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (monitorAllApps) "Merekam notifikasi dari semua aplikasi perpesanan yang terpasang"
                        else "Hanya memantau WhatsApp, IG, Telegram, Messenger, dan Line",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = monitorAllApps,
                    onCheckedChange = {
                        monitorAllApps = it
                        prefs.edit().putBoolean("monitor_all_apps", it).apply()
                    }
                )
            }
        }

        // WhatsApp Folder SAF
        Text("Folder Status WhatsApp (SAF)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (hasFolderUri) "Folder Terhubung" else "Belum Terhubung", fontWeight = FontWeight.SemiBold)
                    Text("Diperlukan untuk membaca folder .Statuses di Android 11+", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onPickFolder) {
                    Text(if (hasFolderUri) "Ganti" else "Hubungkan")
                }
            }
        }

        // Auto-Clean
        Text("Pembersihan Otomatis Riwayat", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Hapus arsip pesan & media lama setelah:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                retentionOptions.forEach { (days, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedDays = days
                                prefs.edit().putInt("auto_clean_days", days).apply()
                                if (days > 0) {
                                    AutoCleanWorker.schedule(context, days)
                                } else {
                                    WorkManager.getInstance(context).cancelUniqueWork("ChatKeepAutoCleanWork")
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedDays == days, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        }

        // Optimasi Baterai
        Text("Keandalan Latar Belakang", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Abaikan Optimasi Baterai", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (isIgnoringBattery) "Sudah aktif (Layanan berjalan optimal)"
                        else "Disarankan agar layanan tidak dimatikan sistem saat layar mati.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isIgnoringBattery) {
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }) {
                        Text("Atur")
                    }
                }
            }
        }

        // Privasi
        Text("Tentang & Privasi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://naufalputra.my.id/privacy-chatkeep")
                    )
                    context.startActivity(browserIntent)
                }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Security, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Kebijakan Privasi (Privacy Policy)", fontWeight = FontWeight.SemiBold)
                    Text("100% Penyimpanan Lokal • ChatKeep — Naufal v1.1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.OpenInNew, contentDescription = null)
            }
        }
    }
}

// ==========================================
// 5. DETAIL SCREEN PERCAKAPAN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    senderName: String,
    messages: List<ChatMessageEntity>,
    onBack: () -> Unit,
    onDeleteThread: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Riwayat Chat?") },
            text = { Text("Seluruh arsip pesan dari $senderName akan dihapus permanen dari perangkat ini.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteThread()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(senderName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus Percakapan", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(message = msg)
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessageEntity) {
    val containerColor = if (message.isDeleted) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val borderModifier = if (message.isDeleted) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(topStart = 2.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 12.dp))
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 12.dp))
            .background(containerColor)
            .then(borderModifier)
            .padding(12.dp)
    ) {
        if (message.isDeleted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Pesan ini telah dihapus pengirim",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        // Tampilkan Pratinjau Foto jika ada
        if (message.mediaUri != null && File(message.mediaUri).exists()) {
            AsyncImage(
                model = File(message.mediaUri),
                contentDescription = "Lampiran Media",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(6.dp))
        }

        // Jika Voice Note / Suara
        if (message.mediaType == "VOICE" || message.mediaType == "AUDIO") {
            AudioPlayerWidget(audioPath = message.mediaUri)
            Spacer(Modifier.height(6.dp))
        }

        Text(
            text = message.messageText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = formatFullDateTime(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
fun AudioPlayerWidget(audioPath: String?) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(audioPath) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                if (audioPath != null && File(audioPath).exists()) {
                    if (isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                    } else {
                        if (mediaPlayer == null) {
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(context, Uri.fromFile(File(audioPath)))
                                prepare()
                                setOnCompletionListener { isPlaying = false }
                            }
                        }
                        mediaPlayer?.start()
                        isPlaying = true
                    }
                } else {
                    Toast.makeText(context, "Rekaman audio tidak tersedia secara lokal", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Putar Pesan Suara",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text("Pesan Suara / Voice Note", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text("Format audio notifikasi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ==========================================
// HELPER FUNCTIONS
// ==========================================
fun isNotificationServiceEnabled(context: Context): Boolean {
    val cn = ComponentName(context, ChatKeepNotificationService::class.java)
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(cn.flattenToString())
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun formatFullDateTime(timestamp: Long): String {
    return SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(timestamp))
}
