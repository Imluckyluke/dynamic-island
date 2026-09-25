package com.hadi.dynamicisland

import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView

class IslandOverlay(
    private val service: IslandService,
    private val windowManager: WindowManager
) {
    private var root: View? = null
    private var expanded = false
    private val stateListener = { update() }

    fun show() {
        if (root != null) {
            update()
            return
        }
        val view = LayoutInflater.from(service).inflate(R.layout.island_view, null)
        val density = service.resources.displayMetrics.density
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = statusBarHeight() + (8f * density).toInt()
        }
        view.findViewById<View>(R.id.islandPill).setOnClickListener { toggleExpanded() }
        view.findViewById<ImageButton>(R.id.btnMediaPrevious).setOnClickListener {
            service.onMediaCommand(MediaMonitor.Command.PREVIOUS)
        }
        view.findViewById<ImageButton>(R.id.btnMediaPlayPause).setOnClickListener {
            service.onMediaCommand(MediaMonitor.Command.TOGGLE)
        }
        view.findViewById<ImageButton>(R.id.btnMediaNext).setOnClickListener {
            service.onMediaCommand(MediaMonitor.Command.NEXT)
        }
        view.findViewById<View>(R.id.btnCancelIslandTimer).setOnClickListener {
            service.cancelTimer()
        }
        view.findViewById<View>(R.id.btnDismissIsland).setOnClickListener {
            service.dismissIsland()
        }
        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            IslandLog.log(service, "OVERLAY", "Overlay add failed", e)
            throw e
        }
        root = view
        IslandState.addListener(stateListener)
        update()
    }

    fun hide() {
        root?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                IslandLog.log(service, "OVERLAY", "Overlay remove failed", e)
            }
        }
        root = null
        IslandState.removeListener(stateListener)
    }

    fun toggleExpanded() {
        expanded = !expanded
        update()
    }

    fun collapse() {
        expanded = false
        update()
    }

    private fun statusBarHeight(): Int {
        val identifier = service.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (identifier > 0) service.resources.getDimensionPixelSize(identifier) else 0
    }

    private fun update() {
        val view = root ?: return
        if (Looper.myLooper() != Looper.getMainLooper()) {
            view.post { performUpdate() }
            return
        }
        performUpdate()
    }

    private fun performUpdate() {
        val view = root ?: return
        val content = IslandState.current()
        val pill = view.findViewById<LinearLayout>(R.id.islandPill)
        val expandedView = view.findViewById<LinearLayout>(R.id.islandExpanded)
        pill.visibility = if (expanded) View.GONE else View.VISIBLE
        expandedView.visibility = if (expanded) View.VISIBLE else View.GONE
        view.findViewById<TextView>(R.id.islandTitle).text =
            content.title.ifBlank { service.getString(R.string.island_default_title) }
        view.findViewById<TextView>(R.id.islandExpandedTitle).text =
            content.title.ifBlank { service.getString(R.string.island_default_title) }
        val subtitle = view.findViewById<TextView>(R.id.islandExpandedSubtitle)
        subtitle.text = content.subtitle
        subtitle.visibility = if (content.subtitle.isBlank()) View.GONE else View.VISIBLE
        val mediaControls = view.findViewById<View>(R.id.islandMediaControls)
        mediaControls.visibility = if (content.kind == IslandState.Kind.MEDIA) View.VISIBLE else View.GONE
        val timerControls = view.findViewById<View>(R.id.islandTimerControls)
        timerControls.visibility = if (content.kind == IslandState.Kind.TIMER) View.VISIBLE else View.GONE
        if (content.kind == IslandState.Kind.MEDIA) {
            val playPause = view.findViewById<ImageButton>(R.id.btnMediaPlayPause)
            playPause.setImageResource(
                if (content.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
            playPause.contentDescription = service.getString(
                if (content.isPlaying) R.string.media_pause else R.string.media_play
            )
        }
        if (content.kind == IslandState.Kind.TIMER) {
            val minutes = content.timerSeconds / 60
            val seconds = content.timerSeconds % 60
            view.findViewById<TextView>(R.id.islandTimerText).text =
                service.getString(R.string.timer_remaining, minutes, seconds)
        }
        tint(view, R.id.btnMediaPrevious)
        tint(view, R.id.btnMediaPlayPause)
        tint(view, R.id.btnMediaNext)
        tint(view, R.id.btnCancelIslandTimer)
        tint(view, R.id.btnDismissIsland)
    }

    private fun tint(view: View, id: Int) {
        view.findViewById<ImageButton>(id)?.setColorFilter(Color.WHITE)
    }
}
