package com.safaksayar.pro.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class ServiceType(val months: Long, val label: String) {
    ER_6(6, "6 ay er"),
    YEDEK_12(12, "12 ay yedek subay / astsubay"),
    BEDELLI_1(1, "1 ay bedelli");

    companion object {
        fun of(name: String?): ServiceType = entries.firstOrNull { it.name == name } ?: ER_6
    }
}

data class SafakSettings(
    val type: ServiceType = ServiceType.ER_6,
    val startDate: LocalDate? = null,
    val acemiDays: Int = 28,
    val evciDays: Int = 0,
    val carsiCount: Int = 0,
    val raporDays: Int = 0,
    val cezaDays: Int = 0,
    val manualEnabled: Boolean = false,
    val manualEndDate: LocalDate? = null,
    val notificationEnabled: Boolean = true,
)

data class SafakResult(
    val start: LocalDate,
    val baseEnd: LocalDate,
    val end: LocalDate,
    val usingManual: Boolean,
    val manualInvalid: Boolean,
    val totalDays: Int,
    val daysLeft: Int,
    val daysPassed: Int,
    val progress: Float,
    val raporExtra: Int,
    val cezaExtra: Int,
    val acemiEnd: LocalDate,
    val acemiLeft: Int,
    val acemiProgress: Float,
    val ustaStarted: Boolean,
    val ustaProgress: Float,
) {
    val percent: Int get() = Math.round(progress * 100)
    val finished: Boolean get() = daysLeft == 0
}

/**
 * Şafak hesabı (TSK kuralları):
 * - Sülüs gününden terhise kesintisiz tek takvim.
 * - Süre sabit gün değil takvim ayıdır (5 Kasım + 6 ay = 5 Mayıs).
 *   Ay sonu taşmasında ayın son gününe yuvarlanır (31 Ağustos + 6 ay = 28/29 Şubat).
 * - Evci, çarşı, dağıtım ve yol izni hizmetten sayılır, şafağı değiştirmez.
 * - Toplam rapor / hava değişiminin ilk 6 günü hizmetten sayılır, aşan her gün terhisi ileri atar.
 * - Ceza / firar / geç katılış günleri terhisi birebir ileri atar.
 * - Kullanıcı çıkış gününü elle seçerse sayaç doğrudan o güne sayar.
 */
object SafakCalculator {
    const val RAPOR_FREE_DAYS = 6

    fun compute(s: SafakSettings, today: LocalDate): SafakResult? {
        val start = s.startDate ?: return null
        val baseEnd = start.plusMonths(s.type.months)
        val raporExtra = (s.raporDays - RAPOR_FREE_DAYS).coerceAtLeast(0)
        val cezaExtra = s.cezaDays.coerceAtLeast(0)
        val autoEnd = baseEnd.plusDays((raporExtra + cezaExtra).toLong())

        val manual = s.manualEndDate
        val manualValid = s.manualEnabled && manual != null && manual.isAfter(start)
        val end = if (manualValid) manual!! else autoEnd

        val total = days(start, end).coerceAtLeast(1)
        val left = days(today, end).coerceIn(0, total)
        val passed = total - left
        val progress = passed.toFloat() / total

        val acemiLen = s.acemiDays.coerceAtLeast(1)
        val acemiEnd = start.plusDays(acemiLen.toLong())
        val acemiLeft = days(today, acemiEnd).coerceIn(0, acemiLen)
        val acemiProgress = (acemiLen - acemiLeft).toFloat() / acemiLen

        val ustaStarted = acemiLeft == 0
        val ustaTotal = days(acemiEnd, end).coerceAtLeast(1)
        val ustaProgress = if (!ustaStarted) 0f
        else ((ustaTotal - left).toFloat() / ustaTotal).coerceIn(0f, 1f)

        return SafakResult(
            start = start,
            baseEnd = baseEnd,
            end = end,
            usingManual = manualValid,
            manualInvalid = s.manualEnabled && !manualValid,
            totalDays = total,
            daysLeft = left,
            daysPassed = passed,
            progress = progress,
            raporExtra = if (manualValid) 0 else raporExtra,
            cezaExtra = if (manualValid) 0 else cezaExtra,
            acemiEnd = acemiEnd,
            acemiLeft = acemiLeft,
            acemiProgress = acemiProgress,
            ustaStarted = ustaStarted,
            ustaProgress = ustaProgress,
        )
    }

    /** Hesaplanan (elle seçilmemiş) terhis tarihi; elle tarih alanının başlangıç değeri için. */
    fun autoEnd(s: SafakSettings): LocalDate? {
        val start = s.startDate ?: return null
        val extra = (s.raporDays - RAPOR_FREE_DAYS).coerceAtLeast(0) + s.cezaDays.coerceAtLeast(0)
        return start.plusMonths(s.type.months).plusDays(extra.toLong())
    }

    private fun days(from: LocalDate, to: LocalDate): Int = ChronoUnit.DAYS.between(from, to).toInt()
}
