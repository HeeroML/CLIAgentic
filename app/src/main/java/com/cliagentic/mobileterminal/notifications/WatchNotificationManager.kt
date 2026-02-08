package com.cliagentic.mobileterminal.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cliagentic.mobileterminal.R
import com.cliagentic.mobileterminal.data.model.WatchMatch
import kotlin.random.Random

class WatchNotificationManager(context: Context) {

    private val appContext = context.applicationContext

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = appContext.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Session Watch Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Local notifications for terminal output watch rules"
        }
        manager.createNotificationChannel(channel)
    }

    fun notifyWatchMatch(sessionLabel: String, match: WatchMatch) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Task update: $sessionLabel")
            .setContentText(match.snippet)
            .setStyle(NotificationCompat.BigTextStyle().bigText(match.snippet))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(appContext).notify(Random.nextInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "watch_rule_alerts"
    }
}
