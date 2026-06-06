package com.example.ui.pairing

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ApiClient
import com.example.data.PairRequest
import com.example.data.PrefsManager
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DeepBlackBg
import com.example.ui.theme.Indigo
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    prefs: PrefsManager,
    onPairSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var tempBaseUrl by remember { mutableStateOf(prefs.baseUrl) }

    Scaffold(
        modifier = modifier.fillMaxSize().background(DeepBlackBg),
        containerColor = DeepBlackBg,
        topBar = {
            TopAppBar(
                title = { Text("", maxLines = 1) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepBlackBg,
                    titleContentColor = TextPrimary
                ),
                actions = {
                    IconButton(
                        onClick = {
                            tempBaseUrl = prefs.baseUrl
                            showSettingsDialog = true
                        },
                        modifier = Modifier
                            .testTag("api_settings_button")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configure Backend Base URL",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App branding section in Sleek styling
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Indigo,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "OpenPay Logo",
                    tint = TextPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "OpenPay Companion",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Link this device to your OpenPay account to synchronously forward real-time business payment SMS alerts.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(44.dp))

            // Connection code text prompt
            Text(
                text = "ENTER 6-DIGIT SYNC CODE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Indigo,
                letterSpacing = 1.6.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Standard hidden TextField coupled with an elegant custom OTP Display grid
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Large numeric-input box grid matching CardBackground colors
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    for (i in 0 until 6) {
                        val char = code.getOrNull(i)?.toString() ?: ""
                        val isFocused = code.length == i

                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 54.dp)
                                .border(
                                    width = if (isFocused) 2.dp else 1.dp,
                                    color = if (isFocused) Indigo else Color(0xFF374151),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(CardBackground, shape = RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Underlying transparent standard OutlinedTextField to capture standard soft keyboard inputs securely
                TextField(
                    value = code,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        if (digits.length <= 6) {
                            code = digits
                            errorMessage = null
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .matchParentSize()
                        .testTag("otp_hidden_input")
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Error Message Display
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            // Link Device Button
            Button(
                onClick = {
                    if (code.length != 6) {
                        errorMessage = "Please enter all 6 digits."
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val service = ApiClient.getService(prefs.baseUrl)
                            val request = PairRequest(
                                merchantCode = code,
                                deviceId = prefs.getDeviceId(),
                                deviceName = prefs.getDeviceName()
                            )

                            val response = service.pairDevice(
                                url = "${prefs.baseUrl.trimEnd('/')}/api/devices/pair",
                                request = request
                            )

                            if (response.isSuccessful && response.body()?.success == true) {
                                val body = response.body()!!
                                val merchName = body.merchantName ?: "Umesh's Shop"
                                val merchId = body.merchantId ?: "uuid"

                                // Persist state keys securely in preferences
                                prefs.merchantCode = code
                                prefs.merchantName = merchName
                                prefs.merchantId = merchId
                                prefs.isPaired = true

                                Toast.makeText(context, "Linked to $merchName!", Toast.LENGTH_SHORT).show()
                                onPairSuccess()
                            } else {
                                val serverMsg = response.body()?.message ?: "Check your code or dashboard settings."
                                errorMessage = "Invalid code. $serverMsg"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Connection failed. Please check your network and Base URL configuration."
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("link_device_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Indigo,
                    contentColor = TextPrimary
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = TextPrimary
                    )
                } else {
                    Text(
                        text = "LINK DEVICE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Informational Target server
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1F2937).copy(alpha = 0.4f))
                    .clickable {
                        tempBaseUrl = prefs.baseUrl
                        showSettingsDialog = true
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF9CA3AF)))
                Text(
                    text = "Endpoint: ${prefs.baseUrl}",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // Settings dialogue
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text(
                    text = "Configure API Endpoint",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Modify the active OpenPay merchant portal endpoint address used for payment forwarding sync.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                    OutlinedTextField(
                        value = tempBaseUrl,
                        onValueChange = { tempBaseUrl = it },
                        label = { Text("Base Server URL") },
                        placeholder = { Text("https://example.com") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Indigo,
                            unfocusedBorderColor = Color(0xFF374151)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("baseUrl_setting_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val validatedUrl = tempBaseUrl.trim()
                        if (validatedUrl.startsWith("http://") || validatedUrl.startsWith("https://")) {
                            prefs.baseUrl = validatedUrl
                            showSettingsDialog = false
                            Toast.makeText(context, "API server updated successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "URL must start with http or https", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo),
                    modifier = Modifier.testTag("save_baseline_url")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSettingsDialog = false }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
