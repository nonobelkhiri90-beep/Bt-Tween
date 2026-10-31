package com.carcam.camera

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class CameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val auto = intent.getBooleanExtra(EXTRA_AUTO_START, false)
        setContent { MaterialTheme { Root(auto) } }
    }

    @Composable
    private fun Root(autoStart: Boolean) {
        val needed = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        var granted by remember { mutableStateOf(false) }
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { r -> granted = r.values.all { it } }
        LaunchedEffect(Unit) { launcher.launch(needed) }
        if (granted) CameraScreen(autoStart) else Box(Modifier.fillMaxSize().background(Color.Black))
    }

    @Composable
    private fun CameraScreen(autoStart: Boolean) {
        val ctx = LocalContext.current
        val owner = LocalLifecycleOwner.current
        val controller = remember { VideoController(ctx, owner) }
        val previewView = remember { PreviewView(ctx) }
        var front by remember { mutableStateOf(false) }
        var res by remember { mutableStateOf(Res.FHD) }
        var ready by remember { mutableStateOf(false) }
        var recording by remember { mutableStateOf(false) }
        var seconds by remember { mutableLongStateOf(0) }

        fun toggle() {
            if (recording) { controller.stop(); return }
            controller.start { e ->
                when (e) {
                    is VideoRecordEvent.Start -> recording = true
                    is VideoRecordEvent.Status -> seconds = e.recordingStats.recordedDurationNanos / 1_000_000_000
                    is VideoRecordEvent.Finalize -> {
                        recording = false; seconds = 0
                        val msg = if (e.hasError()) "فشل الحفظ (${e.error})" else "تم الحفظ في Movies/CarCam"
                        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }

        LaunchedEffect(front, res) {
            ready = false
            controller.bind(previewView, front, res) { ready = true }
        }
        var autoDone by remember { mutableStateOf(false) }
        LaunchedEffect(ready) {
            if (ready && autoStart && !autoDone) { autoDone = true; toggle() }
        }
        DisposableEffect(Unit) { onDispose { controller.stop() } }

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView({ previewView }, Modifier.fillMaxSize())

            if (recording) Text(
                "● %02d:%02d".format(seconds / 60, seconds % 60), color = Color.Red,
                modifier = Modifier.align(Alignment.TopCenter).padding(12.dp)
            )

            Column(
                Modifier.align(Alignment.CenterEnd).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Chip(res.label, enabled = !recording) {
                    res = Res.entries[(res.ordinal + 1) % Res.entries.size]
                }
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(Color.White).clickable(enabled = ready) { toggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.size(if (recording) 28.dp else 56.dp)
                            .clip(if (recording) RoundedCornerShape(4.dp) else CircleShape)
                            .background(Color.Red)
                    )
                }
                Chip("🔄", enabled = !recording) { front = !front }
            }
        }
    }

    @Composable
    private fun Chip(text: String, enabled: Boolean, onClick: () -> Unit) {
        Text(
            text, color = Color.White,
            modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0x99000000))
                .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }

    companion object { const val EXTRA_AUTO_START = "auto_start" }
}
