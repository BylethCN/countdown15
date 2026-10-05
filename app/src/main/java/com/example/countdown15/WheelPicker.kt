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
import kotlin.math.min

class WheelPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** 可选值范围 */
    var minValue: Int = 0
    var maxValue: Int = 23

    /** 当前选中值 */
    var value: Int = 0
        private set

    /** 是否循环 */
    var wrapSelectorWheel: Boolean = false

    /** 显示多少行（必须是奇数） */
    private var visibleCount = 5

    /** 每行高度 */
    private var itemHeight = 0f

    /** 字体大小 */
    private var textSize = 56f

    /** 当前滚动偏移（以行数为单位，0 = 正好选中） */
    private var currentOffset = 0f

    /** 触摸相关 */
    private var lastY = 0f
    private var touchDownY = 0f
    private var isDragging = false
    private var velocityTracker: VelocityTracker? = null
    private var minFlingVelocity = 0

    /** 回调 */
    var onValueChangedListener: ((Int) -> Unit)? = null

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.BLACK
        textSize = this@WheelPicker.textSize
    }

    init {
        // 从 attrs 读取可选配置
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.WheelPicker)
            minValue = ta.getInt(R.styleable.WheelPicker_wp_minValue, minValue)
            maxValue = ta.getInt(R.styleable.WheelPicker_wp_maxValue, maxValue)
            value = ta.getInt(R.styleable.WheelPicker_wp_value, value)
            wrapSelectorWheel = ta.getBoolean(R.styleable.WheelPicker_wp_wrap, wrapSelectorWheel)
            visibleCount = ta.getInt(R.styleable.WheelPicker_wp_visibleCount, visibleCount)
            textSize = ta.getDimension(R.styleable.WheelPicker_wp_textSize, textSize)
            textPaint.textSize = textSize
            ta.recycle()
        }
        // 保证 visibleCount 是奇数
        if (visibleCount % 2 == 0) visibleCount += 1
        minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = suggestedMinimumWidth + paddingLeft + paddingRight +
                (textPaint.measureText("00") * 2).toInt()
        // 高度 = 5 行 * 每行高
        itemHeight = textPaint.fontMetrics.let { it.descent - it.ascent } * 1.6f
        val height = (itemHeight * visibleCount).toInt() + paddingTop + paddingBottom
        setMeasuredDimension(
            resolveSize(width, widthMeasureSpec),
            resolveSize(height, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerY = height / 2f
        val half = visibleCount / 2

        // 画中间 5 行
        for (i in -half..half) {
            val pos = currentOffset + i
            val v = valueFromOffset(pos) ?: continue
            val y = centerY + i * itemHeight - (currentOffset * itemHeight)
            drawItem(canvas, v, y, i)
        }
    }

    private fun drawItem(canvas: Canvas, v: Int, y: Float, rowIndex: Int) {
        val text = String.format("%02d", v)
        // 中间行黑色且大小不变；其他行按距离变浅
        val dist = abs(rowIndex)
        val alpha = when (dist) {
            0 -> 255
            1 -> 140
            else -> 70
        }
        textPaint.color = Color.argb(alpha, 0, 0, 0)
        // 绘制文字，y 是基线位置，需要调整
        val fm = textPaint.fontMetrics
        val baseline = y - (fm.ascent + fm.descent) / 2
        canvas.drawText(text, width / 2f, baseline, textPaint)
    }

    /** 给定偏移位置，返回对应的值（考虑 wrap） */
    private fun valueFromOffset(pos: Float): Int? {
        val range = maxValue - minValue + 1
        // 基准值：value - 当前偏移取整
        val base = value - Math.round(currentOffset)
        var idx = base + Math.round(pos - (pos - currentOffset))
        // 用更直接的方法：pos 是相对 value 的行数偏移
        val raw = value + (pos - currentOffset).toInt()
        return if (wrapSelectorWheel) {
            var v = (raw - minValue) % range
            if (v < 0) v += range
            minValue + v
        } else {
            if (raw < minValue || raw > maxValue) null else raw
        }
    }

    private fun valueAtIndex(index: Int): Int {
        val range = maxValue - minValue + 1
        return if (wrapSelectorWheel) {
            var v = (index - minValue) % range
            if (v < 0) v += range
            minValue + v
        } else {
            index.coerceIn(minValue, maxValue)
        }
    }

    // ============= 触摸处理 =============

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain()
        velocityTracker?.addMovement(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                lastY = event.y
                touchDownY = event.y
                isDragging = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - lastY
                lastY = event.y
                currentOffset -= dy / itemHeight
                clampOffset()
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
                velocityTracker?.computeCurrentVelocity(1000)
                val vy = velocityTracker?.yVelocity ?: 0f
                velocityTracker?.recycle()
                velocityTracker = null

                if (abs(vy) > minFlingVelocity) {
                    // 快速滑动 → 惯性
                    fling(-vy / 1000f)
                } else {
                    // 普通滑动 → 吸附到最近行
                    snapToNearest()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun clampOffset() {
        if (!wrapSelectorWheel) {
            val half = visibleCount / 2
            val minOffset = minValue - value - half
            val maxOffset = maxValue - value + half
            currentOffset = currentOffset.coerceIn(minOffset.toFloat(), maxOffset.toFloat())
        }
    }

    private fun snapToNearest() {
        val target = Math.round(currentOffset).toFloat()
        animateOffsetTo(target)
    }

    private fun fling(v: Float) {
        // 简单的惯性：按速度换算成行数，然后吸附
        val delta = v * 6f
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
        val duration = 200L
        val startTime = System.currentTimeMillis()
        post(object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val t = min(1f, elapsed / duration.toFloat())
                // ease out
                val eased = 1 - (1 - t) * (1 - t)
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
        val intTarget = Math.round(target)
        val delta = intTarget - Math.round(currentOffset)
        currentOffset = 0f
        if (delta != 0) {
            var newValue = value + delta
            val range = maxValue - minValue + 1
            if (wrapSelectorWheel) {
                newValue = minValue + ((newValue - minValue) % range + range) % range
            } else {
                newValue = newValue.coerceIn(minValue, maxValue)
            }
            if (newValue != value) {
                value = newValue
                onValueChangedListener?.invoke(value)
            }
        }
        invalidate()
    }

    /** 外部设置值 */
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
