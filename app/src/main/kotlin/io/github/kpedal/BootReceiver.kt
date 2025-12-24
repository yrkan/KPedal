package io.github.kpedal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * BroadcastReceiver to start KPedalExtension service when device boots.
 * This ensures pedaling metrics are collected for all rides, even without
 * KPedal data fields on the ride screen.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            android.util.Log.i(TAG, "Boot completed - starting KPedal service")

            val serviceIntent = Intent(context, KPedalExtension::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                android.util.Log.i(TAG, "KPedal service started successfully")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to start KPedal service: ${e.message}")
            }
        }
    }
}
