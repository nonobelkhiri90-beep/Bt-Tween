package com.carcam.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Res(val label: String, val quality: Quality) {
    UHD("4K", Quality.UHD), FHD("1080p", Quality.FHD), HD("720p", Quality.HD)
}

/**
 * يغلّف CameraX. الصوت الآن من الميكروفون (MVP).
 * لاحقاً: withAudioEnabled يُستبدل بمسار صوت من audio-engine ثم دمجه.
 */
class VideoController(private val ctx: Context, private val owner: LifecycleOwner) {
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    val isRecording get() = recording != null

    fun bind(view: PreviewView, front: Boolean, res: Res, onReady: () -> Unit) {
        val f = ProcessCameraProvider.getInstance(ctx)
        f.addListener({
            val provider = f.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(res.quality, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))
                ).build()
            val vc = VideoCapture.withOutput(recorder)
            val sel = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            provider.unbindAll()
            provider.bindToLifecycle(owner, sel, preview, vc)
            videoCapture = vc
            onReady()
        }, ContextCompat.getMainExecutor(ctx))
    }

    @SuppressLint("MissingPermission")
    fun start(onEvent: (VideoRecordEvent) -> Unit) {
        val vc = videoCapture ?: return
        if (recording != null) return
        val name = "CarCam_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val cv = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CarCam")
        }
        val opts = MediaStoreOutputOptions.Builder(ctx.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(cv).build()
        recording = vc.output.prepareRecording(ctx, opts)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(ctx)) { e ->
                if (e is VideoRecordEvent.Finalize) recording = null
                onEvent(e)
            }
    }

    fun stop() { recording?.stop(); recording = null }
}
