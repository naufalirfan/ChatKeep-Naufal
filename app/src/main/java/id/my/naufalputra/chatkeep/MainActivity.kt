package id.my.naufalputra.chatkeep

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.my.naufalputra.chatkeep.ui.*
import id.my.naufalputra.chatkeep.ui.theme.ChatKeepTheme
import id.my.naufalputra.chatkeep.worker.AutoCleanWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Jadwalkan pembersihan otomatis sesuai pengaturan tersimpan (default 30 hari)
        val prefs = getSharedPreferences("chatkeep_prefs", Context.MODE_PRIVATE)
        val savedDays = prefs.getInt("auto_clean_days", 30)
        if (savedDays > 0) {
            AutoCleanWorker.schedule(this, savedDays)
        }

        setContent {
            ChatKeepTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ChatKeepApp()
                }
            }
        }
    }
}

@Composable
fun ChatKeepApp(viewModel: ChatViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Permintaan Izin Runtime POST_NOTIFICATIONS untuk Android 13 (API 33) ke atas
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Status izin ditangani oleh sistem */ }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!isGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // 2. State Data & Navigasi
    val allMessages by viewModel.allMessages.collectAsStateWithLifecycle()
    var selectedThread by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    // 3. Refresh status izin NotificationListener saat kembali ke aplikasi (ON_RESUME)
    var resumeTrigger by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 4. Routing Layar
    when {
        showSettings -> {
            BackHandler { showSettings = false }
            SettingsScreen(onBack = { showSettings = false })
        }
        selectedThread != null -> {
            val (senderName, packageName) = selectedThread!!
            val threadMessages by viewModel
                .getThreadMessages(senderName, packageName)
                .collectAsStateWithLifecycle(initialValue = emptyList())

            BackHandler { selectedThread = null }

            ChatDetailScreen(
                senderName = senderName,
                messages = threadMessages,
                onBack = { selectedThread = null },
                onDeleteThread = {
                    viewModel.deleteThread(senderName, packageName)
                    selectedThread = null
                }
            )
        }
        else -> {
            key(resumeTrigger) {
                ChatKeepDashboardScreen(
                    messages = allMessages,
                    onThreadClick = { sender, pkg ->
                        selectedThread = sender to pkg
                    },
                    onOpenSettings = { showSettings = true }
                )
            }
        }
    }
}
