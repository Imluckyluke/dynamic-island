package com.hadi.dynamicisland

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat

class IslandService : Service() {
    companion object {
        const val ACTION_START = "com.hadi.dynamicisland.action.START"
        const val ACTION_STOP = "com.hadi.dynamicisland.action.STOP"
        const val ACTION_MEDIA_COMMAND = "com.hadi.dynamicisland.action.MEDIA_COMMAND"
        const val ACTION_TIMER_START = "com.hadi.dynamicisland.action.TIMER_START"
        const val ACTION_TIMER_CANCEL = "com.hadi.dynamicisland.action.TIMER_CANCEL"
        const val ACTION_SHOW_TEST = "com.hadi.dynamicisland.action.SHOW_TEST"
        const val ACTION_DISMISS = "com.hadi.dynamicisland.action.DISMISS"
        const val EXTRA_MEDIA_COMMAND = "media_command"
        const val EXTRA_TIMER_MINUTES = "timer_minutes"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "island_service"
        private const val TRANSIENT_CHARGING_MS = 5000L
        private const val TRANSIENT_TEST_MS = 4000L

        fun start(context: Context) {
            val intent = Intent(context, IslandService::class.java).setAction(ACTION_START)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, IslandService::class.java).setAction(ACTION_STOP))
        }

        fun startTimer(context: Context, minutes: Int) {
            val intent = Intent(context, IslandService::class.java)
                .setAction(ACTION_TIMER_START)
                .putExtra(EXTRA_TIMER_MINUTES, minutes)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun cancelTimer(context: Context) {
            context.startService(
                Intent(context, IslandService::class.java).setAction(ACTION_TIMER_CANCEL)
            )
        }

        fun showTest(context: Context) {
            val intent = Intent(context, IslandService::class.java).setAction(ACTION_SHOW_TEST)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var overlay: IslandOverlay
    private var wasCharging: Boolean? = null
    private var timerEndElapsed = 0L
    private val timerTick = Runnable { updateTimer() }
    private val restoreDefault = Runnable { restoreAfterTransient() }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) updateCharging(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        IslandLog.installCrashHandler(this)
        IslandLog.log(this, "SERVICE", "Service created")
        overlay = IslandOverlay(this, getSystemService(WindowManager::class.java))
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(batteryReceiver, batteryFilter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(batteryReceiver, batteryFilter)
            }
        } catch (e: Exception) {
            IslandLog.log(this, "SERVICE", "Battery receiver registration failed", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        IslandLog.log(this, "SERVICE", "Command ${intent?.action}")
        if (intent?.action == ACTION_STOP) {
            overlay.hide()
            IslandPrefs.setServiceEnabled(this, false)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!ensureOverlay()) return START_NOT_STICKY
        when (intent?.action) {
            ACTION_MEDIA_COMMAND -> {
                val command = MediaMonitor.Command.values().getOrNull(
                    intent.getIntExtra(EXTRA_MEDIA_COMMAND, -1)
                )
                command?.let {
                    val handled = MediaMonitor.command(it)
                    IslandLog.log(this, "SERVICE", "Media command $it handled=$handled")
                }
            }
            ACTION_TIMER_START -> startTimer(intent.getIntExtra(EXTRA_TIMER_MINUTES, 0))
            ACTION_TIMER_CANCEL -> cancelTimer()
            ACTION_SHOW_TEST -> showTransient(
                IslandState.Content(
                    title = getString(R.string.test_title),
                    subtitle = getString(R.string.test_subtitle),
                    kind = IslandState.Kind.TEST
                ),
                TRANSIENT_TEST_MS
            )
            ACTION_DISMISS -> dismissIsland()
            else -> {
                IslandPrefs.setServiceEnabled(this, true)
            }
        }
        return START_STICKY
    }

    private fun ensureOverlay(): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            IslandLog.log(this, "SERVICE", "Overlay permission unavailable")
            Toast.makeText(this, R.string.msg_need_overlay, Toast.LENGTH_SHORT).show()
            stopSelf()
            return false
        }
        IslandPrefs.setServiceEnabled(this, true)
        return try {
            overlay.show()
            IslandLog.log(this, "SERVICE", "Overlay shown")
            true
        } catch (e: Exception) {
            IslandLog.log(this, "SERVICE", "Overlay failed", e)
            stopSelf()
            false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        IslandLog.log(this, "SERVICE", "Service destroyed")
        handler.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
        }
        if (::overlay.isInitialized) overlay.hide()
        super.onDestroy()
    }

    fun onMediaCommand(command: MediaMonitor.Command) {
        val intent = Intent(this, IslandService::class.java)
            .setAction(ACTION_MEDIA_COMMAND)
            .putExtra(EXTRA_MEDIA_COMMAND, command.ordinal)
        startService(intent)
    }

    fun cancelTimer() {
        IslandLog.log(this, "SERVICE", "Timer cancelled")
        timerEndElapsed = 0L
        handler.removeCallbacks(timerTick)
        restoreAfterTransient()
    }

    fun dismissIsland() {
        handler.removeCallbacks(restoreDefault)
        overlay.collapse()
        restoreAfterTransient()
    }

    private fun startAsForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = getString(R.string.notif_channel_description) }
            )
        }
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.presence_online)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_running))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateCharging(intent: Intent) {
        if (!IslandPrefs.showCharging(this)) {
            wasCharging = null
            return
        }
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).takeIf { it > 0 } ?: 100
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val was = wasCharging
        wasCharging = charging
        if (charging && was != true && level >= 0) {
            val percent = (level * 100 / scale).coerceIn(0, 100)
            showTransient(
                IslandState.Content(
                    title = getString(R.string.charging_connected, percent),
                    subtitle = "",
                    kind = IslandState.Kind.CHARGING
                ),
                TRANSIENT_CHARGING_MS
            )
        }
    }

    private fun startTimer(minutes: Int) {
        IslandLog.log(this, "SERVICE", "Timer requested minutes=$minutes")
        if (!IslandPrefs.showTimer(this) || minutes <= 0) return
        timerEndElapsed = SystemClock.elapsedRealtime() + minutes * 60_000L
        updateTimer()
    }

    private fun updateTimer() {
        if (timerEndElapsed <= 0L) return
        val remaining = ((timerEndElapsed - SystemClock.elapsedRealtime()) / 1000L).toInt()
        if (remaining <= 0) {
            timerEndElapsed = 0L
            showTransient(
                IslandState.Content(
                    title = getString(R.string.timer_done),
                    subtitle = "",
                    kind = IslandState.Kind.TIMER
                ),
                TRANSIENT_TEST_MS
            )
            return
        }
        IslandState.setContent(
            IslandState.Content(
                title = getString(R.string.timer_remaining, remaining / 60, remaining % 60),
                subtitle = "",
                kind = IslandState.Kind.TIMER,
                timerSeconds = remaining
            )
        )
        handler.removeCallbacks(timerTick)
        handler.postDelayed(timerTick, 1000L)
    }

    private fun showTransient(content: IslandState.Content, durationMs: Long) {
        handler.removeCallbacks(timerTick)
        handler.removeCallbacks(restoreDefault)
        IslandState.setContent(content)
        handler.postDelayed(restoreDefault, durationMs)
    }

    private fun restoreAfterTransient() {
        if (timerEndElapsed > SystemClock.elapsedRealtime()) {
            updateTimer()
            return
        }
        timerEndElapsed = 0L
        val controller = IslandState.activeMediaController
        val playing = controller?.playbackState?.state == PlaybackState.STATE_PLAYING
        if (controller != null && playing && IslandPrefs.showMedia(this)) {
            MediaMonitor.updateFromController(this, controller)
        } else {
            IslandState.setContent(IslandState.Content("", "", IslandState.Kind.IDLE))
        }
    }
}
