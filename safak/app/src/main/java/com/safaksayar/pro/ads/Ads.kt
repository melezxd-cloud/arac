package com.safaksayar.pro.ads

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.safaksayar.pro.BuildConfig
import com.safaksayar.pro.ui.SafakColors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reklam izni (UMP) ve AdMob başlatma.
 * Avrupa'daki kullanıcılara izin penceresi gösterilir; izin alınmadan reklam istenmez.
 */
object AdsManager {
    private val initialized = AtomicBoolean(false)

    fun gatherConsentAndInit(activity: Activity, onReady: (canShowAds: Boolean) -> Unit) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity, params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    initIfAllowed(activity, info, onReady)
                }
            },
            { initIfAllowed(activity, info, onReady) }
        )
        // Önceki oturumda izin alındıysa beklemeden başla.
        if (info.canRequestAds()) initIfAllowed(activity, info, onReady)
    }

    private fun initIfAllowed(activity: Activity, info: ConsentInformation, onReady: (Boolean) -> Unit) {
        if (!info.canRequestAds()) { onReady(false); return }
        if (initialized.getAndSet(true)) { onReady(true); return }
        MobileAds.initialize(activity) { onReady(true) }
    }

    fun privacyOptionsRequired(activity: Activity): Boolean =
        UserMessagingPlatform.getConsentInformation(activity).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }
}

/** Ana ekranın en altında sabit, ekran genişliğine uyan banner. */
@Composable
fun BottomBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val adView = remember(widthDp) {
        AdView(context).apply {
            adUnitId = BuildConfig.BANNER_AD_UNIT_ID
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView) { onDispose { adView.destroy() } }
    Box(
        modifier.fillMaxWidth().background(SafakColors.White).navigationBarsPadding().padding(top = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(factory = { adView })
    }
}
