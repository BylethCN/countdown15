package com.example.countdown15

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

class WheelPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var minValue: Int = 0
    var maxValue: Int = 23

    var value: Int = 0
        private set

    var wrapSelectorWheel: Boolean = false

    private var visibleCount = 5
    private var itemHeight = 0f
    private var textSize = 56f
    private var currentOffset = 0f

    private var lastY = 0f
    private var dragDistance = 0f
    private var velocityTracker: VelocityTracker? = null
    private var minFlingVelocity = 0

    private var animRunnable: Runnable? = null

    private var vibrator: Vibrator? = null
    private var lastRoundedOffset = 0
    private var lastVibrateTime = 0L

    /** 是否黑主题 */
    private var isDark = false

    var onValueChangedListener: ((Int) -> Unit)? = null

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.BLACK
    }

    init {
        isClickable = true
        isFocusable = true

        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        // 用 Roboto-Regular 字体
        try {
            androidx.core.content.res.ResourcesCompat.getFont(context, R.font.robotoregular)?.let {
                textPaint.typeface = it
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.WheelPicker)
            minValue = ta.getInt(R.styleable.WheelPicker_wp_minValue, minValue)
            maxValue = ta.getInt(R.styleable.WheelPicker_wp_maxValue, maxValue)
            value = ta.getInt(R.styleable.WheelPicker_wp_value, value)
            wrapSelectorWheel = ta.getBoolean(R.styleable.WheelPicker_wp_wrap, wrapSelectorWheel)
            visibleCount = ta.getInt(R.styleable.WheelPicker_wp_visibleCount, visibleCount)
            textSize = ta.getDimension(R.styleable.WheelPicker_wp_textSize, textSize)
            ta.recycle()
        }
        if (visibleCount % 2 == 0) visibleCount += 1
        minFlingVelocity = (ViewConfiguration.get(context).scaledMinimumFlingVelocity * 4.0f).toInt()
    }

    /** 主题切换：黑主题时文字为白色 */
    fun setDarkTheme(dark: Boolean) {
        isDark = dark
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        textPaint.textSize = textSize
        val fm = textPaint.fontMetrics
        // 行高系数 0.80
        itemHeight = (fm.descent - fm.ascent) * 0.80f

        val width = (90 * resources.displayMetrics.density).toInt()
        var height = (itemHeight * visibleCount).toInt() + paddingTop + paddingBottom
        if (height < 100) {
            height = (itemHeight * visibleCount).toInt().coerceAtLeast(300)
        }

        setMeasuredDimension(
            resolveSize(width, widthMeasureSpec),
            resolveSize(height, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerY = height / 2f

        val minK = floor(((0f - centerY) / itemHeight - currentOffset).toDouble()).toInt() - 1
        val maxK = ceil(((height - centerY) / itemHeight - currentOffset).toDouble()).toInt() + 1

        for (k in minK..maxK) {
            val y = centerY + (k + currentOffset) * itemHeight

            val displayValue = value + k
            val v = normalizeValue(displayValue) ?: continue

            val distFromCenter = abs(k + currentOffset)
            val t = (distFromCenter / 2f).coerceIn(0f, 1f)
            val scale = 1.0f - t * 0.45f
            val alpha = (255 - t * 195f).toInt()

            drawItem(canvas, v, y, scale, alpha)
        }
    }

    private fun normalizeValue(raw: Int): Int? {
        return if (wrapSelectorWheel) {
            val range = maxValue - minValue + 1
            var v = (raw - minValue) % range
            if (v < 0) v += range
            minValue + v
        } else {
            if (raw < minValue || raw > maxValue) null else raw
        }
    }

    private fun drawItem(canvas: Canvas, v: Int, y: Float, scale: Float, alpha: Int) {
        val text = String.format("%02d", v)
        textPaint.textSize = textSize * scale
        // 黑主题用白色，白主题用黑色
        val base = if (isDark) 255 else 0
        textPaint.color = Color.argb(alpha, base, base, base)

        val fm = textPaint.fontMetrics
        val baseline = y - (fm.ascent + fm.descent) / 2
        canvas.drawText(text, width / 2f, baseline, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain()
        velocityTracker?.addMovement(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                animRunnable?.let { removeCallbacks(it) }
                animRunnable = null
                applyFinalOffset(currentOffset)
                lastRoundedOffset = Math.round(currentOffset)
                parent?.requestDisallowInterceptTouchEvent(true)
                lastY = event.y
                dragDistance = 0f
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - lastY
                lastY = event.y
                currentOffset += dy / itemHeight
                dragDistance += abs(dy)
                checkVibration()
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                velocityTracker?.computeCurrentVelocity(1000)
                val vy = velocityTracker?.yVelocity ?: 0f
                velocityTracker?.recycle()
                velocityTracker = null

                if (abs(vy) > minFlingVelocity) {
                    fling(-vy / 1000f)
                } else {
                    snapToNearest()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun checkVibration() {
        val rounded = Math.round(currentOffset)
        if (rounded == lastRoundedOffset) return
        lastRoundedOffset = rounded
        vibrate()
    }

    private fun vibrate() {
        try {
            val vb = vibrator ?: return
            if (!vb.hasVibrator()) return
            val now = System.currentTimeMillis()
            if (now - lastVibrateTime < 30L) return
            lastVibrateTime = now
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vb.vibrate(VibrationEffect.createOneShot(3, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vb.vibrate(3)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun snapToNearest() {
        val target = Math.round(currentOffset).toFloat()
        animateOffsetTo(target)
    }

    private fun fling(v: Float) {
        val deltaBySpeed = -v * 2.0f
        val deltaByDistance = dragDistance / itemHeight * 0.15f
        val sign = if (deltaBySpeed >= 0) 1f else -1f
        var delta = deltaBySpeed + sign * deltaByDistance
        delta = delta.coerceIn(-25f, 25f)
        val target = Math.round(currentOffset + delta).toFloat()
        animateOffsetTo(target)
    }

    private fun animateOffsetTo(target: Float) {
        animRunnable?.let { removeCallbacks(it) }
        animRunnable = null

        val start = currentOffset
        val diff = target - start
        if (abs(diff) < 0.01f) {
            applyFinalOffset(target)
            return
        }
        val distance = abs(diff)
        val duration = (500 + (distance * 110).toInt()).coerceIn(500, 3000).toLong()

        val startTime = System.currentTimeMillis()
        val runnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val t = min(1f, elapsed / duration.toFloat())
                val u = when {
                    t < 0.533f -> (t / 0.533f) * 0.917f
                    t < 0.667f -> 0.917f + ((t - 0.533f) / 0.134f) * 0.041f
                    else -> 0.958f + ((t - 0.667f) / 0.333f) * 0.042f
                }
                val eased = 1 - (1 - u) * (1 - u) * (1 - u) * (1 - u)
                currentOffset = start + diff * eased
                checkVibration()
                invalidate()
                if (t < 1f) {
                    post(this)
                } else {
                    applyFinalOffset(target)
                    animRunnable = null
                }
            }
        }
        animRunnable = runnable
        post(runnable)
    }

    private fun applyFinalOffset(target: Float) {
        checkVibration()
        val delta = -Math.round(target)
        currentOffset = 0f
        if (delta != 0) {
            var newValue = value + delta
            val range = maxValue - minValue + 1
            if (wrapSelectorWheel) {
                newValue = minValue + ((newValue - minValue) % range + range) % range
            } else {
                newValue = newValue.coerceIn(minValue, maxValue)
            }
            value = newValue
            onValueChangedListener?.invoke(value)
        }
        invalidate()
    }

    fun setValue(v: Int) {
        val range = maxValue - minValue + 1
        value = if (wrapSelectorWheel) {
            minValue + ((v - minValue) % range + range) % range
        } else {
            v.coerceIn(minValue, maxValue)
        }
        currentOffset = 0f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animRunnable?.let { removeCallbacks(it) }
        animRunnable = null
    }
}