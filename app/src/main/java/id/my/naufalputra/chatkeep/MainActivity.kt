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

        // Jadwalkan pembersihan otomatis sesuai preferensi pengguna (default 30 hari)
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

    // 1. Permintaan Izin Runtime Notifikasi (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Status izin ditangani oleh sistem */ }
    )

    // 2. Permintaan Izin Media (Untuk membaca media jika diperlukan)
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { /* Status ditangani sistem */ }
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

            // Minta izin baca media foto & video
            mediaPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            )
        } else {
            mediaPermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            )
        }
    }

    // State navigasi thread
    var selectedThread by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Trigger refresh status izin saat kembali ke foreground
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

    // Routing Layar: Detail Thread vs Main Tab Scaffold
    val currentThread = selectedThread
    if (currentThread != null) {
        val (senderName, packageName) = currentThread
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
    } else {
        key(resumeTrigger) {
            MainTabScaffold(
                viewModel = viewModel,
                onThreadClick = { sender, pkg ->
                    selectedThread = sender to pkg
                }
            )
        }
    }
}
