package com.example.countdown15

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
    private var velocityTracker: VelocityTracker? = null
    private var minFlingVelocity = 0

    var onValueChangedListener: ((Int) -> Unit)? = null

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.BLACK
    }

    init {
        isClickable = true
        isFocusable = true

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
        minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        textPaint.textSize = textSize
        val fm = textPaint.fontMetrics
        itemHeight = (fm.descent - fm.ascent) * 1.4f

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

        // 根据当前偏移，算出屏幕范围内需要的所有整数 k
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
        textPaint.color = Color.argb(alpha, 0, 0, 0)

        val fm = textPaint.fontMetrics
        val baseline = y - (fm.ascent + fm.descent) / 2
        canvas.drawText(text, width / 2f, baseline, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain()
        velocityTracker?.addMovement(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                lastY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - lastY
                lastY = event.y
                currentOffset += dy / itemHeight
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

    private fun snapToNearest() {
        val target = Math.round(currentOffset).toFloat()
        animateOffsetTo(target)
    }

    private fun fling(v: Float) {
        // 系数 2f，单次最多 6 格，防止甩出很远
        var delta = v * 2f
        delta = delta.coerceIn(-6f, 6f)
        val target = Math.round(currentOffset + delta).toFloat()
        animateOffsetTo(target)
    }

    private fun animateOffsetTo(target: Float) {
        val start = currentOffset
        val diff = target - start
        if (abs(diff) < 0.01f) {
            applyFinalOffset(target)
            return
        }
        // 时长随距离动态增长，带来明显减速感
        val distance = abs(diff)
        val duration = (200 + (distance * 60).toInt()).coerceIn(200, 600).toLong()

        val startTime = System.currentTimeMillis()
        post(object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val t = min(1f, elapsed / duration.toFloat())
                // 三次缓出，减速感更强
                val eased = 1 - (1 - t) * (1 - t) * (1 - t)
                currentOffset = start + diff * eased
                invalidate()
                if (t < 1f) {
                    post(this)
                } else {
                    applyFinalOffset(target)
                }
            }
        })
    }

    private fun applyFinalOffset(target: Float) {
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
}