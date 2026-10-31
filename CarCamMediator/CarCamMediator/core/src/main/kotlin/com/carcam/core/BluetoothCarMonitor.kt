package com.carcam.core

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** يراقب اتصال/انفصال بلوتوث السيارة المحددة بالعنوان */
@SuppressLint("MissingPermission")
class BluetoothCarMonitor(
    private val context: Context,
    private val carAddress: () -> String?,
    private val onChange: (connected: Boolean) -> Unit,
) {
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val dev = if (Build.VERSION.SDK_INT >= 33)
                i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            else @Suppress("DEPRECATION") i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            if (dev?.address != carAddress()) return
            when (i.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> onChange(true)
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> onChange(false)
            }
        }
    }

    private fun hasPermission() = Build.VERSION.SDK_INT < 31 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    fun start() {
        val f = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        ContextCompat.registerReceiver(context, receiver, f, ContextCompat.RECEIVER_EXPORTED)
        checkAlreadyConnected()
    }

    /** إذا كانت السيارة متصلة أصلاً قبل تشغيل الخدمة */
    private fun checkAlreadyConnected() {
        if (!hasPermission()) return
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return
        adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                val connected = proxy.connectedDevices.any { it.address == carAddress() }
                onChange(connected)
                adapter.closeProfileProxy(profile, proxy)
            }
            override fun onServiceDisconnected(profile: Int) {}
        }, BluetoothProfile.A2DP)
    }

    fun stop() {
        runCatching { context.unregisterReceiver(receiver) }
    }
}
