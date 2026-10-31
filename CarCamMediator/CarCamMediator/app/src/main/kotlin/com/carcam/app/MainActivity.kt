package com.carcam.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import com.carcam.camera.CameraActivity
import com.carcam.core.CarPrefs
import com.carcam.core.MonitorService
import com.carcam.core.SessionStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Screen() } }
    }

    @SuppressLint("MissingPermission")
    @Composable
    private fun Screen() {
        val prefs = remember { CarPrefs(this) }
        val session by SessionStore.state.collectAsStateWithLifecycle()
        var carName by remember { mutableStateOf(prefs.carName) }
        var devices by remember { mutableStateOf(listOf<Pair<String, String>>()) }

        fun loadDevices() {
            val bm = getSystemService(BluetoothManager::class.java)
            devices = bm.adapter?.bondedDevices.orEmpty().map { (it.name ?: "؟") to it.address }
        }

        val permLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { loadDevices() }

        LaunchedEffect(Unit) {
            val perms = buildList {
                if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_CONNECT)
                if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (perms.isEmpty()) loadDevices() else permLauncher.launch(perms.toTypedArray())
        }

        Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
            Text("CarCam Mediator", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text("السيارة: ${carName ?: "لم تُحدد"}")
            Text("البلوتوث: ${if (session.carConnected) "متصل ✅" else "غير متصل"}")
            Text("الموسيقى: ${if (session.musicActive) "تعمل 🎵" else "متوقفة"}")
            Text("جاهز للتسجيل: ${if (session.ready) "نعم" else "لا"}")
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    if (session.monitoring) MonitorService.stop(this@MainActivity)
                    else MonitorService.start(this@MainActivity)
                },
                enabled = carName != null,
            ) { Text(if (session.monitoring) "إيقاف المراقبة" else "بدء المراقبة") }
            Button(onClick = { startActivity(Intent(this@MainActivity, CameraActivity::class.java)) }) {
                Text("فتح الكاميرا")
            }
            Spacer(Modifier.height(16.dp))
            Text("اختر بلوتوث سيارتك:", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                items(devices) { (name, addr) ->
                    Text(
                        name,
                        Modifier.fillMaxWidth().clickable {
                            prefs.carAddress = addr; prefs.carName = name; carName = name
                        }.padding(vertical = 12.dp)
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
