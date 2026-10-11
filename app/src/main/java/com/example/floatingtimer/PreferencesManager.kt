package com.example.floatingtimer

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("floating_timer_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_THEME_MODE = "theme_mode" // 0: Dark, 1: Light, 2: System
        const val KEY_SELECTED_STEP = "selected_step"
        const val KEY_CUSTOM_STEPS = "custom_steps"
        const val KEY_VIBRATE_ENABLE = "vibrate_enable"
        const val KEY_VIBRATE_DURATION = "vibrate_duration" // ms
        const val KEY_RINGTONE_ENABLE = "ringtone_enable"
        const val KEY_BEEP_DURATION = "beep_duration" // ms
        const val KEY_BEEP_VOLUME = "beep_volume" // 1 - 100
        const val KEY_ALLOW_NOTIF_STOP = "allow_notif_stop"
    }

    // 主题模式: 默认 0 (Night/Dark)
    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, 0)
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()

    var selectedStep: Int
        get() = prefs.getInt(KEY_SELECTED_STEP, 15)
        set(value) = prefs.edit().putInt(KEY_SELECTED_STEP, value).apply()

    // 存储逗号分隔的加时额度列表，默认 "5,10,15,20,30,60"
    var stepList: List<Int>
        get() {
            val str = prefs.getString(KEY_CUSTOM_STEPS, "5,10,15,20,30,60") ?: "5,10,15,20,30,60"
            return str.split(",").mapNotNull { it.trim().toIntOrNull() }.distinct().sorted()
        }
        set(value) {
            val str = value.distinct().sorted().joinToString(",")
            prefs.edit().putString(KEY_CUSTOM_STEPS, str).apply()
        }

    fun addStep(step: Int) {
        val current = stepList.toMutableList()
        if (!current.contains(step) && step > 0) {
            current.add(step)
            stepList = current
        }
    }

    fun removeStep(step: Int) {
        val current = stepList.toMutableList()
        current.remove(step)
        if (current.isNotEmpty()) {
            stepList = current
            if (selectedStep == step) {
                selectedStep = current.first()
            }
        }
    }

    var vibrateEnable: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE_ENABLE, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE_ENABLE, value).apply()

    var vibrateDuration: Int
        get() = prefs.getInt(KEY_VIBRATE_DURATION, 1000)
        set(value) = prefs.edit().putInt(KEY_VIBRATE_DURATION, value).apply()

    var ringtoneEnable: Boolean
        get() = prefs.getBoolean(KEY_RINGTONE_ENABLE, true)
        set(value) = prefs.edit().putBoolean(KEY_RINGTONE_ENABLE, value).apply()

    var beepDuration: Int
        get() = prefs.getInt(KEY_BEEP_DURATION, 1000)
        set(value) = prefs.edit().putInt(KEY_BEEP_DURATION, value).apply()

    var beepVolume: Int
        get() = prefs.getInt(KEY_BEEP_VOLUME, 80)
        set(value) = prefs.edit().putInt(KEY_BEEP_VOLUME, value).apply()

    var allowNotifStop: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_NOTIF_STOP, true)
        set(value) = prefs.edit().putBoolean(KEY_ALLOW_NOTIF_STOP, value).apply()
}
