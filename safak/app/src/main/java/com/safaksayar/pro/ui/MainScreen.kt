package com.safaksayar.pro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safaksayar.pro.data.SafakResult
import com.safaksayar.pro.data.SafakSettings
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TR = Locale.forLanguageTag("tr-TR")
val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", TR)
private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss", TR)
private val LONG_FMT = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", TR)

/** Kırmızı başlık: sağ üstte dişli (ana ekran) ya da solda geri oku (ayarlar). */
@Composable
fun Header(title: String, subtitle: String?, onGear: (() -> Unit)?, onBack: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().background(SafakColors.Red).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(48.dp)) {
                if (onBack != null) IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = SafakColors.White)
                }
            }
            Text(
                title, color = SafakColors.White, fontSize = 18.sp, fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
            )
            Box(Modifier.width(48.dp)) {
                if (onGear != null) IconButton(onClick = onGear) {
                    Icon(Icons.Filled.Settings, "Ayarlar", tint = SafakColors.White)
                }
            }
        }
        if (subtitle != null) {
            Text(
                subtitle, color = SafakColors.White, fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }
    }
    Box(
        Modifier.fillMaxWidth().height(14.dp)
            .clip(RoundedCornerShape(bottomStartPercent = 100, bottomEndPercent = 100))
            .background(SafakColors.Red)
    )
}

@Composable
fun MainScreen(
    settings: SafakSettings,
    result: SafakResult?,
    now: LocalDateTime,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Header("ŞAFAK SAYAR", settings.type.label, onGear = onOpenSettings, onBack = null)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (result == null) {
                Welcome(onOpenSettings)
                Spacer(Modifier.height(14.dp))
                ClockCard(now)
            } else {
                ResultContent(settings, result, now)
            }
        }
    }
}

@Composable
private fun ResultContent(settings: SafakSettings, result: SafakResult, now: LocalDateTime) {
            SafakRing(result.daysLeft, 1f - result.progress, result.finished)

            if (result.usingManual) {
                Text(
                    "Çıkış günü elle ayarlandı", color = SafakColors.Blue, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(8.dp))
                        .background(SafakColors.Cream).padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            if (result.daysLeft in 1..81) {
                Spacer(Modifier.height(10.dp))
                PlateBadge(result.daysLeft)
            }

            Spacer(Modifier.height(12.dp))
            ClockCard(now)

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                StatCard("Geçen", result.daysPassed.toString())
                StatCard("Tamamlanan", "%${result.percent}", SafakColors.Green)
                StatCard("Terhis", result.end.format(DATE_FMT), valueSize = 13)
            }

            SectionTitle("AŞAMA TAKİBİ")
            Column(
                Modifier.fillMaxWidth().border(1.dp, SafakColors.RedBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                StageRow("Acemi birliği", if (result.acemiLeft > 0) "${result.acemiLeft} gün kaldı" else "Tamamlandı")
                ProgressBar(result.acemiProgress)
                Spacer(Modifier.height(10.dp))
                StageRow(
                    "Usta birliği (terhis)",
                    when {
                        !result.ustaStarted -> "Başlamadı"
                        result.finished -> "Tamamlandı"
                        else -> "Devam ediyor"
                    }
                )
                ProgressBar(result.ustaProgress)
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                StatCard("Evci izni", "${settings.evciDays} gün", valueSize = 15)
                StatCard("Çarşı izni", "${settings.carsiCount} kez", valueSize = 15)
            }

            Text(
                "Terhis tarihi tahminidir. Kesin tarih için birliğine danış.",
                color = SafakColors.RedDark, fontSize = 11.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
}

@Composable
private fun StageRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 5.dp)) {
        Text(label, color = SafakColors.RedText, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = SafakColors.Red, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ClockCard(now: LocalDateTime) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SafakColors.Red).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(now.format(TIME_FMT), color = SafakColors.White, fontSize = 26.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
        Text(now.format(LONG_FMT), color = SafakColors.White, fontSize = 13.sp)
    }
}

@Composable
private fun Welcome(onOpenSettings: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().border(1.dp, SafakColors.RedBorder, RoundedCornerShape(14.dp)).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Şafağını saymaya başla", color = SafakColors.Red, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text(
            "Sülüs (teslim) tarihini ve askerlik türünü gir, kalan günün hemen hesaplansın.",
            color = SafakColors.RedText, fontSize = 14.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp)
        )
        Button(
            onClick = onOpenSettings,
            colors = ButtonDefaults.buttonColors(containerColor = SafakColors.Red, contentColor = SafakColors.White)
        ) { Text("Sülüs tarihini gir") }
    }
}
