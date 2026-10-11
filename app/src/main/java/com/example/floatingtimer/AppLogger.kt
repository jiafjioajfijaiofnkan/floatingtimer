package com.example.floatingtimer

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object AppLogger {
    private const val TAG = "FloatingTimerApp"
    private val logBuffer = StringBuilder()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private var logFile: File? = null

    fun init(context: Context) {
        try {
            logFile = File(context.filesDir, "app_logs.txt")
            log("System", "AppLogger Initialized")

            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                e("Crash", "Uncaught Exception in thread ${thread.name}", throwable)
                defaultHandler?.uncaughtException(thread, throwable)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun log(tag: String, message: String) {
        val time = dateFormat.format(Date())
        val entry = "[$time][$tag] $message\n"
        Log.d(TAG, entry)
        logBuffer.append(entry)
        appendToFile(entry)
    }

    @Synchronized
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val time = dateFormat.format(Date())
        val stackTrace = throwable?.let { Log.getStackTraceString(it) } ?: ""
        val entry = "[$time][$tag][ERROR] $message\n$stackTrace\n"
        Log.e(TAG, entry)
        logBuffer.append(entry)
        appendToFile(entry)
    }

    private fun appendToFile(entry: String) {
        try {
            logFile?.appendText(entry)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun getLogs(): String {
        return try {
            if (logFile?.exists() == true) {
                logFile?.readText() ?: logBuffer.toString()
            } else {
                logBuffer.toString()
            }
        } catch (e: Exception) {
            logBuffer.toString()
        }
    }

    @Synchronized
    fun clearLogs() {
        logBuffer.clear()
        try {
            logFile?.writeText("")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
