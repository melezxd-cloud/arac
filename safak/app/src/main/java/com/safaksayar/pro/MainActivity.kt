package com.safaksayar.pro

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.review.ReviewManagerFactory
import com.safaksayar.pro.ads.AdsManager
import com.safaksayar.pro.ads.BottomBanner
import com.safaksayar.pro.data.SafakCalculator
import com.safaksayar.pro.data.SafakSettings
import com.safaksayar.pro.data.SettingsRepository
import com.safaksayar.pro.notify.SafakNotifier
import com.safaksayar.pro.ui.MainScreen
import com.safaksayar.pro.ui.SafakColors
import com.safaksayar.pro.ui.SettingsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {

    private lateinit var repo: SettingsRepository
    private var adsReady by mutableStateOf(false)
    private var privacyRequired by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            // Kırmızı başlık durum çubuğunun arkasına uzanır, ikonlar beyaz kalır.
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.WHITE)
        )
        super.onCreate(savedInstanceState)
        repo = SettingsRepository(applicationContext)
        SafakNotifier.createChannels(this)

        AdsManager.gatherConsentAndInit(this) { ok ->
            runOnUiThread {
                adsReady = ok
                privacyRequired = AdsManager.privacyOptionsRequired(this)
            }
        }

        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = SafakColors.Red, surface = SafakColors.White)) {
                SafakApp()
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun SafakApp() {
        val loaded by repo.settings.collectAsStateWithLifecycle<SafakSettings?>(initialValue = null)
        val settings = loaded ?: SafakSettings()
        var showSettings by rememberSaveable { mutableStateOf(false) }
        var now by remember { mutableStateOf(LocalDateTime.now()) }
        val scope = rememberCoroutineScope()

        // Saat her saniye, şafak gün değişince kendiliğinden güncellenir.
        LaunchedEffect(Unit) {
            while (true) {
                now = LocalDateTime.now()
                delay(1000L - System.currentTimeMillis() % 1000L)
            }
        }
        val today = now.toLocalDate()
        val result = remember(settings, today) { SafakCalculator.compute(settings, today) }

        // Ayar her değiştiğinde bildirim çubuğunu yenile.
        LaunchedEffect(settings) { SafakNotifier.refresh(applicationContext) }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            lifecycleScope.launch { SafakNotifier.refresh(applicationContext) }
        }

        // Android 13+ bildirim izni: ilk açılışta bir kez sorulur.
        val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            lifecycleScope.launch { SafakNotifier.refresh(applicationContext) }
        }
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !SafakNotifier.canPost(this@MainActivity)) {
                permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        BackHandler(enabled = showSettings) { showSettings = false }

        Column(Modifier.fillMaxSize().background(SafakColors.White)) {
            if (loaded == null) {
                // Ayarlar okunurken kısa bir an boş beyaz ekran (karşılama ekranı yanıp sönmesin diye).
            } else if (showSettings) {
                SettingsScreen(
                    settings = settings,
                    result = result,
                    onChange = { t -> scope.launch { repo.update(t) } },
                    onBack = { showSettings = false },
                    onRate = { openReview() },
                    onShare = { shareApp() },
                    onAdPrivacy = if (privacyRequired) ({ AdsManager.showPrivacyOptions(this@MainActivity) }) else null,
                    modifier = Modifier.weight(1f),
                )
            } else {
                MainScreen(
                    settings = settings,
                    result = result,
                    now = now,
                    onOpenSettings = { showSettings = true },
                    modifier = Modifier.weight(1f),
                )
                if (adsReady) BottomBanner() else androidx.compose.foundation.layout.Spacer(Modifier.navigationBarsPadding())
            }
        }
    }

    private fun openReview() {
        val manager = ReviewManagerFactory.create(this)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) manager.launchReviewFlow(this, task.result) else openStorePage()
        }
    }

    private fun openStorePage() {
        val uri = android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }

    private fun shareApp() {
        val text = "Şafağını Şafak Sayar Pro ile say: https://play.google.com/store/apps/details?id=$packageName"
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text),
                "Paylaş"
            )
        )
    }
}
