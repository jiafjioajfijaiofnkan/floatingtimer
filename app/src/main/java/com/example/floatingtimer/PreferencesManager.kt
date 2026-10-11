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

        // 震动
        const val KEY_VIBRATE_ENABLE = "vibrate_enable"
        const val KEY_VIBRATE_DURATION = "vibrate_duration" // ms
        const val KEY_VIBRATE_INTENSITY = "vibrate_intensity" // 1 - 255

        // 响铃 (Beep)
        const val KEY_RINGTONE_ENABLE = "ringtone_enable"
        const val KEY_BEEP_DURATION = "beep_duration" // ms
        const val KEY_BEEP_VOLUME = "beep_volume" // 1 - 100

        // 视觉提醒 ( Shake / Flash )
        const val KEY_SHAKE_ENABLE = "shake_enable"
        const val KEY_SHAKE_DURATION = "shake_duration" // ms
        const val KEY_SHAKE_AMPLITUDE = "shake_amplitude" // px/dp
        const val KEY_FLASH_ENABLE = "flash_enable"

        // 界面显示
        const val KEY_SHOW_PROGRESS_RING = "show_progress_ring"
        const val KEY_CHANGE_COLOR_ON_TIMER = "change_color_on_timer"

        // 其他
        const val KEY_ALLOW_NOTIF_STOP = "allow_notif_stop"
    }

    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, 0)
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()

    var selectedStep: Int
        get() = prefs.getInt(KEY_SELECTED_STEP, 15)
        set(value) = prefs.edit().putInt(KEY_SELECTED_STEP, value).apply()

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

    // 震动配置
    var vibrateEnable: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE_ENABLE, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE_ENABLE, value).apply()

    var vibrateDuration: Int
        get() = prefs.getInt(KEY_VIBRATE_DURATION, 1000)
        set(value) = prefs.edit().putInt(KEY_VIBRATE_DURATION, value).apply()

    var vibrateIntensity: Int
        get() = prefs.getInt(KEY_VIBRATE_INTENSITY, 200)
        set(value) = prefs.edit().putInt(KEY_VIBRATE_INTENSITY, value).apply()

    // 蜂鸣配置
    var ringtoneEnable: Boolean
        get() = prefs.getBoolean(KEY_RINGTONE_ENABLE, true)
        set(value) = prefs.edit().putBoolean(KEY_RINGTONE_ENABLE, value).apply()

    var beepDuration: Int
        get() = prefs.getInt(KEY_BEEP_DURATION, 1000)
        set(value) = prefs.edit().putInt(KEY_BEEP_DURATION, value).apply()

    var beepVolume: Int
        get() = prefs.getInt(KEY_BEEP_VOLUME, 80)
        set(value) = prefs.edit().putInt(KEY_BEEP_VOLUME, value).apply()

    // 视觉动画配置
    var shakeEnable: Boolean
        get() = prefs.getBoolean(KEY_SHAKE_ENABLE, true)
        set(value) = prefs.edit().putBoolean(KEY_SHAKE_ENABLE, value).apply()

    var shakeDuration: Int
        get() = prefs.getInt(KEY_SHAKE_DURATION, 800)
        set(value) = prefs.edit().putInt(KEY_SHAKE_DURATION, value).apply()

    var shakeAmplitude: Int
        get() = prefs.getInt(KEY_SHAKE_AMPLITUDE, 15) // px
        set(value) = prefs.edit().putInt(KEY_SHAKE_AMPLITUDE, value).apply()

    var flashEnable: Boolean
        get() = prefs.getBoolean(KEY_FLASH_ENABLE, true)
        set(value) = prefs.edit().putBoolean(KEY_FLASH_ENABLE, value).apply()

    // 显示控制
    var showProgressRing: Boolean
        get() = prefs.getBoolean(KEY_SHOW_PROGRESS_RING, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_PROGRESS_RING, value).apply()

    var changeColorOnTimer: Boolean
        get() = prefs.getBoolean(KEY_CHANGE_COLOR_ON_TIMER, true)
        set(value) = prefs.edit().putBoolean(KEY_CHANGE_COLOR_ON_TIMER, value).apply()

    var allowNotifStop: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_NOTIF_STOP, true)
        set(value) = prefs.edit().putBoolean(KEY_ALLOW_NOTIF_STOP, value).apply()
}
