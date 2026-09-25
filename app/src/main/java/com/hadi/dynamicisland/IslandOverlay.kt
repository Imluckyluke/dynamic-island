package com.hadi.dynamicisland

import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.animation.OvershootInterpolator
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView

class IslandOverlay(
    private val service: IslandService,
    private val windowManager: WindowManager
) {
    private var root: View? = null
    private var expanded = false
    private var lastDropKey: String? = null
    private val stateListener = { update() }

    fun show() {
        if (root != null) {
            recalibrate()
            return
        }
        val themedService = ContextThemeWrapper(service, R.style.Theme_DynamicIsland)
        val view = LayoutInflater.from(themedService).inflate(R.layout.island_view, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                blurBehindRadius = 48
            }
        }
        val pill = view.findViewById<View>(R.id.islandPill)
        pill.setOnClickListener { service.openContent() }
        pill.setOnLongClickListener {
            service.toggleIsland()
            true
        }
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
        view.findViewById<View>(R.id.islandExpanded).setOnClickListener {
            service.collapseIsland()
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
        view.post { recalibrate() }
    }

    fun recalibrate() {
        val view = root ?: return
        val geometry = geometry()
        val pill = view.findViewById<LinearLayout>(R.id.islandPill)
        val expandedView = view.findViewById<LinearLayout>(R.id.islandExpanded)
        val expandedWidth = maxOf((280f * service.resources.displayMetrics.density).toInt(), geometry.pillWidth)
        pill.layoutParams = FrameLayout.LayoutParams(
            geometry.pillWidth,
            geometry.pillHeight,
            Gravity.START or Gravity.TOP
        ).apply {
            leftMargin = geometry.centerX - geometry.pillWidth / 2
        }
        val density = service.resources.displayMetrics.density
        expandedView.layoutParams = FrameLayout.LayoutParams(
            expandedWidth,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.START or Gravity.TOP
        ).apply {
            leftMargin = geometry.centerX - expandedWidth / 2
            topMargin = (statusBarHeight() + (10f * density).toInt() - geometry.top).coerceAtLeast(0)
        }
        (view.layoutParams as? WindowManager.LayoutParams)?.let { params ->
            params.y = geometry.top
            try {
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {
                IslandLog.log(service, "OVERLAY", "Overlay geometry update failed", e)
            }
        }
        IslandLog.log(
            service,
            "OVERLAY",
            "Geometry center=${geometry.centerX} pill=${geometry.pillWidth}x${geometry.pillHeight} " +
                "top=${geometry.top} expanded=$expanded status=${statusBarHeight()}"
        )
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
        recalibrate()
    }

    fun setExpanded(value: Boolean) {
        if (expanded == value) {
            recalibrate()
            return
        }
        expanded = value
        recalibrate()
    }

    fun collapse() {
        expanded = false
        recalibrate()
    }

    fun collapseAnimated(onDone: () -> Unit = {}) {
        val view = root
        val expandedView = view?.findViewById<View>(R.id.islandExpanded)
        if (view == null || expandedView == null || !expanded) {
            collapse()
            onDone()
            return
        }
        val density = service.resources.displayMetrics.density
        expandedView.animate().cancel()
        expandedView.animate()
            .translationY(-24f * density)
            .alpha(0f)
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(220)
            .withEndAction {
                if (root == null) {
                    onDone()
                    return@withEndAction
                }
                expanded = false
                recalibrate()
                onDone()
            }
            .start()
    }

    private data class Geometry(
        val centerX: Int,
        val pillWidth: Int,
        val pillHeight: Int,
        val top: Int
    )

    private fun geometry(): Geometry {
        val density = service.resources.displayMetrics.density
        val screenWidth = screenWidthPx()
        if (IslandPrefs.autoPosition(service)) {
            val stored = IslandPrefs.autoGeometry(service)
            if (stored[1] > 0f && stored[2] > 0f) {
                return Geometry(
                    centerX = (stored[0] * density).toInt(),
                    pillWidth = ((stored[1] + 12f) * density).toInt().coerceAtLeast((24f * density).toInt()),
                    pillHeight = ((stored[2] + 10f) * density).toInt().coerceAtLeast((24f * density).toInt()),
                    top = ((stored[3] - 8f) * density).toInt().coerceAtLeast(0)
                )
            }
            val rect = root?.rootWindowInsets?.displayCutout?.boundingRects?.firstOrNull()
            if (rect != null) {
                val pillWidth = rect.width() + (12f * density).toInt()
                val pillHeight = rect.height() + (10f * density).toInt()
                return Geometry(
                    centerX = rect.centerX(),
                    pillWidth = pillWidth.coerceAtLeast((24f * density).toInt()),
                    pillHeight = pillHeight.coerceAtLeast((24f * density).toInt()),
                    top = (rect.top - 8f * density).toInt().coerceAtLeast(0)
                )
            }
        }
        val manualWidth = (IslandPrefs.pillWidthDp(service) * density).toInt()
        val manualHeight = (IslandPrefs.pillHeightDp(service) * density).toInt()
        return Geometry(
            centerX = screenWidth / 2 + (IslandPrefs.centerOffsetDp(service) * density).toInt(),
            pillWidth = manualWidth.coerceAtLeast((24f * density).toInt()),
            pillHeight = manualHeight.coerceAtLeast((24f * density).toInt()),
            top = (IslandPrefs.topOffsetDp(service) * density).toInt().coerceAtLeast(0)
        )
    }

    private fun statusBarHeight(): Int {
        val identifier = service.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (identifier > 0) service.resources.getDimensionPixelSize(identifier) else 0
    }

    @Suppress("DEPRECATION")
    private fun screenWidthPx(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.width()
        } else {
            service.resources.displayMetrics.widthPixels
        }
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
        val idle = content.kind == IslandState.Kind.IDLE
        pill.visibility = if (expanded) View.GONE else View.VISIBLE
        if (expanded) {
            expandedView.visibility = View.VISIBLE
            animateDrop(expandedView, content)
        } else {
            lastDropKey = null
            expandedView.animate().cancel()
            expandedView.alpha = 1f
            expandedView.translationY = 0f
            expandedView.scaleX = 1f
            expandedView.scaleY = 1f
            expandedView.visibility = View.GONE
        }
        val pillDot = view.findViewById<View>(R.id.islandDot)
        val pillTitle = view.findViewById<TextView>(R.id.islandTitle)
        pillDot.visibility = if (idle) View.GONE else View.VISIBLE
        pillTitle.visibility = if (idle) View.GONE else View.VISIBLE
        pillTitle.text = content.title
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

    private fun animateDrop(expandedView: View, content: IslandState.Content) {
        val dropKey = content.kind.name + "|" + content.key + "|" + content.title
        if (lastDropKey == dropKey) return
        lastDropKey = dropKey
        val density = service.resources.displayMetrics.density
        val fallDistance = if (expandedView.height > 0) {
            expandedView.height.toFloat()
        } else {
            120f * density
        }
        expandedView.animate().cancel()
        expandedView.alpha = 0f
        expandedView.translationY = -fallDistance
        expandedView.scaleX = 0.92f
        expandedView.scaleY = 0.92f
        expandedView.visibility = View.VISIBLE
        expandedView.post {
            if (root == null || !expanded) return@post
            expandedView.animate()
                .translationY(0f)
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(420)
                .setInterpolator(OvershootInterpolator(1.15f))
                .start()
        }
    }

    private fun tint(view: View, id: Int) {
        view.findViewById<ImageButton>(id)?.setColorFilter(Color.WHITE)
    }
}
