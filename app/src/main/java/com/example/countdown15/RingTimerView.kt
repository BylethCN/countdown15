package com.example.countdown15

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 倒计时外圈：大圆面 + 内缩一圈的轨道 + 蓝色剩余进度弧 + 弧末端小圆点。
 *
 * 大圆（#FAFAFA）比轨道再外扩 8dp，阴影只画在大圆外侧。
 * 阴影先在 1/4 分辨率的位图里渲染，再双线性放大，避免低透明度渐变在 8bit 下
 * 出现一块块的色带。
 */
class RingTimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val strokeWidth = 4.2f * density
    private val knobRadius = 3.6f * density

    /** 四周留白，必须大于阴影扩散半径 */
    private val inset = 64f * density

    /** 大圆比轨道多出来的半径 */
    private val discOutset = 8f * density

    val ringInset: Int
        get() = inset.toInt()

    /** 弧末端与圆点之间的角度空隙 */
    private val gapDegrees = 7f

    private val trackColorLight = Color.parseColor("#EDEDED")
    private val trackColorDark = Color.parseColor("#383838")
    private val discColorLight = Color.parseColor("#FAFAFA")
    private val discColorDark = Color.parseColor("#272727")
    private val accentColor = Color.parseColor("#3481FF")
    private val accentColorDark = Color.parseColor("#2679F9")

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@RingTimerView.strokeWidth
        strokeCap = Paint.Cap.ROUND
    }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = accentColor
    }
    private val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    /** 圆点底下的“垫圈”，用圆盘底色把弧线切开，让点的左右两侧都留出空隙 */
    private val knobHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isDither = true
    }
    private val shadowBitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

    private val arcRect = RectF()
    private var shadowBitmap: Bitmap? = null
    private var shadowOuter = 0f

    /** 剩余时间占比，1 = 满圈 */
    private var progress = 1f
    private var isDark = false

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidate()
    }

    fun setDarkTheme(dark: Boolean) {
        isDark = dark
        knobPaint.color = if (dark) accentColorDark else accentColor
        buildShadow()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildShadow()
    }

    private fun ringRadius(): Float =
        min(width, height) / 2f - inset - strokeWidth / 2f

    private fun discRadius(): Float = ringRadius() + discOutset

    private fun buildShadow() {
        val discR = discRadius()
        if (discR <= 0f) return

        shadowOuter = discR + 46f * density
        val cx = width / 2f
        val cy = height / 2f

        // 阴影从圆边立刻开始（不留空隙），起点最强，向外线性淡出
        val edge = (discR / shadowOuter).coerceIn(0f, 0.95f)
        shadowPaint.shader = RadialGradient(
            cx, cy, shadowOuter,
            intArrayOf(
                Color.TRANSPARENT,
                Color.argb(if (isDark) 90 else 12, 0, 0, 0),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, edge, 1f),
            Shader.TileMode.CLAMP
        )

        // 1/4 分辨率渲染 + 放大，抹掉 8bit 渐变的色带
        val scale = 4
        val bw = max(1, width / scale)
        val bh = max(1, height / scale)
        val bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(1f / scale, 1f / scale)
        canvas.drawCircle(cx, cy, shadowOuter, shadowPaint)

        shadowBitmap?.recycle()
        shadowBitmap = bitmap
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = ringRadius()
        if (radius <= 0f) return

        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)

        shadowBitmap?.let {
            canvas.drawBitmap(it, null, Rect(0, 0, width, height), shadowBitmapPaint)
        }

        // 大圆
        discPaint.color = if (isDark) discColorDark else discColorLight
        canvas.drawCircle(cx, cy, discRadius(), discPaint)

        // 内缩的轨道
        ringPaint.color = if (isDark) trackColorDark else trackColorLight
        canvas.drawCircle(cx, cy, radius, ringPaint)

        if (progress <= 0.001f) return

        val sweep = 360f * progress
        ringPaint.color = if (isDark) accentColorDark else accentColor
        canvas.drawArc(arcRect, START_ANGLE, sweep, false, ringPaint)

        val radians = Math.toRadians((START_ANGLE + sweep).toDouble())
        val knobX = cx + radius * cos(radians).toFloat()
        val knobY = cy + radius * sin(radians).toFloat()

        // 圆点先垫一圈底色，让它的左右两边都和弧线留出同样的空隙
        knobHaloPaint.color = if (isDark) discColorDark else discColorLight
        canvas.drawCircle(knobX, knobY, knobRadius + 3.5f * density, knobHaloPaint)
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        shadowBitmap?.recycle()
        shadowBitmap = null
    }

    private companion object {
        const val START_ANGLE = -90f
    }
}
