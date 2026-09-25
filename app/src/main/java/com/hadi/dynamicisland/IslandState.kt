package com.hadi.dynamicisland

import android.media.session.MediaController
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object IslandState {
    enum class Kind {
        IDLE,
        NOTIFICATION,
        MEDIA,
        CHARGING,
        TIMER,
        TEST
    }

    data class Content(
        val title: String,
        val subtitle: String,
        val kind: Kind,
        val isPlaying: Boolean = false,
        val timerSeconds: Int = 0,
        val key: String = "",
        val packageName: String = ""
    )

    @Volatile
    private var content = Content("", "", Kind.IDLE)

    @Volatile
    var activeMediaController: MediaController? = null

    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val transientListeners = CopyOnWriteArrayList<(Content, Long) -> Unit>()
    private val shownKeys = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun current(): Content = content

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun setContent(next: Content) {
        content = next
        for (listener in listeners) listener()
    }

    fun addTransientListener(listener: (Content, Long) -> Unit) {
        transientListeners.add(listener)
    }

    fun removeTransientListener(listener: (Content, Long) -> Unit) {
        transientListeners.remove(listener)
    }

    fun requestTransient(next: Content, durationMs: Long): Boolean {
        if (transientListeners.isEmpty()) {
            setContent(next)
            return false
        }
        for (listener in transientListeners) listener(next, durationMs)
        return true
    }

    fun markShown(key: String): Boolean = shownKeys.add(key)

    fun isShown(key: String): Boolean = shownKeys.contains(key)

    fun clearShown(key: String) {
        shownKeys.remove(key)
    }
}
