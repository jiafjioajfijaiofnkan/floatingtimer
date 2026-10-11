package com.example.floatingtimer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PreferencesManager
    private var isServiceRunning = false

    private lateinit var tvPermStatus: TextView
    private lateinit var btnRequestPerm: Button
    private lateinit var btnToggleService: Button

    private lateinit var btnCopyLogs: Button
    private lateinit var btnClearLogs: Button

    private lateinit var rgTheme: RadioGroup
    private lateinit var rgStep: RadioGroup
    private lateinit var etCustomStep: EditText
    private lateinit var btnAddStep: Button
    private lateinit var btnDeleteStep: Button

    private lateinit var cbVibrate: CheckBox
    private lateinit var tvVibrateDuration: TextView
    private lateinit var sbVibrateDuration: SeekBar
    private lateinit var btnTestVibrate: Button

    private lateinit var cbRingtone: CheckBox
    private lateinit var rgBeepDuration: RadioGroup
    private lateinit var tvBeepVolume: TextView
    private lateinit var sbBeepVolume: SeekBar
    private lateinit var btnTestBeep: Button

    private lateinit var cbAllowNotifStop: CheckBox

    private var vibrator: Vibrator? = null
    private var toneGenerator: ToneGenerator? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        AppLogger.init(applicationContext)
        AppLogger.log("MainActivity", "onCreate called")

        prefs = PreferencesManager(this)
        applyTheme(prefs.themeMode)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        @Suppress("DEPRECATION")
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        initViews()
        loadPreferences()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        checkOverlayPermission()
    }

    private fun initViews() {
        tvPermStatus = findViewById(R.id.tv_perm_status)
        btnRequestPerm = findViewById(R.id.btn_request_perm)
        btnToggleService = findViewById(R.id.btn_toggle_service)

        btnCopyLogs = findViewById(R.id.btn_copy_logs)
        btnClearLogs = findViewById(R.id.btn_clear_logs)

        rgTheme = findViewById(R.id.rg_theme)
        rgStep = findViewById(R.id.rg_step)
        etCustomStep = findViewById(R.id.et_custom_step)
        btnAddStep = findViewById(R.id.btn_add_step)
        btnDeleteStep = findViewById(R.id.btn_delete_step)

        cbVibrate = findViewById(R.id.cb_vibrate)
        tvVibrateDuration = findViewById(R.id.tv_vibrate_duration)
        sbVibrateDuration = findViewById(R.id.sb_vibrate_duration)
        btnTestVibrate = findViewById(R.id.btn_test_vibrate)

        cbRingtone = findViewById(R.id.cb_ringtone)
        rgBeepDuration = findViewById(R.id.rg_beep_duration)
        tvBeepVolume = findViewById(R.id.tv_beep_volume)
        sbBeepVolume = findViewById(R.id.sb_beep_volume)
        btnTestBeep = findViewById(R.id.btn_test_beep)

        cbAllowNotifStop = findViewById(R.id.cb_allow_notif_stop)
    }

    private fun applyTheme(mode: Int) {
        when (mode) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun loadPreferences() {
        when (prefs.themeMode) {
            0 -> findViewById<RadioButton>(R.id.rb_theme_dark).isChecked = true
            1 -> findViewById<RadioButton>(R.id.rb_theme_light).isChecked = true
            else -> findViewById<RadioButton>(R.id.rb_theme_system).isChecked = true
        }

        renderStepRadioButtons()

        cbVibrate.isChecked = prefs.vibrateEnable
        val vibDur = prefs.vibrateDuration
        sbVibrateDuration.progress = vibDur
        tvVibrateDuration.text = "震动时长: %.1f 秒".format(vibDur / 1000f)

        cbRingtone.isChecked = prefs.ringtoneEnable
        if (prefs.beepDuration == 500) {
            findViewById<RadioButton>(R.id.rb_beep_05s).isChecked = true
        } else {
            findViewById<RadioButton>(R.id.rb_beep_1s).isChecked = true
        }

        val vol = prefs.beepVolume
        sbBeepVolume.progress = vol
        tvBeepVolume.text = "蜂鸣音量: $vol%"

        cbAllowNotifStop.isChecked = prefs.allowNotifStop
    }

    private fun renderStepRadioButtons() {
        rgStep.removeAllViews()
        val steps = prefs.stepList
        val selected = prefs.selectedStep

        for (step in steps) {
            val rb = RadioButton(this).apply {
                id = View.generateViewId()
                text = "${step}s"
                tag = step
                isChecked = (step == selected)
            }
            rgStep.addView(rb)
        }
    }

    private fun setupListeners() {
        btnCopyLogs.setOnClickListener {
            val logs = AppLogger.getLogs()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("FloatingTimerLogs", logs)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "运行日志已复制到剪贴板！", Toast.LENGTH_SHORT).show()
        }

        btnClearLogs.setOnClickListener {
            AppLogger.clearLogs()
            Toast.makeText(this, "日志已清理！", Toast.LENGTH_SHORT).show()
        }

        rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rb_theme_dark -> 0
                R.id.rb_theme_light -> 1
                else -> 2
            }
            if (mode != prefs.themeMode) {
                prefs.themeMode = mode
                applyTheme(mode)
            }
        }

        btnRequestPerm.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        btnToggleService.setOnClickListener {
            val serviceIntent = Intent(this, FloatingTimerService::class.java)
            if (isServiceRunning) {
                stopService(serviceIntent)
                isServiceRunning = false
                btnToggleService.text = "开启悬浮球服务"
                AppLogger.log("MainActivity", "Service stopped")
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "请先授予悬浮窗权限！", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                isServiceRunning = true
                btnToggleService.text = "关闭悬浮球服务"
                AppLogger.log("MainActivity", "Service started")
            }
        }

        rgStep.setOnCheckedChangeListener { group, checkedId ->
            val radioButton = group.findViewById<RadioButton>(checkedId)
            val step = radioButton?.tag as? Int
            if (step != null) {
                prefs.selectedStep = step
                notifyServiceUpdate()
            }
        }

        btnAddStep.setOnClickListener {
            val inputText = etCustomStep.text.toString().trim()
            val newStep = inputText.toIntOrNull()
            if (newStep != null && newStep > 0) {
                prefs.addStep(newStep)
                prefs.selectedStep = newStep
                etCustomStep.text.clear()
                renderStepRadioButtons()
                notifyServiceUpdate()
                Toast.makeText(this, "已成功添加 ${newStep}s 额度", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "请输入有效秒数！", Toast.LENGTH_SHORT).show()
            }
        }

        btnDeleteStep.setOnClickListener {
            val currentSelected = prefs.selectedStep
            val steps = prefs.stepList
            if (steps.size <= 1) {
                Toast.makeText(this, "至少保留一个加时额度！", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.removeStep(currentSelected)
            renderStepRadioButtons()
            notifyServiceUpdate()
            Toast.makeText(this, "已移除 ${currentSelected}s 额度", Toast.LENGTH_SHORT).show()
        }

        cbVibrate.setOnCheckedChangeListener { _, isChecked ->
            prefs.vibrateEnable = isChecked
            notifyServiceUpdate()
        }

        sbVibrateDuration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val dur = if (progress < 100) 100 else progress
                tvVibrateDuration.text = "震动时长: %.1f 秒".format(dur / 1000f)
                if (fromUser) {
                    prefs.vibrateDuration = dur
                    notifyServiceUpdate()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnTestVibrate.setOnClickListener {
            val v = vibrator ?: return@setOnClickListener
            val duration = prefs.vibrateDuration.toLong()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(duration)
            }
        }

        cbRingtone.setOnCheckedChangeListener { _, isChecked ->
            prefs.ringtoneEnable = isChecked
            notifyServiceUpdate()
        }

        rgBeepDuration.setOnCheckedChangeListener { _, checkedId ->
            val duration = if (checkedId == R.id.rb_beep_05s) 500 else 1000
            prefs.beepDuration = duration
            notifyServiceUpdate()
        }

        sbBeepVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvBeepVolume.text = "蜂鸣音量: $progress%"
                if (fromUser) {
                    prefs.beepVolume = progress
                    notifyServiceUpdate()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnTestBeep.setOnClickListener {
            testBeep(prefs.beepDuration, prefs.beepVolume)
        }

        cbAllowNotifStop.setOnCheckedChangeListener { _, isChecked ->
            prefs.allowNotifStop = isChecked
            notifyServiceUpdate()
        }
    }

    private fun testBeep(durationMs: Int, volume: Int) {
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()

            val streamType = AudioManager.STREAM_ALARM
            toneGenerator = ToneGenerator(streamType, volume)
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs)

            handler.postDelayed({
                toneGenerator?.stopTone()
                toneGenerator?.release()
                toneGenerator = null
            }, durationMs.toLong() + 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                tvPermStatus.text = "悬浮窗权限: 已授予 ✅"
                btnRequestPerm.isEnabled = false
            } else {
                tvPermStatus.text = "悬浮窗权限: 未授予 ❌"
                btnRequestPerm.isEnabled = true
            }
        } else {
            tvPermStatus.text = "悬浮窗权限: 默认开启 ✅"
            btnRequestPerm.isEnabled = false
        }
    }

    private fun notifyServiceUpdate() {
        val intent = Intent(this, FloatingTimerService::class.java).apply {
            action = FloatingTimerService.ACTION_UPDATE_PREFS
        }
        startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
    }
}
