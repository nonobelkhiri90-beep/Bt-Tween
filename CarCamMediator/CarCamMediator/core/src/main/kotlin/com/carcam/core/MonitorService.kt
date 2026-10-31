package com.carcam.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class MonitorService : Service() {

    private lateinit var prefs: CarPrefs
    private lateinit var bt: BluetoothCarMonitor
    private lateinit var music: MusicStateMonitor
    private var wasReady = false

    override fun onCreate() {
        super.onCreate()
        prefs = CarPrefs(this)
        createChannels()
        ServiceCompat.startForeground(
            this, ONGOING_ID, ongoing(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )
        SessionStore.update { it.copy(monitoring = true) }

        bt = BluetoothCarMonitor(this, { prefs.carAddress }) { c ->
            SessionStore.update { it.copy(carConnected = c) }; evaluate()
        }
        music = MusicStateMonitor(this) { m ->
            SessionStore.update { it.copy(musicActive = m) }; evaluate()
        }
        bt.start(); music.start()
    }

    /** عند تحقق الشرطين لأول مرة نعرض إشعاراً بزر "ابدأ التسجيل" */
    private fun evaluate() {
        val s = SessionStore.state.value
        if (s.ready && !wasReady) offerRecording()
        if (!s.ready) nm().cancel(OFFER_ID)
        wasReady = s.ready
    }

    private fun offerRecording() {
        val open = Intent().setClassName(packageName, "com.carcam.camera.CameraActivity")
            .putExtra("auto_start", true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pi = PendingIntent.getActivity(
            this, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(this, CH_OFFER)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("السيارة متصلة والموسيقى تعمل")
            .setContentText("اضغط لبدء تسجيل الفيديو")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentIntent(pi)
            .addAction(android.R.drawable.ic_menu_camera, "ابدأ التسجيل", pi)
            .setAutoCancel(true)
            .build()
        nm().notify(OFFER_ID, n)
    }

    private fun ongoing(): Notification =
        NotificationCompat.Builder(this, CH_ONGOING)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("المراقبة تعمل")
            .setContentText("بانتظار اتصال السيارة وتشغيل الموسيقى")
            .setOngoing(true)
            .build()

    private fun createChannels() {
        nm().createNotificationChannel(
            NotificationChannel(CH_ONGOING, "المراقبة", NotificationManager.IMPORTANCE_LOW))
        nm().createNotificationChannel(
            NotificationChannel(CH_OFFER, "عرض التسجيل", NotificationManager.IMPORTANCE_HIGH))
    }

    private fun nm() = getSystemService(NotificationManager::class.java)

    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY
    override fun onBind(i: Intent?): IBinder? = null

    override fun onDestroy() {
        bt.stop(); music.stop()
        nm().cancel(OFFER_ID)
        SessionStore.update { CarSession() }
        super.onDestroy()
    }

    companion object {
        private const val CH_ONGOING = "ongoing"
        private const val CH_OFFER = "offer"
        private const val ONGOING_ID = 1
        private const val OFFER_ID = 2

        fun start(c: Context) = c.startForegroundService(Intent(c, MonitorService::class.java))
        fun stop(c: Context) = c.stopService(Intent(c, MonitorService::class.java))
    }
}
