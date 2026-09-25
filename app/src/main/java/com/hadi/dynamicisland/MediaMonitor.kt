package com.hadi.dynamicisland

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState

object MediaMonitor {
    enum class Command {
        TOGGLE,
        NEXT,
        PREVIOUS
    }

    private var controller: MediaController? = null
    private var callback: MediaController.Callback? = null

    fun clear() {
        detach()
    }

    fun refresh(listener: IslandNotificationListener) {
        if (!IslandPrefs.showMedia(listener)) {
            detach()
            return
        }
        val manager = listener.getSystemService(MediaSessionManager::class.java) ?: return
        val component = ComponentName(listener, IslandNotificationListener::class.java)
        val sessions = try {
            manager.getActiveSessions(component)
        } catch (e: SecurityException) {
            return
        }
        val active = sessions.firstOrNull()
        if (active == null || active.sessionToken != controller?.sessionToken) {
            attach(listener, active)
        } else {
            updateFromController(listener, active)
        }
    }

    fun command(command: Command): Boolean {
        val controls = IslandState.activeMediaController?.transportControls ?: return false
        return try {
            when (command) {
                Command.TOGGLE -> {
                    val playing = IslandState.activeMediaController
                        ?.playbackState?.state == PlaybackState.STATE_PLAYING
                    if (playing) controls.pause() else controls.play()
                }
                Command.NEXT -> controls.skipToNext()
                Command.PREVIOUS -> controls.skipToPrevious()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun attach(context: Context, next: MediaController?) {
        controller?.let { current ->
            callback?.let { current.unregisterCallback(it) }
        }
        controller = null
        callback = null
        IslandState.activeMediaController = null
        if (next == null) {
            if (IslandState.current().kind == IslandState.Kind.MEDIA) {
                IslandState.setContent(IslandState.Content("", "", IslandState.Kind.IDLE))
            }
            return
        }
        val listenerCallback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                updateFromController(context, next)
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                updateFromController(context, next)
            }

            override fun onSessionDestroyed() {
                detach()
            }
        }
        next.registerCallback(listenerCallback)
        controller = next
        callback = listenerCallback
        IslandState.activeMediaController = next
        updateFromController(context, next)
    }

    private fun detach() {
        controller?.let { current ->
            callback?.let { current.unregisterCallback(it) }
        }
        controller = null
        callback = null
        IslandState.activeMediaController = null
        if (IslandState.current().kind == IslandState.Kind.MEDIA) {
            IslandState.setContent(IslandState.Content("", "", IslandState.Kind.IDLE))
        }
    }

    fun updateFromController(context: Context, mediaController: MediaController) {
        if (!IslandPrefs.showMedia(context)) return
        val metadata = mediaController.metadata
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.media_unknown_title)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.media_unknown_artist)
        val playing = mediaController.playbackState?.state == PlaybackState.STATE_PLAYING
        IslandState.setContent(
            IslandState.Content(
                title = title,
                subtitle = artist,
                kind = IslandState.Kind.MEDIA,
                isPlaying = playing
            )
        )
    }
}
