package com.example.ui.dashboard

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ApiClient
import com.example.data.PrefsManager
import com.example.data.TransactionRequest
import com.example.model.SyncLogEntry
import com.example.service.SmsSyncNotifier
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    prefs: PrefsManager,
    onUnlink: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Sync state logs list loaded reactively
    val syncLogs = remember { mutableStateListOf<SyncLogEntry>() }
    var autoSyncEnabled by remember { mutableStateOf(prefs.autoSyncEnabled) }
    var showTestSyncDialog by remember { mutableStateOf(false) }

    // Prefill data for test alert
    var testSender by remember { mutableStateOf("AD-HDFCBK") }
    var testSmsText by remember { mutableStateOf("Alert: Your A/c x4918 has been Credited with Rs. 500.00 via UPI Ref 384918274019 on 06-06-2026.") }
    var isTestingSync by remember { mutableStateOf(false) }

    // Load logs on mount and subscribe to receiving sms events
    LaunchedEffect(Unit) {
        syncLogs.clear()
        syncLogs.addAll(prefs.getSyncLogs())

        SmsSyncNotifier.syncEvent.collect {
            syncLogs.clear()
            syncLogs.addAll(prefs.getSyncLogs())
        }
    }

    // Helper for initials
    val initials = remember(prefs.merchantName) {
        val name = prefs.merchantName.trim()
        if (name.isEmpty()) "OP"
        else {
            val parts = name.split(Regex("\\s+"))
            if (parts.size >= 2) {
                (parts[0].take(1) + parts[1].take(1)).uppercase(Locale.ROOT)
            } else {
                name.take(2).uppercase(Locale.ROOT)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().background(DeepBlackBg),
        bottomBar = {
            // Sleek Control Panel Footer
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Row 1: Auto-Sync control info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Auto-sync SMS Alerts",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (autoSyncEnabled) "Real-time forwarding active" else "SMS forwarding inactive in background",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Switch(
                            checked = autoSyncEnabled,
                            onCheckedChange = { isChecked ->
                                autoSyncEnabled = isChecked
                                prefs.autoSyncEnabled = isChecked
                                Toast.makeText(
                                    context,
                                    if (isChecked) "Auto-sync active!" else "Auto-sync disabled.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = Indigo,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = DeepBlackBg
                            ),
                            modifier = Modifier.testTag("auto_sync_toggle")
                        )
                    }

                    // Row 2: Unlink & Test Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Unlink device action
                        Button(
                            onClick = {
                                prefs.unlink()
                                onUnlink()
                                Toast.makeText(context, "Device unlinked successfully.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("unlink_device_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF27272A),
                                contentColor = Color(0xFFD4D4D8)
                            )
                        ) {
                            Text(
                                text = "UNLINK DEVICE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }

                        // Circular Simulated FAB built as square elegant action box to trigger manual mockups
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Indigo)
                                .clickable {
                                    showTestSyncDialog = true
                                }
                                .testTag("test_sync_fab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Manual Test Sync Tool",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding)
                .background(DeepBlackBg)
        ) {
            // Styled App Header in the style of the Sleek Interface
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Indigo),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Syncing system decoration",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "OpenPay",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "COMPANION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.8.sp
                        )
                    }
                }

                // Dynamic Avatar Initial Bubble
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F2937))
                        .border(1.dp, Color(0xFF374151), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Indigo
                    )
                }
            }

            // High Fidelity Merchant Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFF1F2937).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .background(CardBackground)
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Row 1: Merchant Details + Connected Status Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "MERCHANT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = prefs.merchantName.ifEmpty { "Umesh's Shop" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // High fidelity "Connected" badge with breathing dot
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(SuccessGreen.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen)
                            )
                            Text(
                                text = "CONNECTED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Row 2: Device ID or info + Custom layout for Code badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DEVICE ID",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${prefs.getDeviceName()} (${prefs.getDeviceId().take(8).uppercase(Locale.ROOT)})",
                                fontSize = 13.sp,
                                color = Color(0xFFD1D5DB),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Compact Code Badge Card
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1C1C1E))
                                .border(1.dp, Color(0xFF374151), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "CODE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = prefs.merchantCode,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Indigo,
                                letterSpacing = (-0.5).sp
                            )
                        }
                    }
                }
            }

            // Dynamic Sync Feed Section Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT SYNCS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.5.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1F2937))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Last 20",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (syncLogs.isNotEmpty()) {
                        Text(
                            text = "Clear",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary.copy(alpha = 0.8f),
                            modifier = Modifier
                                .clickable {
                                    prefs.clearLog()
                                    syncLogs.clear()
                                    Toast.makeText(context, "Log cleared successfully", Toast.LENGTH_SHORT).show()
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Sync Log Feed list or empty view
            if (syncLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF374151),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Waiting for payment alerts...",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Trigger a simulator alert using 'Test Sync' in the bottom control panel to verify delivery.",
                            fontSize = 12.sp,
                            color = Color(0xFF4B5563),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("sync_logs_list"),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(syncLogs) { log ->
                        SleekSyncLogItem(log = log)
                    }
                }
            }
        }
    }

    // Dialogue popup for simulation
    if (showTestSyncDialog) {
        AlertDialog(
            onDismissRequest = { if (!isTestingSync) showTestSyncDialog = false },
            title = {
                Text(
                    text = "Simulate UPI Credit SMS",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Inputs mock transaction fields to trigger a background routing test to Next.js OpenPay endpoints.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    OutlinedTextField(
                        value = testSender,
                        onValueChange = { testSender = it },
                        label = { Text("Sender Header") },
                        placeholder = { Text("e.g. AD-HDFCBK") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Indigo,
                            unfocusedBorderColor = Color(0xFF374151)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("test_sender_input")
                    )

                    OutlinedTextField(
                        value = testSmsText,
                        onValueChange = { testSmsText = it },
                        label = { Text("SMS Messages body") },
                        placeholder = { Text("Credit message details") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Indigo,
                            unfocusedBorderColor = Color(0xFF374151)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("test_body_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalSender = testSender.trim()
                        val finalBody = testSmsText.trim()
                        if (finalSender.isEmpty() || finalBody.isEmpty()) {
                            Toast.makeText(context, "Please complete fields.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        scope.launch {
                            isTestingSync = true
                            val result = simulateSmsSubmission(
                                context = context,
                                prefs = prefs,
                                sender = finalSender,
                                smsText = finalBody
                            )
                            isTestingSync = false
                            showTestSyncDialog = false

                            if (result) {
                                Toast.makeText(context, "Simulation successful ✓", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Log synchronized locally in missed status", Toast.LENGTH_LONG).show()
                            }
                            syncLogs.clear()
                            syncLogs.addAll(prefs.getSyncLogs())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo),
                    enabled = !isTestingSync,
                    modifier = Modifier.testTag("submit_test_sync")
                ) {
                    if (isTestingSync) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = TextPrimary)
                    } else {
                        Text("Simulate")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTestSyncDialog = false },
                    enabled = !isTestingSync
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun SleekSyncLogItem(log: SyncLogEntry) {
    val indicatorColor = when (log.status) {
        "success" -> SuccessGreen
        "failed" -> ErrorRed
        else -> Color(0xFF6B7280) // Grey for missed/sync paused
    }

    val stateIcon = when (log.status) {
        "success" -> Icons.Default.Check
        "failed" -> Icons.Default.Close
        else -> Icons.Default.Info
    }

    // Row containing border indication slice on left inside card bg
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // High-fidelity border slice indicators matching border-l-4 style
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .height(72.dp) // Maintain consistent height bounding
                .background(indicatorColor)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.sender,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = formatIsoTimestamp(log.timestamp),
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = log.smsPreview,
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Render dynamic reference uuid values if successful
                log.transactionId?.let { txId ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ref: ${txId.take(16)}...",
                        fontSize = 9.sp,
                        color = Indigo,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Round compact secondary status circle indicator
            Icon(
                imageVector = stateIcon,
                contentDescription = log.status,
                tint = indicatorColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatIsoTimestamp(iso: String): String {
    return try {
        val parts = iso.split("T")
        if (parts.size == 2) {
            val time = parts[1].substringBefore(".")
            val formattedTime = if (time.length >= 5) time.substring(0, 5) else time
            formattedTime
        } else {
            iso
        }
    } catch (e: Exception) {
        iso
    }
}

// Background simulator function execution
private suspend fun simulateSmsSubmission(
    context: android.content.Context,
    prefs: PrefsManager,
    sender: String,
    smsText: String
): Boolean {
    return withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val timestampIso = sdf.format(Date())

        if (!prefs.autoSyncEnabled) {
            val entry = SyncLogEntry(
                sender = sender,
                smsPreview = smsText,
                timestamp = timestampIso,
                status = "missed",
                transactionId = null
            )
            prefs.addSyncLog(entry)
            return@withContext false
        }

        try {
            val service = ApiClient.getService(prefs.baseUrl)
            val request = TransactionRequest(
                merchantCode = prefs.merchantCode,
                smsText = smsText,
                senderHeader = sender,
                timestamp = timestampIso,
                deviceId = prefs.getDeviceId(),
                deviceName = prefs.getDeviceName()
            )

            val response = service.forwardTransaction(
                url = "${prefs.baseUrl.trimEnd('/')}/api/transactions",
                request = request
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val txId = response.body()?.transactionId
                val entry = SyncLogEntry(
                    sender = sender,
                    smsPreview = smsText,
                    timestamp = timestampIso,
                    status = "success",
                    transactionId = txId
                )
                prefs.addSyncLog(entry)
                true
            } else {
                val entry = SyncLogEntry(
                    sender = sender,
                    smsPreview = smsText,
                    timestamp = timestampIso,
                    status = "failed",
                    transactionId = null
                )
                prefs.addSyncLog(entry)
                false
            }
        } catch (e: Exception) {
            val entry = SyncLogEntry(
                sender = sender,
                smsPreview = smsText,
                timestamp = timestampIso,
                status = "failed",
                transactionId = null
            )
            prefs.addSyncLog(entry)
            false
        }
    }
}
