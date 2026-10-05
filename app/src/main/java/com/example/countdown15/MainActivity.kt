package com.example.countdown15

import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvTimer: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
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
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        npHour = findViewById(R.id.npHour)
        npMinute = findViewById(R.id.npMinute)
        npSecond = findViewById(R.id.npSecond)

        // 初始化三个滚轮
        // 小时：0~99
        npHour.minValue = 0
        npHour.maxValue = 99
        npHour.value = 0
        npHour.wrapSelectorWheel = true

        // 分钟：0~59
        npMinute.minValue = 0
        npMinute.maxValue = 59
        npMinute.value = 0
        npMinute.wrapSelectorWheel = true

        // 秒：0~59，默认 15
        npSecond.minValue = 0
        npSecond.maxValue = 59
        npSecond.value = 15
        npSecond.wrapSelectorWheel = true

        // 让数字两位显示
        setPickerFormatter(npHour)
        setPickerFormatter(npMinute)
        setPickerFormatter(npSecond)

        showStartState()

        btnStart.setOnClickListener {
            totalSeconds = npHour.value * 3600 + npMinute.value * 60 + npSecond.value
            if (totalSeconds <= 0) totalSeconds = 1
            startCountdown()
        }

        btnStop.setOnClickListener {
            stopRingtoneAndReset()
        }

        // 点屏幕任意位置：只有倒计时中才重新计时
        findViewById<android.view.View>(android.R.id.content).setOnClickListener {
            if (isCounting) {
                startCountdown()
            }
        }
    }

    /** 让滚轮数字显示成 00、01 这种两位格式 */
    private fun setPickerFormatter(picker: NumberPicker) {
        picker.setFormatter { value -> String.format("%02d", value) }
    }

    /** 初始 / 停止后：显示时间选择和开始按钮 */
    private fun showStartState() {
        isCounting = false
        tvTimer.visibility = TextView.GONE
        btnStop.visibility = Button.GONE
        findViewById<android.view.View>(R.id.timePickerLayout).visibility = android.view.View.VISIBLE
        btnStart.visibility = Button.VISIBLE
    }

    /** 倒计时中：只显示数字 */
    private fun showCountingState() {
        isCounting = true
        findViewById<android.view.View>(R.id.timePickerLayout).visibility = android.view.View.GONE
        btnStart.visibility = Button.GONE
        btnStop.visibility = Button.GONE
        tvTimer.visibility = TextView.VISIBLE
    }

    /** 响铃中：只显示停止按钮 */
    private fun showRingingState() {
        isCounting = false
        findViewById<android.view.View>(R.id.timePickerLayout).visibility = android.view.View.GONE
        tvTimer.visibility = TextView.GONE
        btnStart.visibility = Button.GONE
        btnStop.visibility = Button.VISIBLE
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
