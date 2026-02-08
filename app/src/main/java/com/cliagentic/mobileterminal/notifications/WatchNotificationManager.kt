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
        val watchChannel = NotificationChannel(
            CHANNEL_ID,
            "Session Watch Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Local notifications for terminal output watch rules"
        }
        manager.createNotificationChannel(watchChannel)

        val sessionStatusChannel = NotificationChannel(
            SESSION_STATUS_CHANNEL_ID,
            "Session Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent status for active SSH sessions"
            setShowBadge(false)
        }
        manager.createNotificationChannel(sessionStatusChannel)
    }

    fun notifyWatchMatch(sessionLabel: String, match: WatchMatch) {
        if (!canPostNotifications()) return

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

    fun notifySessionActive(sessionLabel: String) {
        if (!canPostNotifications()) return

        val notification = NotificationCompat.Builder(appContext, SESSION_STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Session active: $sessionLabel")
            .setContentText("SSH connected. Keepalive is enabled.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        NotificationManagerCompat.from(appContext).notify(SESSION_STATUS_NOTIFICATION_ID, notification)
    }

    fun clearSessionActive() {
        NotificationManagerCompat.from(appContext).cancel(SESSION_STATUS_NOTIFICATION_ID)
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val CHANNEL_ID = "watch_rule_alerts"
        const val SESSION_STATUS_CHANNEL_ID = "session_status"
        const val SESSION_STATUS_NOTIFICATION_ID = 1_001
    }
}
