package com.example.countdown15

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.view.animation.LinearInterpolator
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat

class MainActivity : AppCompatActivity() {

    private lateinit var timerRow: LinearLayout
    private lateinit var tvHour: TextView
    private lateinit var tvColon1: TextView
    private lateinit var tvMinute: TextView
    private lateinit var tvColon2: TextView
    private lateinit var tvSecond: TextView

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
    private lateinit var countdownLayout: FrameLayout
    private lateinit var ringView: RingTimerView

    private val flyViews = mutableListOf<View>()

    /** 冒号墨迹中心相对字体框中心的偏移，滚轮和计时用同一个值 */
    private var colonOffsetY = 0f

    private var countdownAnimator: ValueAnimator? = null
    private var ringtone: Ringtone? = null

    private var isCounting = false
    private var totalSeconds = 15

    private var isDarkTheme = false

    private val prefsName = "countdown15_prefs"
    private val keyDarkTheme = "dark_theme"

    private val timerViews: List<TextView>
        get() = listOf(tvHour, tvColon1, tvMinute, tvColon2, tvSecond)

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
        timerRow = findViewById(R.id.timerRow)
        tvHour = findViewById(R.id.tvHour)
        tvColon1 = findViewById(R.id.tvColon1)
        tvMinute = findViewById(R.id.tvMinute)
        tvColon2 = findViewById(R.id.tvColon2)
        tvSecond = findViewById(R.id.tvSecond)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        npHour = findViewById(R.id.npHour)
        npMinute = findViewById(R.id.npMinute)
        npSecond = findViewById(R.id.npSecond)
        btnMenu = findViewById(R.id.btnMenu)
        colon1 = findViewById(R.id.colon1)
        colon2 = findViewById(R.id.colon2)
        menuMask = findViewById(R.id.menuMask)
        countdownLayout = findViewById(R.id.countdownLayout)
        ringView = findViewById(R.id.ringView)

        // 计时数字与滚轮用同一套字体，冒号用同样的视觉对齐
        val timerFont = ResourcesCompat.getFont(this, R.font.robotoregular)
        timerViews.forEach {
            it.typeface = timerFont
            it.includeFontPadding = false
        }
        colonOffsetY = colonOffset(tvColon1.textSize)
        tvColon1.translationY = colonOffsetY
        tvColon2.translationY = colonOffsetY

        npHour.setValue(0)
        npMinute.setValue(0)
        npSecond.setValue(15)

