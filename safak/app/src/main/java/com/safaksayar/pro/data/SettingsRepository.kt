package com.safaksayar.pro.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "safak_settings")

class SettingsRepository(private val context: Context) {

    private object K {
        val TYPE = stringPreferencesKey("type")
        val START = longPreferencesKey("start_epoch_day")
        val ACEMI = intPreferencesKey("acemi_days")
        val EVCI = intPreferencesKey("evci_days")
        val CARSI = intPreferencesKey("carsi_count")
        val RAPOR = intPreferencesKey("rapor_days")
        val CEZA = intPreferencesKey("ceza_days")
        val MANUAL_ON = booleanPreferencesKey("manual_enabled")
        val MANUAL_END = longPreferencesKey("manual_end_epoch_day")
        val NOTIF_ON = booleanPreferencesKey("notification_enabled")
        val LAST_MILESTONE = stringPreferencesKey("last_milestone")
    }

    val settings: Flow<SafakSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): SafakSettings = settings.first()

    suspend fun update(transform: (SafakSettings) -> SafakSettings) {
        context.dataStore.edit { p ->
            val n = transform(p.toSettings())
            p[K.TYPE] = n.type.name
            n.startDate?.let { p[K.START] = it.toEpochDay() } ?: p.remove(K.START)
            p[K.ACEMI] = n.acemiDays
            p[K.EVCI] = n.evciDays
            p[K.CARSI] = n.carsiCount
            p[K.RAPOR] = n.raporDays
            p[K.CEZA] = n.cezaDays
            p[K.MANUAL_ON] = n.manualEnabled
            n.manualEndDate?.let { p[K.MANUAL_END] = it.toEpochDay() } ?: p.remove(K.MANUAL_END)
            p[K.NOTIF_ON] = n.notificationEnabled
        }
    }

    /** Aynı özel gün bildirimi iki kez gönderilmesin diye son gönderilenin anahtarı. */
    suspend fun lastMilestone(): String? = context.dataStore.data.first()[K.LAST_MILESTONE]

    suspend fun setLastMilestone(key: String) {
        context.dataStore.edit { it[K.LAST_MILESTONE] = key }
    }

    private fun Preferences.toSettings() = SafakSettings(
        type = ServiceType.of(this[K.TYPE]),
        startDate = this[K.START]?.let(LocalDate::ofEpochDay),
        acemiDays = this[K.ACEMI] ?: 28,
        evciDays = this[K.EVCI] ?: 0,
        carsiCount = this[K.CARSI] ?: 0,
        raporDays = this[K.RAPOR] ?: 0,
        cezaDays = this[K.CEZA] ?: 0,
        manualEnabled = this[K.MANUAL_ON] ?: false,
        manualEndDate = this[K.MANUAL_END]?.let(LocalDate::ofEpochDay),
        notificationEnabled = this[K.NOTIF_ON] ?: true,
    )
}
