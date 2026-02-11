package com.lowprioritycitizen.peppy.monitoring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class ForegroundMonitoringService : Service() {

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null

    // cooldown map simple in-memory to avoid repeated triggers (package -> lastTriggerMillis)
    private val lastTriggered = mutableMapOf<String, Long>()
    private val running = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.action == "STOP") {
            stopForeground(true)
            stopSelf()
            running.set(false)
            return START_NOT_STICKY
        }

        if (running.compareAndSet(false, true)) {
            startForeground(1, buildNotification())
            startPolling()
        }
        return START_STICKY
    }

    private fun startPolling() {
        job = coroutineScope.launch {
            val usage = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
            while (isActive) {
                val blacklist = loadBlacklist()
                try {
                    val now = System.currentTimeMillis()
                    val start = now - 1000L * 60
                    val stats = usage.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, now)
                    val top = stats.maxByOrNull { it.lastTimeUsed }?.packageName

                    if (!top.isNullOrEmpty() && blacklist.contains(top)) {
                        val last = lastTriggered[top] ?: 0L
                        val cooldownMs = 6_000L // 6 seconds cooldown (tweak)
                        if (now - last > cooldownMs) {
                            lastTriggered[top] = now
                            // Launch blocker
                            val blocker = Intent(this@ForegroundMonitoringService, com.lowprioritycitizen.peppy.ui.blocker.BlockerActivity::class.java)
                            (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP).also { blocker.flags = it }
                            blocker.putExtra("trigger_pkg", top)
                            startActivity(blocker)
                            // Log locally (implement persistent log as next step)
                            Log.d("MonitorService", "Triggered blocker for $top")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MonitorService", "poll error", e)
                }
                delay(1000L)
            }
        }
    }

    private fun loadBlacklist(): Set<String> {
        // TODO: read from SharedPreferences or Room DB
        // for now, test with hardcoded example:
        return setOf("com.instagram.android", "com.whatsapp")
    }

    override fun onDestroy() {
        job?.cancel()
        coroutineScope.cancel()
        running.set(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val ch = NotificationChannel(
            "peppy_monitor_channel",
            "Peppy monitoring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitoring running to detect distracting apps"
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {

        val stopIntent = Intent(this, ForegroundMonitoringService::class.java).apply {
            action = "STOP"
        }

        val stopPending = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "peppy_monitor_channel")
            .setContentTitle("Peppy monitoring")
            .setContentText("Monitoring distracting apps")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .addAction(0, "Stop", stopPending)
            .setOngoing(true)
            .build()
    }

}