        val timePickerLayout = findViewById<View>(R.id.timePickerLayout)
        rootLayout.post {
            val pickerHeight = timePickerLayout.height
            val topMargin = (rootLayout.height - pickerHeight) / 2

            val params = timePickerLayout.layoutParams as FrameLayout.LayoutParams
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.topMargin = topMargin
            timePickerLayout.layoutParams = params

            // 倒计时圆环：直径 74.2% 屏宽，按物理屏幕居中（含导航栏区域）
            val ringSize = (rootLayout.width * 0.742f).toInt()
            val boxSize = ringSize + ringView.ringInset * 2
            val screenHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowManager.currentWindowMetrics.bounds.height()
            } else {
                resources.displayMetrics.heightPixels
            }
            val rootLocation = IntArray(2)
            rootLayout.getLocationOnScreen(rootLocation)
            val ringParams = FrameLayout.LayoutParams(boxSize, boxSize)
            ringParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ringParams.topMargin = ((screenHeight - boxSize) / 2f - rootLocation[1]).toInt()
            countdownLayout.layoutParams = ringParams
        }

        showStartState()
        applyTheme(isDarkTheme)
        alignColons()

        btnStart.setOnClickListener {
            // 先把还在惯性滑动中的滚轮落位，保证读到的就是中间那个数字
            npHour.commitPendingValue()
            npMinute.commitPendingValue()
            npSecond.commitPendingValue()
            totalSeconds = npHour.value * 3600 + npMinute.value * 60 + npSecond.value
            if (totalSeconds <= 0) totalSeconds = 1
            startCountdown()
        }

        btnStop.setOnClickListener {
            stopRingtoneAndReset()
        }

        timerRow.setOnClickListener {
            if (isCounting) {
                startCountdown()
            }
        }

        // 右上角菜单
        btnMenu.setOnClickListener {
            menuMask.alpha = 0f
            menuMask.visibility = View.VISIBLE
            menuMask.animate()
                .alpha(1f)
                .setDuration(200)
                .start()

            val popupView = layoutInflater.inflate(R.layout.popup_menu, null)
            val popupRoot = popupView.findViewById<LinearLayout>(R.id.popupRoot)
            val itemAbout = popupView.findViewById<TextView>(R.id.popupAbout)
            val itemToggle = popupView.findViewById<TextView>(R.id.popupToggleTheme)

            val popupBgColor = if (isDarkTheme) Color.parseColor("#2B2B2B") else Color.parseColor("#FFFFFF")
            val popupTextColor = if (isDarkTheme) Color.parseColor("#FFFFFF") else Color.parseColor("#000000")

            val bg = GradientDrawable()
            bg.shape = GradientDrawable.RECTANGLE
            bg.cornerRadius = dp(8f).toFloat()
            bg.setColor(popupBgColor)
            popupRoot.background = bg

            itemAbout.setTextColor(popupTextColor)
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

            itemAbout.setOnClickListener {
                startActivity(Intent(this, AboutActivity::class.java))
                popup.dismiss()
            }

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

            popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val popupWidth = popupView.measuredWidth

            val loc = IntArray(2)
            btnMenu.getLocationOnScreen(loc)

            val x = loc[0] + btnMenu.width - popupWidth - dp(20f)
            val y = loc[1] + btnMenu.height + dp(8f)

            popup.showAtLocation(rootLayout, Gravity.NO_GRAVITY, x, y)

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
        val bgColor = if (dark) Color.parseColor("#000000") else Color.parseColor("#F7F7F7")
        val mainTextColor = if (dark) Color.parseColor("#FFFFFF") else Color.parseColor("#000000")
        val buttonBgColor = if (dark) Color.parseColor("#2C2C2E") else Color.parseColor("#FFFFFF")

        rootLayout.setBackgroundColor(bgColor)

        colon1.setTextColor(mainTextColor)
        colon2.setTextColor(mainTextColor)

        timerViews.forEach { it.setTextColor(mainTextColor) }

        val btnBg = GradientDrawable()
        btnBg.shape = GradientDrawable.RECTANGLE
        btnBg.cornerRadius = dp(32f).toFloat()
        btnBg.setColor(buttonBgColor)
        btnStart.background = btnBg
        btnStop.background = btnBg

        // 深色下播放/停止图标统一成小米的蓝，浅色保持原来的绿/红
        val playIcon = btnStart.getChildAt(0) as? ImageView
        val stopIcon = btnStop.getChildAt(0) as? ImageView
        if (dark) {
            playIcon?.setColorFilter(Color.parseColor("#2679F9"))
            stopIcon?.setColorFilter(Color.parseColor("#2679F9"))
        } else {
            playIcon?.setColorFilter(Color.parseColor("#2196F3"))
            stopIcon?.setColorFilter(Color.parseColor("#F44336"))
        }

        npHour.setDarkTheme(dark)
        npMinute.setDarkTheme(dark)
        npSecond.setDarkTheme(dark)

        ringView.setDarkTheme(dark)

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

    /**
     * 冒号与滚轮数字的视觉对齐：字体框居中会让冒号看起来偏低，
     * 按「0」和「:」墨迹包围盒的差值上移。
     */
    private fun alignColons() {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = colon1.typeface
            textSize = colon1.textSize
        }
        val colonBounds = Rect()
        val digitBounds = Rect()
        paint.getTextBounds(":", 0, 1, colonBounds)
        paint.getTextBounds("0", 0, 1, digitBounds)

        val dy = ((digitBounds.top + digitBounds.bottom) -
                (colonBounds.top + colonBounds.bottom)) / 2f
        colon1.translationY = dy
        colon2.translationY = dy
    }

    private fun colonOffset(textSizePx: Float): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ResourcesCompat.getFont(this@MainActivity, R.font.robotoregular)
            textSize = textSizePx
        }
        val colonBounds = Rect()
        val digitBounds = Rect()
        paint.getTextBounds(":", 0, 1, colonBounds)
        paint.getTextBounds("0", 0, 1, digitBounds)
        return ((digitBounds.top + digitBounds.bottom) -
                (colonBounds.top + colonBounds.bottom)) / 2f
    }

    private fun setTimerValues(total: Int) {
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        tvHour.text = String.format("%02d", h)
        tvMinute.text = String.format("%02d", m)
        tvSecond.text = String.format("%02d", s)
    }

    private fun dp(value: Float): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun showStartState() {
        isCounting = false
        resetPickerTransition()
        countdownLayout.visibility = View.GONE
        val picker = findViewById<View>(R.id.timePickerLayout)
        picker.visibility = View.VISIBLE
        picker.alpha = 1f
        btnStart.visibility = View.VISIBLE
        btnStop.visibility = View.GONE
    }

    private fun showCountingState() {
        isCounting = true
        btnStart.visibility = View.GONE
        btnStop.visibility = View.VISIBLE

        if (countdownLayout.visibility == View.VISIBLE) {
            // 点数字重开：不重播入场动画
            timerRow.alpha = 1f
            return
        }

        countdownLayout.visibility = View.VISIBLE
        countdownLayout.alpha = 1f
        timerRow.alpha = 0f
        ringView.alpha = 0f
        ringView.scaleX = 0.94f
        ringView.scaleY = 0.94f

        countdownLayout.post {
            clearFlyViews()
            // 用带内边距的临时副本做动画（避免动画层把字形裁掉）
            timerViews.indices.forEach { index ->
                val fly = createFlyView(index)
                addFlyView(fly, wheelCenterX(index), wheelCenterY(index))
                val animation = fly.animate()
                    .translationX(segCenterX(index) - rootLayout.width / 2f)
                    .translationY(segCenterY(index) - rootLayout.height / 2f)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                if (index == timerViews.lastIndex) {
                    animation.withEndAction {
                        if (!isCounting) return@withEndAction
                        clearFlyViews()
                        timerRow.alpha = 1f
                    }
                }
                animation.start()
            }
            findViewById<View>(R.id.timePickerLayout).visibility = View.GONE
            ringView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(340)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }

    private fun showRingingState() {
        isCounting = false
        findViewById<View>(R.id.timePickerLayout).visibility = View.GONE
        countdownLayout.visibility = View.VISIBLE
        countdownLayout.alpha = 1f
        timerRow.alpha = 1f
        ringView.alpha = 1f
        ringView.scaleX = 1f
        ringView.scaleY = 1f
        btnStart.visibility = View.GONE
        btnStop.visibility = View.VISIBLE
    }

    /** 把五段文字摆到滚轮上对应的位置（冒号额外带上对齐偏移） */
    private fun applyWheelStartOffsets() {
        val picker = findViewById<View>(R.id.timePickerLayout)
        val wheels = listOf(npHour, colon1, npMinute, colon2, npSecond)
        timerViews.forEachIndexed { index, view ->
            val base = if (index % 2 == 1) colonOffsetY else 0f
            val naturalX = countdownLayout.left + timerRow.left + view.left + view.width / 2f
            val naturalY = countdownLayout.top + timerRow.top + view.top + view.height / 2f
            val wheelX = picker.left + wheels[index].left + wheels[index].width / 2f
            val wheelY = picker.top + picker.height / 2f + base
            view.translationX = wheelX - naturalX
            view.translationY = wheelY - naturalY
        }
    }

    private fun animateSegmentsToRest() {
        timerViews.forEachIndexed { index, view ->
            val base = if (index % 2 == 1) colonOffsetY else 0f
            view.animate()
                .translationX(0f)
                .translationY(base)
                .setDuration(280)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }

    /** 反向：从圆心飞回滚轮，最后一段动画结束时回调 */
    private fun animateSegmentsToWheels(onEnd: () -> Unit) {
        val picker = findViewById<View>(R.id.timePickerLayout)
        val wheels = listOf(npHour, colon1, npMinute, colon2, npSecond)
        timerViews.forEachIndexed { index, view ->
            val base = if (index % 2 == 1) colonOffsetY else 0f
            val naturalX = countdownLayout.left + timerRow.left + view.left + view.width / 2f
            val naturalY = countdownLayout.top + timerRow.top + view.top + view.height / 2f
            val wheelX = picker.left + wheels[index].left + wheels[index].width / 2f
            val wheelY = picker.top + picker.height / 2f + base
            val animation = view.animate()
                .translationX(wheelX - naturalX)
                .translationY(wheelY - naturalY)
                .setDuration(300)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
            if (index == timerViews.lastIndex) {
                animation.withEndAction { onEnd() }
            }
            animation.start()
        }
    }

    private fun segCenterX(index: Int): Float {
        val view = timerViews[index]
        return countdownLayout.left + timerRow.left + view.left + view.width / 2f
    }

    private fun segCenterY(index: Int): Float {
        val view = timerViews[index]
        val base = if (index % 2 == 1) colonOffsetY else 0f
        return countdownLayout.top + timerRow.top + view.top + view.height / 2f + base
    }

    private fun wheelCenterX(index: Int): Float {
        val picker = findViewById<View>(R.id.timePickerLayout)
        val wheels = listOf(npHour, colon1, npMinute, colon2, npSecond)
        return picker.left + wheels[index].left + wheels[index].width / 2f
    }

    private fun wheelCenterY(index: Int): Float {
        val picker = findViewById<View>(R.id.timePickerLayout)
        val base = if (index % 2 == 1) colonOffsetY else 0f
        return picker.top + picker.height / 2f + base
    }

    private fun createFlyView(index: Int): TextView {
        val source = timerViews[index]
        return TextView(this).apply {
            text = source.text
            setTextColor(source.currentTextColor)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, source.textSize)
            typeface = source.typeface
            includeFontPadding = false
            gravity = Gravity.CENTER
            val pad = dp(6f)
            setPadding(pad, pad, pad, pad)
        }
    }

    private fun addFlyView(view: TextView, centerX: Float, centerY: Float) {
        rootLayout.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
        view.translationX = centerX - rootLayout.width / 2f
        view.translationY = centerY - rootLayout.height / 2f
        flyViews.add(view)
    }

    private fun clearFlyViews() {
        flyViews.forEach { rootLayout.removeView(it) }
        flyViews.clear()
    }

    private fun flyBackToPicker() {
        val picker = findViewById<View>(R.id.timePickerLayout)
        if (countdownLayout.visibility != View.VISIBLE) {
            showStartState()
            return
        }

        // 结束后环上是 00:00:00，先切回设定时长，再按这个数字飞回去
        setTimerValues(totalSeconds)

        ringView.animate()
            .alpha(0f)
            .setDuration(300)
            .start()

        animateSegmentsToWheels {
            countdownLayout.visibility = View.GONE
            ringView.alpha = 1f
            ringView.scaleX = 1f
            ringView.scaleY = 1f
            timerRow.alpha = 1f
            timerViews.forEachIndexed { index, view ->
                view.translationX = 0f
                view.translationY = if (index % 2 == 1) colonOffsetY else 0f
            }
            picker.visibility = View.VISIBLE
            picker.alpha = 1f
            isCounting = false
            btnStop.visibility = View.GONE
            btnStart.visibility = View.VISIBLE
        }
    }

    private fun resetPickerTransition() {
        clearFlyViews()
        timerViews.forEach { it.animate().cancel() }
        countdownLayout.animate().cancel()
        ringView.animate().cancel()
        countdownLayout.alpha = 1f
        ringView.alpha = 1f
        ringView.scaleX = 1f
        ringView.scaleY = 1f
        timerRow.alpha = 1f
        timerViews.forEachIndexed { index, view ->
            view.translationX = 0f
            view.translationY = if (index % 2 == 1) colonOffsetY else 0f
        }
    }

    private fun startCountdown() {
        stopRingtone()
        cancelCountdown()

        val totalMillis = totalSeconds * 1000L
        setTimerValues(totalSeconds)
        ringView.setProgress(1f)
        showCountingState()

        var lastShownSecond = totalSeconds

        // 用逐帧动画驱动圆环，而不是 50ms 定时器：进度按屏幕刷新率更新（60/120Hz）
        val animator = ValueAnimator.ofFloat(totalMillis.toFloat(), 0f)
        animator.duration = totalMillis
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { animation ->
            val remaining = animation.animatedValue as Float
            ringView.setProgress(remaining / totalMillis)

            val seconds = ((remaining + 999f) / 1000f).toInt().coerceIn(0, totalSeconds)
            if (seconds != lastShownSecond) {
                lastShownSecond = seconds
                setTimerValues(seconds)
            }
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (countdownAnimator !== animation) return
                countdownAnimator = null
                ringView.setProgress(0f)
                setTimerValues(0)
                playRingtone()
                showRingingState()
            }
        })
        countdownAnimator = animator
        animator.start()
    }

    private fun cancelCountdown() {
        val animator = countdownAnimator ?: return
        countdownAnimator = null
        animator.removeAllListeners()
        animator.removeAllUpdateListeners()
        animator.cancel()
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
        cancelCountdown()
        flyBackToPicker()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelCountdown()
        ringtone?.stop()
    }
}

