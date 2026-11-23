package com.safaksayar.pro.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safaksayar.pro.data.SafakCalculator
import com.safaksayar.pro.data.SafakResult
import com.safaksayar.pro.data.SafakSettings
import com.safaksayar.pro.data.ServiceType
import java.time.LocalDate

private val fieldShape = RoundedCornerShape(12.dp)
private val fieldMod = Modifier.fillMaxWidth().padding(bottom = 7.dp)

@Composable
fun SettingsScreen(
    settings: SafakSettings,
    result: SafakResult?,
    onChange: ((SafakSettings) -> SafakSettings) -> Unit,
    onBack: () -> Unit,
    onRate: () -> Unit,
    onShare: () -> Unit,
    onAdPrivacy: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Header("AYARLAR", null, onGear = null, onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 16.dp).navigationBarsPadding()
        ) {
            SectionTitle("ASKERLİK BİLGİLERİ")
            Column(fieldMod.border(1.dp, SafakColors.RedBorder, fieldShape).padding(vertical = 4.dp)) {
                ServiceType.entries.forEach { t ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onChange { it.copy(type = t) } }.padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.type == t, onClick = { onChange { it.copy(type = t) } },
                            colors = RadioButtonDefaults.colors(selectedColor = SafakColors.Red)
                        )
                        Text(t.label, color = SafakColors.RedText, fontSize = 14.sp)
                    }
                }
            }
            DateField("Sülüs (teslim) tarihi", settings.startDate) { d -> onChange { it.copy(startDate = d) } }
            Stepper("Acemilik süresi (gün)", settings.acemiDays, 1, 90) { v -> onChange { it.copy(acemiDays = v) } }

            SectionTitle("ÇIKIŞ GÜNÜ")
            SwitchRow("Çıkış günümü kendim ayarlamak istiyorum", settings.manualEnabled) { on ->
                onChange {
                    val seed = it.manualEndDate ?: SafakCalculator.autoEnd(it)
                    it.copy(manualEnabled = on, manualEndDate = if (on) seed else it.manualEndDate)
                }
            }
            if (settings.manualEnabled) {
                DateField("Çıkış (terhis) günüm", settings.manualEndDate) { d -> onChange { it.copy(manualEndDate = d) } }
                if (result?.manualInvalid == true) {
                    Note("Çıkış günü sülüs tarihinden sonra olmalı.", SafakColors.Red)
                } else {
                    Note("Sayaç bu güne göre geri sayar. Rapor ve ceza hesabı devre dışı.")
                }
            }

            SectionTitle("İZİNLER")
            Stepper("Evci izni (gün)", settings.evciDays, 0, 365) { v -> onChange { it.copy(evciDays = v) } }
            Stepper("Çarşı izni (kez)", settings.carsiCount, 0, 999) { v -> onChange { it.copy(carsiCount = v) } }
            Note("Evci ve çarşı izni hizmetten sayılır, şafağı değiştirmez.")

            Column(Modifier.alpha(if (settings.manualEnabled) 0.45f else 1f)) {
                SectionTitle("ŞAFAĞI UZATANLAR")
                Stepper("Toplam rapor / hava değişimi", settings.raporDays, 0, 365, enabled = !settings.manualEnabled) { v ->
                    onChange { it.copy(raporDays = v) }
                }
                Stepper("Ceza / firar / geç katılış", settings.cezaDays, 0, 365, enabled = !settings.manualEnabled) { v ->
                    onChange { it.copy(cezaDays = v) }
                }
                Note("Raporun ilk 6 günü hizmetten sayılır, aşan her gün terhisi ileri atar.")
            }

            if (result != null) {
                val lines = buildList {
                    if (result.usingManual) {
                        add("Elle ayarlanan çıkış: ${result.end.format(DATE_FMT)}")
                        add("Toplam süre: ${result.totalDays} gün")
                    } else {
                        add("Temel terhis: ${result.baseEnd.format(DATE_FMT)} (${settings.type.months} takvim ayı)")
                        if (result.raporExtra > 0) add("6 günü aşan rapor: +${result.raporExtra} gün")
                        if (result.cezaExtra > 0) add("Ceza / firar / geç katılış: +${result.cezaExtra} gün")
                        add("Tahmini terhis: ${result.end.format(DATE_FMT)}")
                    }
                }
                Text(
                    lines.joinToString("\n"), color = SafakColors.RedDark, fontSize = 12.sp, lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 8.dp, start = 2.dp)
                )
            }

            SectionTitle("BİLDİRİM")
            SwitchRow("Bildirim çubuğunda göster", settings.notificationEnabled) { on ->
                onChange { it.copy(notificationEnabled = on) }
            }

            SectionTitle("UYGULAMA")
            ActionRow(Icons.Filled.Star, "Uygulamayı değerlendir", onRate)
            ActionRow(Icons.Filled.Share, "Arkadaşınla paylaş", onShare)
            if (onAdPrivacy != null) ActionRow(Icons.Filled.PrivacyTip, "Reklam gizlilik ayarları", onAdPrivacy)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Note(text: String, color: androidx.compose.ui.graphics.Color = SafakColors.Green) {
    Text(text, color = color, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
}

@Composable
private fun FieldRow(label: String, content: @Composable () -> Unit) {
    Row(
        fieldMod.border(1.dp, SafakColors.RedBorder, fieldShape).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = SafakColors.RedDark, fontSize = 13.sp, modifier = Modifier.weight(1f))
        content()
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    FieldRow(label) {
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = SafakColors.Red, checkedThumbColor = SafakColors.White,
                uncheckedTrackColor = SafakColors.SwitchOff, uncheckedThumbColor = SafakColors.White,
                uncheckedBorderColor = SafakColors.SwitchOff
            )
        )
    }
}

@Composable
private fun Stepper(label: String, value: Int, min: Int, max: Int, enabled: Boolean = true, onValue: (Int) -> Unit) {
    FieldRow(label) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            RoundIcon(Icons.Filled.Remove, "Azalt", enabled && value > min) { onValue(value - 1) }
            Text(
                value.toString(), color = SafakColors.Red, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.width(40.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            RoundIcon(Icons.Filled.Add, "Artır", enabled && value < max) { onValue(value + 1) }
        }
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.size(34.dp).border(1.dp, SafakColors.RedBorder, CircleShape)
    ) {
        Icon(icon, desc, tint = if (enabled) SafakColors.Red else SafakColors.RedBorder, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        fieldMod.border(1.dp, SafakColors.RedBorder, fieldShape).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SafakColors.Red, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = SafakColors.RedText, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, date: LocalDate?, onPick: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    FieldRow(label) {
        TextButton(onClick = { open = true }) {
            Text(
                date?.format(DATE_FMT) ?: "Tarih seç", color = SafakColors.Red,
                fontSize = 15.sp, fontWeight = FontWeight.Medium
            )
        }
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: LocalDate.now()).toEpochDay() * DAY_MS
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPick(LocalDate.ofEpochDay(it / DAY_MS)) }
                    open = false
                }) { Text("Tamam", color = SafakColors.Red) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Vazgeç", color = SafakColors.RedDark) } }
        ) {
            DatePicker(state = state, showModeToggle = true)
        }
    }
}

private const val DAY_MS = 86_400_000L
