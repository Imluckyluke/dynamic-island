package com.hadi.dynamicisland

import android.app.Notification
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class IslandNotificationListener : NotificationListenerService() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onListenerConnected() {
        MediaMonitor.refresh(this)
    }

    override fun onListenerDisconnected() {
        MediaMonitor.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val posted = sbn ?: return
        val notification = posted.notification ?: return
        if (posted.packageName == packageName) return
        if (isMediaNotification(notification)) {
            MediaMonitor.refresh(this)
            return
        }
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        IslandState.markShown(posted.key)
        IslandState.setContent(
            IslandState.Content(
                title = title.ifBlank { text },
                subtitle = if (title.isBlank()) "" else text,
                kind = IslandState.Kind.NOTIFICATION
            )
        )
        if (shouldSuppress(notification)) {
            handler.post {
                try {
                    cancelNotification(posted.key)
                } catch (e: Exception) {
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.let { IslandState.clearShown(it.key) }
    }

    private fun isMediaNotification(notification: Notification): Boolean {
        if (notification.category == Notification.CATEGORY_TRANSPORT) return true
        val template = notification.extras?.getString(Notification.EXTRA_TEMPLATE).orEmpty()
        return template.endsWith("MediaStyle")
    }

    private fun shouldSuppress(notification: Notification): Boolean {
        if (!IslandPrefs.suppressDuplicates(this)) return false
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) {
            return !IslandPrefs.keepPriority(this)
        }
        return when (notification.category) {
            Notification.CATEGORY_CALL,
            Notification.CATEGORY_ALARM,
            Notification.CATEGORY_REMINDER -> !IslandPrefs.keepPriority(this)
            Notification.CATEGORY_SERVICE,
            Notification.CATEGORY_TRANSPORT,
            Notification.CATEGORY_SYSTEM -> false
            else -> true
        }
    }
}
