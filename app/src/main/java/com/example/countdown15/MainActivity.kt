package com.example.countdown15

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvTimer: TextView
    private lateinit var tvVersion: TextView
    private lateinit var btnStart: FrameLayout
    private lateinit var btnStop: FrameLayout
    private lateinit var npHour: WheelPicker
    private lateinit var npMinute: WheelPicker
    private lateinit var npSecond: WheelPicker
    private lateinit var rootLayout: FrameLayout
    private lateinit var btnMenu: ImageView
    private lateinit var colon1: TextView
    private lateinit var colon2: TextView
    private lateinit var menuMask: View

    private var timer: CountDownTimer? = null
    private var ringtone: Ringtone? = null

    private var isCounting = false
    private var totalSeconds = 15

    private var isDarkTheme = false

    private val prefsName = "countdown15_prefs"
    private val keyDarkTheme = "dark_theme"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        isDarkTheme = prefs.getBoolean(keyDarkTheme, false)

        @Suppress("DEPRECATION")
        var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isDarkTheme) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = flags

        setContentView(R.layout.activity_main)

        rootLayout = findViewById(R.id.rootLayout)
        tvTimer = findViewById(R.id.tvTimer)
        tvVersion = findViewById(R.id.tvVersion)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        npHour = findViewById(R.id.npHour)
        npMinute = findViewById(R.id.npMinute)
        npSecond = findViewById(R.id.npSecond)
        btnMenu = findViewById(R.id.btnMenu)
        colon1 = findViewById(R.id.colon1)
        colon2 = findViewById(R.id.colon2)
        menuMask = findViewById(R.id.menuMask)

        tvVersion.text = "v" + getAppVersion()

        npHour.setValue(0)
        npMinute.setValue(0)
        npSecond.setValue(15)

        val timePickerLayout = findViewById<View>(R.id.timePickerLayout)
        rootLayout.post {
            val screenHeight = resources.displayMetrics.heightPixels
            val pickerHeight = timePickerLayout.height
            val topMargin = (screenHeight - pickerHeight) / 2

            val params = timePickerLayout.layoutParams as FrameLayout.LayoutParams
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.topMargin = topMargin
            timePickerLayout.layoutParams = params
        }

        showStartState()
        applyTheme(isDarkTheme)

        btnStart.setOnClickListener {
            totalSeconds = npHour.value * 3600 + npMinute.value * 60 + npSecond.value
            if (totalSeconds <= 0) totalSeconds = 1
            startCountdown()
        }

        btnStop.setOnClickListener {
            stopRingtoneAndReset()
        }

        tvTimer.setOnClickListener {
            if (isCounting) {
                startCountdown()
            }
        }

        // 右上角菜单
        btnMenu.setOnClickListener {
            // 遮罩：渐显
            menuMask.alpha = 0f
            menuMask.visibility = View.VISIBLE
            menuMask.animate()
                .alpha(1f)
                .setDuration(200)
                .start()

            val popupView = layoutInflater.inflate(R.layout.popup_menu, null)
            val popupRoot = popupView.findViewById<LinearLayout>(R.id.popupRoot)
            val itemToggle = popupView.findViewById<TextView>(R.id.popupToggleTheme)

            val popupBgColor = if (isDarkTheme) Color.parseColor("#2B2B2B") else Color.parseColor("#FFFFFF")
            val popupTextColor = if (isDarkTheme) Color.parseColor("#FFFFFF") else Color.parseColor("#000000")

            val bg = GradientDrawable()
            bg.shape = GradientDrawable.RECTANGLE
            bg.cornerRadius = dp(8f).toFloat()
            bg.setColor(popupBgColor)
            popupRoot.background = bg

            itemToggle.setTextColor(popupTextColor)

            val popup = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )
            popup.elevation = dp(8f).toFloat()
            popup.isOutsideTouchable = true
            popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            itemToggle.setOnClickListener {
                isDarkTheme = !isDarkTheme
                prefs.edit().putBoolean(keyDarkTheme, isDarkTheme).apply()
                applyTheme(isDarkTheme)
                popup.dismiss()
            }

            popup.setOnDismissListener {
                menuMask.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction {
                        menuMask.visibility = View.GONE
                    }
                    .start()
            }

            menuMask.setOnClickListener {
                popup.dismiss()
            }

            // 先测量 popup 尺寸
            popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val popupWidth = popupView.measuredWidth

            // 三个点按钮在屏幕上的坐标
            val loc = IntArray(2)
            btnMenu.getLocationOnScreen(loc)

            // 菜单右边和按钮右边对齐，再往左偏 20dp
            val x = loc[0] + btnMenu.width - popupWidth - dp(20f)
            val y = loc[1] + btnMenu.height + dp(8f)

            popup.showAtLocation(rootLayout, Gravity.NO_GRAVITY, x, y)

            // 缩放动画
            popupView.post {
                popupView.pivotX = popupView.width.toFloat()
                popupView.pivotY = 0f
                popupView.scaleX = 0f
                popupView.scaleY = 0f
                popupView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
        }
    }

    private fun applyTheme(dark: Boolean) {
        val bgColor = if (dark) Color.parseColor("#000000") else Color.parseColor("#F5F5F5")
        val mainTextColor = if (dark) Color.parseColor("#FFFFFF") else Color.parseColor("#000000")
        val versionColor = if (dark) Color.parseColor("#888888") else Color.parseColor("#999999")
        val buttonBgColor = if (dark) Color.parseColor("#1E1E1E") else Color.parseColor("#FFFFFF")

        rootLayout.setBackgroundColor(bgColor)

        colon1.setTextColor(mainTextColor)
        colon2.setTextColor(mainTextColor)

        tvTimer.setTextColor(mainTextColor)
        tvVersion.setTextColor(versionColor)

        val btnBg = GradientDrawable()
        btnBg.shape = GradientDrawable.RECTANGLE
        btnBg.cornerRadius = dp(32f).toFloat()
        btnBg.setColor(buttonBgColor)
        btnStart.background = btnBg
        btnStop.background = btnBg

        npHour.setDarkTheme(dark)
        npMinute.setDarkTheme(dark)
        npSecond.setDarkTheme(dark)

        btnMenu.setColorFilter(mainTextColor)

        @Suppress("DEPRECATION")
        var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!dark) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = flags
    }

    private fun dp(value: Float): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun getAppVersion(): String {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: "0.0.1"
        } catch (e: Exception) {
            "0.0.1"
        }
    }

    private fun showStartState() {
        isCounting = false
        tvTimer.visibility = TextView.GONE
        findViewById<View>(R.id.timePickerLayout).visibility = View.VISIBLE
        btnStart.visibility = View.VISIBLE
        btnStop.visibility = View.GONE
    }

    private fun showCountingState() {
        isCounting = true
        findViewById<View>(R.id.timePickerLayout).visibility = View.GONE
        btnStart.visibility = View.GONE
        btnStop.visibility = View.VISIBLE
        tvTimer.visibility = TextView.VISIBLE
    }

    private fun showRingingState() {
        isCounting = false
        findViewById<View>(R.id.timePickerLayout).visibility = View.GONE
        tvTimer.visibility = TextView.GONE
        btnStart.visibility = View.GONE
        btnStop.visibility = View.VISIBLE
    }

    private fun startCountdown() {
        stopRingtone()
        timer?.cancel()
        showCountingState()

        val totalMillis = totalSeconds * 1000L
        tvTimer.text = formatSeconds(totalSeconds)

        timer = object : CountDownTimer(totalMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt() + 1
                tvTimer.text = formatSeconds(seconds)
            }

            override fun onFinish() {
                playRingtone()
                showRingingState()
            }
        }.start()
    }

    private fun formatSeconds(total: Int): String {
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) {
            String.format("%d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }

    private fun playRingtone() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(applicationContext, uri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRingtone() {
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            ringtone = null
        }
    }

    private fun stopRingtoneAndReset() {
        stopRingtone()
        timer?.cancel()
        showStartState()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        ringtone?.stop()
    }
}