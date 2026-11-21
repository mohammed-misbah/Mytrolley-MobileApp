package com.mytrolley.app

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mytrolley.app.ui.home.HomeScreen
import com.mytrolley.app.ui.theme.MH
import com.mytrolley.app.ui.theme.MarketHallSerif
import com.mytrolley.app.ui.theme.MyTrolleyTheme

private const val ACCESS_LOCAL_NETWORK_PERMISSION =
    "android.permission.ACCESS_LOCAL_NETWORK"

class MainActivity : ComponentActivity() {
    private var requiresLocalNetworkPermission = false
    private val hasLocalNetworkPermission = mutableStateOf(false)
    private val permissionWasDenied = mutableStateOf(false)

    private val localNetworkPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocalNetworkPermission.value = granted
        permissionWasDenied.value = !granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requiresLocalNetworkPermission =
            BuildConfig.DEBUG &&
                    Build.VERSION.SDK_INT >= 37 &&
                    isLocalDevelopmentApi()

        hasLocalNetworkPermission.value =
            !requiresLocalNetworkPermission || hasLocalNetworkPermission()

        setContent {
            MyTrolleyTheme {
                if (hasLocalNetworkPermission.value) {
                    HomeScreen()
                } else {
                    LocalNetworkPermissionScreen(
                        permissionWasDenied = permissionWasDenied.value,
                        onRequestPermission = {
                            localNetworkPermissionLauncher.launch(
                                ACCESS_LOCAL_NETWORK_PERMISSION
                            )
                        },
                        onOpenSettings = {
                            startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", packageName, null)
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (requiresLocalNetworkPermission) {
            val granted = hasLocalNetworkPermission()
            hasLocalNetworkPermission.value = granted
            if (granted) permissionWasDenied.value = false
        }
    }

    private fun hasLocalNetworkPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            ACCESS_LOCAL_NETWORK_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED

    private fun isLocalDevelopmentApi(): Boolean =
        isLocalNetworkHost(Uri.parse(BuildConfig.API_BASE_URL).host)

    private fun isLocalNetworkHost(host: String?): Boolean {
        val normalizedHost = host?.lowercase() ?: return false

        if (
            normalizedHost == "localhost" ||
            normalizedHost == "127.0.0.1" ||
            normalizedHost == "10.0.2.2" ||
            normalizedHost.endsWith(".local") ||
            normalizedHost.startsWith("10.") ||
            normalizedHost.startsWith("192.168.")
        ) {
            return true
        }

        val addressParts = normalizedHost.split(".")
        val secondOctet = addressParts.getOrNull(1)?.toIntOrNull()

        return addressParts.size == 4 &&
                addressParts[0] == "172" &&
                secondOctet?.let { it in 16..31 } == true
    }
}

@Composable
private fun LocalNetworkPermissionScreen(
    permissionWasDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MH.Cream)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Allow local network access",
            style = TextStyle(
                color = MH.Forest,
                fontFamily = MarketHallSerif,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "MyTrolley needs access to connect to its development catalogue server " +
                    "and load live stores, categories, and products.",
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MH.PrimaryButton,
                contentColor = MH.OnPrimaryButton,
                disabledContainerColor = MH.PrimaryButtonDisabled,
                disabledContentColor = MH.OnPrimaryButtonDisabled
            )
        ) {
            Text(if (permissionWasDenied) "Try again" else "Continue")
        }

        if (permissionWasDenied) {
            OutlinedButton(onClick = onOpenSettings) {
                Text("Open app settings", color = MH.Forest)
            }
        }
    }
}

