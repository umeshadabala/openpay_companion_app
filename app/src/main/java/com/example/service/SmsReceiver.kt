package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.ApiClient
import com.example.data.PrefsManager
import com.example.data.TransactionRequest
import com.example.model.SyncLogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object SmsSyncNotifier {
    private val _syncEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val syncEvent: SharedFlow<Unit> = _syncEvent

    fun notifySmsSynced() {
        _syncEvent.tryEmit(Unit)
    }
}

class SmsReceiver : BroadcastReceiver() {

    private val patterns = listOf(
        "HDFC", "SBI", "ICIC", "AXIS", "PNB", "BOB", "KOTAK", 
        "YES", "IDBI", "CANARA", "UNION", "PAYTM", "PHONPE", 
        "GPAY", "BHIM", "AMZN", "CRED"
    )
    private val indianBankRegex = Regex("^[a-zA-Z]{2}-[a-zA-Z]{6}$")

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = PrefsManager(context)
        if (!prefs.isPaired) {
            Log.d("SmsReceiver", "Skipping SMS: Device not paired with companion app.")
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        // Extract sender and build full message text (conglomerating multipart if needed)
        val sender = messages[0].originatingAddress ?: "Unknown"
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }
        val timestampMs = messages[0].timestampMillis

        // Apply bank filtering
        if (!shouldForward(sender)) {
            Log.d("SmsReceiver", "Skipping SMS: sender '$sender' does not match bank patterns.")
            return
        }

        val timestampIso = formatEpochToIso8601(timestampMs)

        // Handle auto-sync toggle
        if (!prefs.autoSyncEnabled) {
            Log.d("SmsReceiver", "Auto-sync disabled. Storing missed log entry locally.")
            val entry = SyncLogEntry(
                sender = sender,
                smsPreview = fullBody,
                timestamp = timestampIso,
                status = "missed",
                transactionId = null
            )
            prefs.addSyncLog(entry)
            SmsSyncNotifier.notifySmsSynced()
            return
        }

        // Asynchronously forward the SMS text to Vercel/Next.js backend via our dynamic service client
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val service = ApiClient.getService(prefs.baseUrl)
                val request = TransactionRequest(
                    merchantCode = prefs.merchantCode,
                    smsText = fullBody,
                    senderHeader = sender,
                    timestamp = timestampIso,
                    deviceId = prefs.getDeviceId(),
                    deviceName = prefs.getDeviceName()
                )

                // Call forward transaction
                val response = service.forwardTransaction(
                    url = "${prefs.baseUrl.trimEnd('/')}/api/transactions",
                    request = request
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val txId = response.body()?.transactionId
                    val entry = SyncLogEntry(
                        sender = sender,
                        smsPreview = fullBody,
                        timestamp = timestampIso,
                        status = "success",
                        transactionId = txId
                    )
                    prefs.addSyncLog(entry)
                    showNotification(context, "OpenPay: Payment alert synced ✓", "Forwarded SMS from $sender successfully.")
                } else {
                    val errMsg = response.body()?.message ?: "API returned failure"
                    Log.e("SmsReceiver", "Forward transaction failed: $errMsg")
                    val entry = SyncLogEntry(
                        sender = sender,
                        smsPreview = fullBody,
                        timestamp = timestampIso,
                        status = "failed",
                        transactionId = null
                    )
                    prefs.addSyncLog(entry)
                    showNotification(context, "OpenPay: Sync failed ✗", "Alert from $sender failed to upload.")
                }
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Network error forwarding transaction", e)
                val entry = SyncLogEntry(
                    sender = sender,
                    smsPreview = fullBody,
                    timestamp = timestampIso,
                    status = "failed",
                    transactionId = null
                )
                prefs.addSyncLog(entry)
                showNotification(context, "OpenPay: Sync failed ✗", "Network or timeout error trying to connect.")
            } finally {
                SmsSyncNotifier.notifySmsSynced()
                pendingResult.finish()
            }
        }
    }

    private fun shouldForward(sender: String): Boolean {
        val uppercaseSender = sender.uppercase(Locale.US)
        val keywordMatch = patterns.any { uppercaseSender.contains(it) }
        val formatMatch = indianBankRegex.matches(sender)
        return keywordMatch || formatMatch
    }

    private fun formatEpochToIso8601(epochMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(epochMs))
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val channelId = "openpay_notifications"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "OpenPay Sync Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for forward transaction status alerts"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            Log.e("SmsReceiver", "Notification capability not authorized", e)
        }
    }
}
