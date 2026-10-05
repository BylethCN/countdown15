package com.example.countdown15

import android.graphics.Color
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvTimer: TextView
    private lateinit var tvVersion: TextView
    private lateinit var btnStart: FrameLayout
    private lateinit var btnStop: FrameLayout
    private lateinit var npHour: NumberPicker
    private lateinit var npMinute: NumberPicker
    private lateinit var npSecond: NumberPicker

    private var timer: CountDownTimer? = null
    private var ringtone: Ringtone? = null

    private var isCounting = false
    private var totalSeconds = 15

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTimer = findViewById(R.id.tvTimer)
        tvVersion = findViewById(R.id.tvVersion)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        npHour = findViewById(R.id.npHour)
        npMinute = findViewById(R.id.npMinute)
        npSecond = findViewById(R.id.npSecond)

        // 版本号
        tvVersion.text = "v" + getAppVersion()

        // 初始化滚轮
        npHour.minValue = 0
        npHour.maxValue = 99
        npHour.value = 0
        npHour.wrapSelectorWheel = true

        npMinute.minValue = 0
        npMinute.maxValue = 59
        npMinute.value = 0
        npMinute.wrapSelectorWheel = true

        npSecond.minValue = 0
        npSecond.maxValue = 59
        npSecond.value = 15
        npSecond.wrapSelectorWheel = true

        setPickerFormatter(npHour)
        setPickerFormatter(npMinute)
        setPickerFormatter(npSecond)

        // 白色文字、加大字号、去掉上下渐变边界
        listOf(npHour, npMinute, npSecond).forEach { picker ->
            picker.setBackgroundColor(Color.TRANSPARENT)
            customizeNumberPickerText(picker)
        }

        showStartState()

        btnStart.setOnClickListener {
            totalSeconds = npHour.value * 3600 + npMinute.value * 60 + npSecond.value
            if (totalSeconds <= 0) totalSeconds = 1
            startCountdown()
        }

        btnStop.setOnClickListener {
            stopRingtoneAndReset()
        }

        findViewById<View>(android.R.id.content).setOnClickListener {
            if (isCounting) {
                startCountdown()
            }
        }
    }

    /** 通过反射改 NumberPicker 内部 EditText 的颜色和字号 */
    private fun customizeNumberPickerText(picker: NumberPicker) {
        try {
            val f = NumberPicker::class.java.getDeclaredField("mInputText")
            f.isAccessible = true
            val et = f.get(picker) as EditText
            et.setTextColor(Color.BLACK)
            et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 56f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getAppVersion(): String {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: "0.0.1"
        } catch (e: Exception) {
            "0.0.1"
        }
    }

    private fun setPickerFormatter(picker: NumberPicker) {
        picker.setFormatter { value -> String.format("%02d", value) }
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
        btnStop.visibility = View.GONE
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
