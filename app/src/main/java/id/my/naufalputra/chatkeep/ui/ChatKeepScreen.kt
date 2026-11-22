package id.my.naufalputra.chatkeep.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.work.WorkManager
import id.my.naufalputra.chatkeep.data.ChatMessageEntity
import id.my.naufalputra.chatkeep.service.ChatKeepNotificationService
import id.my.naufalputra.chatkeep.worker.AutoCleanWorker
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatKeepDashboardScreen(
    messages: List<ChatMessageEntity>,
    onThreadClick: (senderName: String, packageName: String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("Semua") }
    var searchQuery by remember { mutableStateOf("") }
    var showDisclosureDialog by remember { mutableStateOf(false) }
    val isPermissionGranted = remember { isNotificationServiceEnabled(context) }
    val filters = listOf("Semua", "WhatsApp", "Instagram", "Dihapus")

    // Prominent Disclosure Dialog (Wajib untuk Kepatuhan Kebijakan Google Play)
    if (showDisclosureDialog) {
        AlertDialog(
            onDismissRequest = { showDisclosureDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null) },
            title = { Text("Pengungkapan Akses Notifikasi") },
            text = {
                Text(
                    "ChatKeep — Naufal memerlukan izin Akses Notifikasi untuk membaca teks pesan masuk dari WhatsApp dan Instagram.\n\n" +
                    "• Tujuan: Mencadangkan pesan secara lokal agar tetap terbaca meskipun dihapus pengirim.\n" +
                    "• Privasi: 100% data disimpan di penyimpanan internal perangkat Anda dan tidak pernah dikirim ke internet."
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
                Triple(latest, deletedCount, list.size)
            }
            .sortedByDescending { it.first.timestamp }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ChatKeep — Naufal", fontWeight = FontWeight.Bold)
                        Text(
                            "Arsip Notifikasi Lokal & Anti-Hapus",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Banner Peringatan jika Izin Notifikasi belum aktif
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
                            Text(
                                "Akses Notifikasi Belum Aktif",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Aktifkan izin agar ChatKeep dapat mencadangkan pesan masuk.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Button(onClick = { showDisclosureDialog = true }) {
                            Text("Aktifkan")
                        }
                    }
                }
            }

            // Kolom Pencarian
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Cari pengirim atau isi pesan...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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

            // Daftar Percakapan
            if (threads.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Belum ada riwayat notifikasi tersimpan.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(threads, key = { "${it.first.senderName}_${it.first.packageName}" }) { (latest, deletedCount, _) ->
                        ThreadListItem(
                            latestMessage = latest,
                            deletedCount = deletedCount,
                            onClick = { onThreadClick(latest.senderName, latest.packageName) }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun ThreadListItem(
    latestMessage: ChatMessageEntity,
    deletedCount: Int,
    onClick: () -> Unit
) {
    val appBadge = when {
        latestMessage.packageName.startsWith("com.whatsapp") -> "WA"
        latestMessage.packageName == "com.instagram.android" -> "IG"
        else -> "APP"
    }
    val badgeColor = if (appBadge == "WA") Color(0xFF25D366) else Color(0xFFE1306C)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar Inisial
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
            Text(
                text = latestMessage.messageText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (latestMessage.isDeleted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatTime(latestMessage.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (deletedCount > 0) {
                Spacer(Modifier.height(4.dp))
                Badge(containerColor = MaterialTheme.colorScheme.error) {
                    Text("$deletedCount dihapus", color = MaterialTheme.colorScheme.onError)
                }
            }
        }
    }
}

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
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus Percakapan",
                            tint = MaterialTheme.colorScheme.error
                        )
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

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("chatkeep_prefs", Context.MODE_PRIVATE) }
    var selectedDays by remember { mutableIntStateOf(prefs.getInt("auto_clean_days", 30)) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bagian 1: Auto-Clean Log Lama
            Text("Pembersihan Otomatis Riwayat", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Hapus arsip pesan secara otomatis setelah:",
                        style = MaterialTheme.typography.bodyMedium
                    )
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
                            RadioButton(
                                selected = selectedDays == days,
                                onClick = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            }

            // Bagian 2: Optimasi Baterai
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

            // Bagian 3: Kebijakan Privasi
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
                    Icon(Icons.Default.Policy, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Kebijakan Privasi (Privacy Policy)", fontWeight = FontWeight.SemiBold)
                        Text(
                            "100% Penyimpanan Lokal • ChatKeep — Naufal v1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                }
            }
        }
    }
}

// Helper Functions
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
