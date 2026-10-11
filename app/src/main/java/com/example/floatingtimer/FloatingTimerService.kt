package com.example.floatingtimer

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class FloatingTimerService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: PreferencesManager

    private var floatingView: View? = null
    private var flBallContainer: FrameLayout? = null
    private var tvTimer: TextView? = null
    private var pbRing: ProgressBar? = null
    private var vDocked: View? = null
    private var params: WindowManager.LayoutParams? = null

    // 倒计时逻辑
    private var totalSeconds = 0
    private var remainingSeconds = 0
    private var countDownTimer: CountDownTimer? = null

    // 提醒控制（震动与 Beep）
    private var vibrator: Vibrator? = null
    private var toneGenerator: ToneGenerator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var stopBeepRunnable: Runnable? = null

    // 状态控制：是否在贴边隐藏状态
    private var isDocked = false
    private var isDockedOnLeft = true

    companion object {
        const val CHANNEL_ID = "FloatingTimerChannel"
        const val NOTIF_ID = 1001
        const val ACTION_STOP_ALARM = "com.example.floatingtimer.ACTION_STOP_ALARM"
        const val ACTION_UPDATE_PREFS = "com.example.floatingtimer.ACTION_UPDATE_PREFS"
    }

    override fun onCreate() {
        super.onCreate()
        AppLogger.log("FloatingTimerService", "onCreate called")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = PreferencesManager(this)
        @Suppress("DEPRECATION")
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("悬浮球运行中"))

        initFloatingWindow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLogger.log("FloatingTimerService", "onStartCommand action=${intent?.action}")
        when (intent?.action) {
            ACTION_STOP_ALARM -> stopAlarm()
            ACTION_UPDATE_PREFS -> {
                updateVisualPrefs()
            }
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppLogger.log("FloatingTimerService", "onConfigurationChanged orientation=${newConfig.orientation}")
        if (isDocked) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                floatingView?.visibility = View.GONE
            } else {
                floatingView?.visibility = View.VISIBLE
            }
        }
    }

    private fun initFloatingWindow() {
        try {
            floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_ball, null)
            flBallContainer = floatingView?.findViewById(R.id.fl_ball_container)
            tvTimer = floatingView?.findViewById(R.id.tv_timer)
            pbRing = floatingView?.findViewById(R.id.pb_ring)
            vDocked = floatingView?.findViewById(R.id.v_docked)

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 300
            }

            setupTouchListener()

            windowManager.addView(floatingView, params)
            AppLogger.log("FloatingTimerService", "Floating window added successfully")
        } catch (e: Exception) {
            AppLogger.e("FloatingTimerService", "Failed to init floating window", e)
        }
        updateTimerDisplay()
        updateVisualPrefs()
    }

    private fun updateVisualPrefs() {
        if (prefs.showProgressRing && remainingSeconds > 0) {
            pbRing?.visibility = View.VISIBLE
        } else {
            pbRing?.visibility = View.GONE
        }

        if (prefs.changeColorOnTimer && remainingSeconds > 0) {
            tvTimer?.setBackgroundResource(R.drawable.bg_floating_ball_active)
        } else {
            tvTimer?.setBackgroundResource(R.drawable.bg_floating_ball)
        }
    }

    private fun safeUpdateViewLayout() {
        val view = floatingView ?: return
        val p = params ?: return
        try {
            if (view.isAttachedToWindow) {
                windowManager.updateViewLayout(view, p)
            } else {
                AppLogger.log("FloatingTimerService", "safeUpdateViewLayout skipped: view not attached")
            }
        } catch (e: Exception) {
            AppLogger.e("FloatingTimerService", "Exception in safeUpdateViewLayout", e)
        }
    }

    private fun setupTouchListener() {
        val touchView = floatingView ?: return

        touchView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var touchStartX = 0f
            private var touchStartY = 0f
            private var isDragging = false
            private val touchSlop = 10

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                val p = params ?: return false

                try {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = p.x
                            initialY = p.y
                            touchStartX = event.rawX
                            touchStartY = event.rawY
                            isDragging = false
                            return false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - touchStartX).toInt()
                            val dy = (event.rawY - touchStartY).toInt()

                            if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                                isDragging = true
                            }

                            if (isDragging) {
                                if (isDocked) {
                                    undockFloatingBall()
                                }
                                p.x = initialX + dx
                                p.y = initialY + dy
                                safeUpdateViewLayout()
                            }
                            return isDragging
                        }
                        MotionEvent.ACTION_UP -> {
                            if (isDragging) {
                                AppLogger.log("FloatingTimerService", "ACTION_UP dragging finish at x=${p.x}, y=${p.y}")
                                checkDocking(p.x)
                                return true
                            }
                            return false
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e("FloatingTimerService", "Exception in onTouch event=${event.action}", e)
                }
                return false
            }
        })

        touchView.setOnClickListener {
            try {
                if (isDocked) {
                    undockFloatingBall()
                } else {
                    onBallClicked()
                }
            } catch (e: Exception) {
                AppLogger.e("FloatingTimerService", "Exception in setOnClickListener", e)
            }
        }

        touchView.setOnLongClickListener {
            try {
                if (!isDocked) {
                    showPresetOptionDialog()
                } else {
                    undockFloatingBall()
                }
            } catch (e: Exception) {
                AppLogger.e("FloatingTimerService", "Exception in setOnLongClickListener", e)
            }
            true
        }
    }

    private fun getScreenWidth(): Int {
        return try {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.widthPixels
        } catch (e: Exception) {
            AppLogger.e("FloatingTimerService", "Failed to get screen width", e)
            1080
        }
    }

    private fun checkDocking(x: Int) {
        try {
            val screenWidth = getScreenWidth()
            val ballSizePx = dpToPx(56)
            val edgeThreshold = dpToPx(40)

            AppLogger.log("FloatingTimerService", "checkDocking x=$x, screenWidth=$screenWidth, threshold=$edgeThreshold")

            if (x <= edgeThreshold) {
                dockFloatingBall(isLeft = true)
            } else if (x >= screenWidth - ballSizePx - edgeThreshold) {
                dockFloatingBall(isLeft = false)
            }
        } catch (e: Exception) {
            AppLogger.e("FloatingTimerService", "Exception in checkDocking", e)
        }
    }

    private fun dockFloatingBall(isLeft: Boolean) {
        try {
            AppLogger.log("FloatingTimerService", "dockFloatingBall isLeft=$isLeft")
            isDocked = true
            isDockedOnLeft = isLeft
            flBallContainer?.visibility = View.GONE
            vDocked?.visibility = View.VISIBLE

            vDocked?.setBackgroundResource(
                if (isLeft) R.drawable.bg_docked_bar_left else R.drawable.bg_docked_bar_right
            )

            params?.let { p ->
                p.x = if (isLeft) 0 else (getScreenWidth() - dpToPx(18)).coerceAtLeast(0)
                safeUpdateViewLayout()
            }
        } catch (e: Exception) {
            AppLogger.e("FloatingTimerService", "Exception in dockFloatingBall", e)
        }
    }

    private fun undockFloatingBall() {
        try {
            if (!isDocked) return
            AppLogger.log("FloatingTimerService", "undockFloatingBall triggered")
            isDocked = false
            vDocked?.visibility = View.GONE
            flBallContainer?.visibility = View.VISIBLE

            params?.let { p ->
                val ballSizePx = dpToPx(56)
                if (isDockedOnLeft) {
                    p.x = dpToPx(20)
                } else {
                    p.x = (getScreenWidth() - ballSizePx - dpToPx(20)).coerceAtLeast(0)
                }
                safeUpdateViewLayout()
            }
        } catch (e: Exception) {
            AppLogger.e("FloatingTimerService", "Exception in undockFloatingBall", e)
        }
    }

    private fun onBallClicked() {
        val step = prefs.selectedStep
        remainingSeconds += step
        totalSeconds += step
        AppLogger.log("FloatingTimerService", "onBallClicked add ${step}s, total=$remainingSeconds")
        updateTimerDisplay()
        updateVisualPrefs()
        startOrRestartTimer()
    }

    private fun startOrRestartTimer() {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(remainingSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = ((millisUntilFinished + 500) / 1000).toInt()
                updateTimerDisplay()
            }

            override fun onFinish() {
                remainingSeconds = 0
                totalSeconds = 0
                updateTimerDisplay()
                updateVisualPrefs()
                onTimerFinished()
            }
        }.start()
    }

    private fun updateTimerDisplay() {
        tvTimer?.text = remainingSeconds.toString()
        if (totalSeconds > 0 && prefs.showProgressRing) {
            pbRing?.visibility = View.VISIBLE
            val pct = (remainingSeconds.toFloat() / totalSeconds * 100).toInt()
            pbRing?.progress = pct
        } else {
            pbRing?.visibility = View.GONE
        }
    }

    private fun onTimerFinished() {
        AppLogger.log("FloatingTimerService", "onTimerFinished triggered")

        if (prefs.shakeEnable) {
            triggerShakeAnimation()
        }

        if (prefs.flashEnable) {
            triggerFlashAnimation()
        }

        if (prefs.vibrateEnable) {
            triggerVibration(prefs.vibrateDuration.toLong(), prefs.vibrateIntensity)
        }

        if (prefs.ringtoneEnable) {
            triggerBeep(prefs.beepDuration, prefs.beepVolume)
        }

        val notifManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notifManager.notify(NOTIF_ID, buildNotification("倒计时结束！", showStopAction = prefs.allowNotifStop))
    }

    private fun triggerShakeAnimation() {
        try {
            val amplitude = dpToPx(prefs.shakeAmplitude)
            val duration = prefs.shakeDuration.toLong()

            val rotate = TranslateAnimation(
                -amplitude.toFloat(), amplitude.toFloat(), 0f, 0f
            ).apply {
                this.duration = 50
                repeatCount = (duration / 50).toInt()
                repeatMode = Animation.REVERSE
            }
            flBallContainer?.startAnimation(rotate)
        } catch (e: Exception) {
            AppLogger.e("FloatingTimerService", "Exception in triggerShakeAnimation", e)
        }
    }

    private fun triggerFlashAnimation() {
        try {
            val alphaAnim = AlphaAnimation(0.2f, 1.0f).apply {
                duration = 150
                repeatCount = 5
                repeatMode = Animation.REVERSE
            }
            flBallContainer?.startAnimation(alphaAnim)
        } catch (e: Exception) {
            AppLogger.e("FloatingTimerService", "Exception in triggerFlashAnimation", e)
        }
    }

    private fun triggerVibration(durationMs: Long, intensity: Int) {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(durationMs, intensity.coerceIn(1, 255))
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(durationMs)
        }
    }

    private fun triggerBeep(durationMs: Int, volume: Int) {
        try {
            stopBeep()
            val streamType = AudioManager.STREAM_ALARM
            toneGenerator = ToneGenerator(streamType, volume)
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs)

            stopBeepRunnable = Runnable {
                stopBeep()
            }
            handler.postDelayed(stopBeepRunnable!!, durationMs.toLong() + 100)
        } catch (e: Exception) {
            AppLogger.e("FloatingTimerService", "Exception in triggerBeep", e)
        }
    }

    private fun stopBeep() {
        stopBeepRunnable?.let { handler.removeCallbacks(it) }
        stopBeepRunnable = null
        toneGenerator?.stopTone()
        toneGenerator?.release()
        toneGenerator = null
    }

    private fun stopAlarm() {
        vibrator?.cancel()
        stopBeep()
        val notifManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notifManager.notify(NOTIF_ID, buildNotification("悬浮球运行中"))
    }

    private fun showPresetOptionDialog() {
        try {
            val steps = prefs.stepList
            val labels = steps.map { "${it}秒" }.toMutableList()
            labels.add("⚙️ 打开设置")

            val themedContext = ContextThemeWrapper(this, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog)
            val builder = AlertDialog.Builder(themedContext)
                .setTitle("选择加时额度 (当前: ${prefs.selectedStep}s)")
                .setItems(labels.toTypedArray()) { dialog, which ->
                    if (which == steps.size) {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(intent)
                    } else {
                        val newStep = steps[which]
                        prefs.selectedStep = newStep
                        Toast.makeText(this, "已切换加时额度为 ${newStep}s", Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }

            val alertDialog = builder.create()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                alertDialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            } else {
                @Suppress("DEPRECATION")
                alertDialog.window?.setType(WindowManager.LayoutParams.TYPE_PHONE)
            }
            alertDialog.show()
        } catch (e: Exception) {
            AppLogger.e("FloatingTimerService", "Exception in showPresetOptionDialog", e)
            Toast.makeText(this, "长按菜单加载失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮倒计时服务",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String, showStopAction: Boolean = false): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("悬浮倒计时")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        if (showStopAction) {
            val stopIntent = Intent(this, NotificationActionReceiver::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            val stopPendingIntent = PendingIntent.getBroadcast(
                this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "停止提醒", stopPendingIntent)
        }

        return builder.build()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.log("FloatingTimerService", "onDestroy called")
        countDownTimer?.cancel()
        stopAlarm()
        floatingView?.let {
            if (it.isAttachedToWindow) {
                try {
                    windowManager.removeView(it)
                } catch (e: Exception) {
                    AppLogger.e("FloatingTimerService", "Exception in removeView", e)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
