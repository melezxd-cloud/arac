package com.riri.notes

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Uygulamanın ekranları assets/www/index.html içinde; bu aktivite onları tam ekran gösterir
 * ve veriyi kaydetme, bildirim ve widget işleri için JavaScript'e küçük bir köprü açar.
 */
class MainActivity : Activity() {

    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#FF4FA3")

        web = WebView(this)
        web.setBackgroundColor(Color.parseColor("#FFE36E"))
        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
            textZoom = 100
        }
        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()
        web.addJavascriptInterface(Bridge(), "Android")
        setContentView(web)
        web.loadUrl("file:///android_asset/www/index.html")

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        web.evaluateJavascript("window.ririBack ? window.ririBack() : false") { handled ->
            if (handled != "true") {
                @Suppress("DEPRECATION")
                super.onBackPressed()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        web.evaluateJavascript("window.ririSave && window.ririSave()", null)
    }

    inner class Bridge {
        private val prefs get() = getSharedPreferences("riri", Context.MODE_PRIVATE)

        /** Bütün uygulama verisi tek bir JSON olarak telefon hafızasına yazılır. */
        @JavascriptInterface
        fun save(json: String) {
            prefs.edit().putString("state", json).apply()
        }

        @JavascriptInterface
        fun load(): String = prefs.getString("state", "") ?: ""

        /** Ana ekran widget'ının göstereceği bilgiler. */
        @JavascriptInterface
        fun updateWidget(json: String) {
            prefs.edit().putString("widget", json).apply()
            RiriWidget.refreshAll(this@MainActivity)
        }

        /** Doğum günü listesi ve hatırlatma tercihleri; alarmlar yeniden kurulur. */
        @JavascriptInterface
        fun scheduleBirthdays(json: String) {
            prefs.edit().putString("birthdays", json).apply()
            Alarms.scheduleAll(this@MainActivity)
        }

        @JavascriptInterface
        fun isAndroid(): Boolean = true
    }
}
