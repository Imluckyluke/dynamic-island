package com.hadi.dynamicisland

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object IslandLog {
    private const val LOG_DIR = "logs"
    private const val LOG_NAME = "island.log"
    private const val MAX_BYTES = 256 * 1024L
    private const val MAX_FILES = 3
    private val lock = Any()
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun installCrashHandler(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val appContext = context.applicationContext
                writeHeaderIfNeeded(appContext)
                append(appContext, "CRASH", "Uncaught exception in ${thread.name}", throwable)
            } catch (e: Exception) {
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun log(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        try {
            val appContext = context.applicationContext
            writeHeaderIfNeeded(appContext)
            append(appContext, tag, message, throwable)
        } catch (e: Exception) {
        }
    }

    fun currentFile(context: Context): File {
        val directory = File(context.applicationContext.filesDir, LOG_DIR)
        if (!directory.exists()) directory.mkdirs()
        return File(directory, LOG_NAME)
    }

    private fun writeHeaderIfNeeded(context: Context) {
        val file = currentFile(context)
        if (file.exists() && file.length() > 0L) return
        val packageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (e: Exception) {
            null
        }
        val version = packageInfo?.versionName ?: "unknown"
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode?.toString()
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toString()
        } ?: "unknown"
        append(
            context,
            "STARTUP",
            "package=${context.packageName} version=$version code=$code " +
                "sdk=${Build.VERSION.SDK_INT} release=${Build.VERSION.RELEASE} " +
                "device=${Build.MANUFACTURER} ${Build.MODEL}",
            null
        )
    }

    private fun append(context: Context, tag: String, message: String, throwable: Throwable?) {
        synchronized(lock) {
            rotateIfNeeded(context)
            val file = currentFile(context)
            FileWriter(file, true).use { writer ->
                writer.append(timestampFormat.format(Date()))
                    .append(' ')
                    .append(tag)
                    .append(' ')
                    .append(message.replace('\n', ' '))
                    .append('\n')
                if (throwable != null) {
                    writer.append(throwable.stackTraceToString()).append('\n')
                }
                writer.flush()
            }
        }
    }

    private fun rotateIfNeeded(context: Context) {
        val file = currentFile(context)
        if (!file.exists() || file.length() < MAX_BYTES) return
        for (index in MAX_FILES - 1 downTo 1) {
            val older = File(file.parent, "$LOG_NAME.$index")
            val newer = File(file.parent, "$LOG_NAME.${index + 1}")
            if (older.exists()) older.renameTo(newer)
        }
        file.renameTo(File(file.parent, "$LOG_NAME.1"))
    }
}
