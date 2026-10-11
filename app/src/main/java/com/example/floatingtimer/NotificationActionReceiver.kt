package com.example.floatingtimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == FloatingTimerService.ACTION_STOP_ALARM) {
            val serviceIntent = Intent(context, FloatingTimerService::class.java).apply {
                action = FloatingTimerService.ACTION_STOP_ALARM
            }
            context?.startService(serviceIntent)
        }
    }
}
