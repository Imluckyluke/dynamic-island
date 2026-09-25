package com.hadi.dynamicisland

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 1001
    }

    private lateinit var tvStatus: TextView
    private lateinit var statusDot: View
    private lateinit var swSuppress: SwitchMaterial
    private lateinit var swKeepPriority: SwitchMaterial
    private lateinit var swMedia: SwitchMaterial
    private lateinit var swCharging: SwitchMaterial
    private lateinit var swTimer: SwitchMaterial
    private lateinit var swAutoPosition: SwitchMaterial
    private lateinit var sliderCenterOffset: Slider
    private lateinit var sliderTopOffset: Slider
    private lateinit var sliderPillWidth: Slider
    private lateinit var sliderPillHeight: Slider

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        IslandLog.installCrashHandler(this)
        IslandLog.log(this, "MAIN", "MainActivity created")
        setContentView(R.layout.activity_main)
        tvStatus = findViewById(R.id.tvStatus)
        statusDot = findViewById(R.id.statusDot)
        swSuppress = findViewById(R.id.swSuppressDuplicates)
        swKeepPriority = findViewById(R.id.swKeepPriority)
        swMedia = findViewById(R.id.swMedia)
        swCharging = findViewById(R.id.swCharging)
        swTimer = findViewById(R.id.swTimer)
        swAutoPosition = findViewById(R.id.swAutoPosition)
        sliderCenterOffset = findViewById(R.id.sliderCenterOffset)
        sliderTopOffset = findViewById(R.id.sliderTopOffset)
        sliderPillWidth = findViewById(R.id.sliderPillWidth)
        sliderPillHeight = findViewById(R.id.sliderPillHeight)

        swSuppress.isChecked = IslandPrefs.suppressDuplicates(this)
        swKeepPriority.isChecked = IslandPrefs.keepPriority(this)
        swMedia.isChecked = IslandPrefs.showMedia(this)
        swCharging.isChecked = IslandPrefs.showCharging(this)
        swTimer.isChecked = IslandPrefs.showTimer(this)
        swSuppress.setOnCheckedChangeListener { _, checked ->
            IslandPrefs.setSuppressDuplicates(this, checked)
        }
        swKeepPriority.setOnCheckedChangeListener { _, checked ->
            IslandPrefs.setKeepPriority(this, checked)
        }
        swMedia.setOnCheckedChangeListener { _, checked ->
            IslandPrefs.setShowMedia(this, checked)
        }
        swCharging.setOnCheckedChangeListener { _, checked ->
            IslandPrefs.setShowCharging(this, checked)
        }
        swTimer.setOnCheckedChangeListener { _, checked ->
            IslandPrefs.setShowTimer(this, checked)
        }
        swAutoPosition.isChecked = IslandPrefs.autoPosition(this)
        sliderCenterOffset.value = IslandPrefs.centerOffsetDp(this)
        sliderTopOffset.value = IslandPrefs.topOffsetDp(this)
        sliderPillWidth.value = IslandPrefs.pillWidthDp(this)
        sliderPillHeight.value = IslandPrefs.pillHeightDp(this)
        swAutoPosition.setOnCheckedChangeListener { _, checked ->
            IslandPrefs.setAutoPosition(this, checked)
            refreshGeometryEditors()
        }
        sliderCenterOffset.addOnChangeListener { _, value, fromUser ->
            if (fromUser) IslandPrefs.setCenterOffsetDp(this, value)
        }
        sliderTopOffset.addOnChangeListener { _, value, fromUser ->
            if (fromUser) IslandPrefs.setTopOffsetDp(this, value)
        }
        sliderPillWidth.addOnChangeListener { _, value, fromUser ->
            if (fromUser) IslandPrefs.setPillWidthDp(this, value)
        }
        sliderPillHeight.addOnChangeListener { _, value, fromUser ->
            if (fromUser) IslandPrefs.setPillHeightDp(this, value)
        }
        refreshGeometryEditors()

        findViewById<MaterialButton>(R.id.btnOverlayPermission).setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
        findViewById<MaterialButton>(R.id.btnNotificationAccess).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        findViewById<MaterialButton>(R.id.btnBatteryOptimization).setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        }
        findViewById<MaterialButton>(R.id.btnPostNotifications).setOnClickListener {
            requestPostNotifications()
        }
        findViewById<MaterialButton>(R.id.btnTimer1).setOnClickListener {
            IslandService.startTimer(this, 1)
        }
        findViewById<MaterialButton>(R.id.btnTimer5).setOnClickListener {
            IslandService.startTimer(this, 5)
        }
        findViewById<MaterialButton>(R.id.btnTimer10).setOnClickListener {
            IslandService.startTimer(this, 10)
        }
        findViewById<MaterialButton>(R.id.btnCancelTimer).setOnClickListener {
            IslandService.cancelTimer(this)
        }
        findViewById<MaterialButton>(R.id.btnTestIsland).setOnClickListener {
            IslandLog.log(this, "MAIN", "Test requested")
            IslandService.showTest(this)
        }
        findViewById<MaterialButton>(R.id.btnShareLogs).setOnClickListener {
            shareLogs()
        }
        findViewById<MaterialButton>(R.id.btnStartService).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.msg_need_overlay, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            IslandService.start(this)
            IslandLog.log(this, "MAIN", "Service start requested")
            Toast.makeText(this, R.string.msg_service_started, Toast.LENGTH_SHORT).show()
            refreshStatus()
        }
        findViewById<MaterialButton>(R.id.btnStopService).setOnClickListener {
            IslandService.stop(this)
            IslandLog.log(this, "MAIN", "Service stop requested")
            Toast.makeText(this, R.string.msg_service_stopped, Toast.LENGTH_SHORT).show()
            refreshStatus()
        }
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE) refreshStatus()
    }

    private fun shareLogs() {
        val source = IslandLog.currentFile(this)
        if (!source.exists() || source.length() == 0L) {
            Toast.makeText(this, R.string.msg_no_logs, Toast.LENGTH_SHORT).show()
            return
        }
        val directory = java.io.File(cacheDir, "shared-logs")
        if (!directory.exists()) directory.mkdirs()
        val file = java.io.File(directory, "island-log.txt")
        try {
            source.copyTo(file, overwrite = true)
        } catch (e: Exception) {
            IslandLog.log(this, "MAIN", "Log copy failed", e)
            Toast.makeText(this, R.string.msg_no_logs, Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val share = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(share, getString(R.string.share_logs_title)))
        IslandLog.log(this, "MAIN", "Logs shared")
    }

    private fun refreshGeometryEditors() {
        val manual = !IslandPrefs.autoPosition(this)
        sliderCenterOffset.isEnabled = manual
        sliderTopOffset.isEnabled = manual
        sliderPillWidth.isEnabled = manual
        sliderPillHeight.isEnabled = manual
    }

    private fun refreshStatus() {
        val overlay = Settings.canDrawOverlays(this)
        val listener = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        val battery = getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(packageName) == true
        val notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        val ready = overlay && listener && battery && notifications
        tvStatus.text = getString(
            if (ready) R.string.status_ready else R.string.status_missing
        )
        statusDot.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                this,
                if (ready) android.R.color.holo_green_dark else android.R.color.holo_red_dark
            )
        )
        updatePermissionButton(R.id.btnOverlayPermission, overlay)
        updatePermissionButton(R.id.btnNotificationAccess, listener)
        updatePermissionButton(R.id.btnBatteryOptimization, battery)
        updatePermissionButton(R.id.btnPostNotifications, notifications)
    }

    private fun updatePermissionButton(id: Int, granted: Boolean) {
        findViewById<MaterialButton>(id).apply {
            text = getString(if (granted) R.string.status_granted else R.string.btn_grant)
            isEnabled = !granted
        }
    }

    private fun requestPostNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            refreshStatus()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            refreshStatus()
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_CODE
        )
    }
}
