package com.example.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.model.SyncLogEntry
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.UUID

class PrefsManager(private val context: Context) {
    private val prefsName = "openpay_companion_prefs"
    private val sharedPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val syncLogType = Types.newParameterizedType(List::class.java, SyncLogEntry::class.java)
    private val syncLogAdapter = moshi.adapter<List<SyncLogEntry>>(syncLogType)

    fun getDeviceId(): String {
        var existingId = sharedPrefs.getString("deviceId", null)
        if (existingId.isNullOrEmpty()) {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            existingId = if (!androidId.isNullOrEmpty()) {
                androidId
            } else {
                UUID.randomUUID().toString()
            }
            sharedPrefs.edit().putString("deviceId", existingId).apply()
        }
        return existingId
    }

    fun getDeviceName(): String {
        return Build.MODEL ?: "Android Device"
    }

    var isPaired: Boolean
        get() = sharedPrefs.getBoolean("isPaired", false)
        set(value) = sharedPrefs.edit().putBoolean("isPaired", value).apply()

    var merchantCode: String
        get() = sharedPrefs.getString("merchantCode", "") ?: ""
        set(value) = sharedPrefs.edit().putString("merchantCode", value).apply()

    var merchantName: String
        get() = sharedPrefs.getString("merchantName", "") ?: ""
        set(value) = sharedPrefs.edit().putString("merchantName", value).apply()

    var merchantId: String
        get() = sharedPrefs.getString("merchantId", "") ?: ""
        set(value) = sharedPrefs.edit().putString("merchantId", value).apply()

    var baseUrl: String
        get() = sharedPrefs.getString("baseUrl", "https://open-pay-822w.vercel.app") ?: "https://open-pay-822w.vercel.app"
        set(value) = sharedPrefs.edit().putString("baseUrl", value).apply()

    var autoSyncEnabled: Boolean
        get() = sharedPrefs.getBoolean("autoSyncEnabled", true)
        set(value) = sharedPrefs.edit().putBoolean("autoSyncEnabled", value).apply()

    fun getSyncLogs(): List<SyncLogEntry> {
        val json = sharedPrefs.getString("syncLog", null) ?: return emptyList()
        return try {
            syncLogAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addSyncLog(entry: SyncLogEntry) {
        val current = getSyncLogs().toMutableList()
        current.add(0, entry) // Insert at top of the list
        val trimmed = if (current.size > 20) current.take(20) else current
        val json = syncLogAdapter.toJson(trimmed)
        sharedPrefs.edit().putString("syncLog", json).apply()
    }

    fun clearLog() {
        sharedPrefs.edit().remove("syncLog").apply()
    }

    fun unlink() {
        sharedPrefs.edit()
            .remove("isPaired")
            .remove("merchantCode")
            .remove("merchantName")
            .remove("merchantId")
            .apply()
    }
}
