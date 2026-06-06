package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.PrefsManager
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.pairing.PairingScreen
import com.example.ui.theme.CardBackground
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.Indigo
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val prefs = remember { PrefsManager(context) }
            
            // Dynamic pairing state tracker
            var isPairedState by remember { mutableStateOf(prefs.isPaired) }

            // Dynamic permission state tracker
            var hasSmsPermissions by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                )
            }

            // Launcher for requesting permissions
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val receiveSms = permissions[Manifest.permission.RECEIVE_SMS] ?: false
                val readSms = permissions[Manifest.permission.READ_SMS] ?: false
                hasSmsPermissions = receiveSms && readSms
            }

            // Trigger permission flow once immediately after first-time pairing
            LaunchedEffect(isPairedState) {
                if (isPairedState && !hasSmsPermissions) {
                    val reqList = mutableListOf(
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_SMS
                    )
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        reqList.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(reqList.toTypedArray())
                }
            }

            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AnimatedContent(
                        targetState = isPairedState,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "ScreenNavigation"
                    ) { paired ->
                        if (paired) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Informative Permission banner if revoked/denied
                                if (!hasSmsPermissions) {
                                    PermissionExplanationCard(
                                        onRequestPermission = {
                                            val reqList = mutableListOf(
                                                Manifest.permission.RECEIVE_SMS,
                                                Manifest.permission.READ_SMS
                                            )
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                                reqList.add(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                            permissionLauncher.launch(reqList.toTypedArray())
                                        }
                                    )
                                }

                                DashboardScreen(
                                    prefs = prefs,
                                    onUnlink = {
                                        isPairedState = false
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            PairingScreen(
                                prefs = prefs,
                                onPairSuccess = {
                                    isPairedState = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionExplanationCard(
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 44.dp, start = 16.dp, end = 16.dp, bottom = 4.dp)
            .testTag("permission_explanation_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = ErrorRed,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "SMS Permission Required",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "OpenPay Companion needs SMS interception rights to detect incoming bank UPI alerts (SBI, HDFC, Paytm, etc.) and sync them with your merchant console automatically. The app will not work without this.",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Indigo),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("grant_permission_button")
            ) {
                Text(
                    text = "Grant SMS Access",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
