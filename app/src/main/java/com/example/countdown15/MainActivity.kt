package com.example.countdown15

import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvTimer: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var timePicker: TimePicker

    private var timer: CountDownTimer? = null
    private var ringtone: Ringtone? = null

    private var isCounting = false

    // 当前设置的总秒数（默认 15 秒）
    private var totalSeconds = 15

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTimer = findViewById(R.id.tvTimer)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        timePicker = findViewById(R.id.timePicker)

        // 强制显示秒
        timePicker.setIs24HourView(true)
        try {
            timePicker.setHour(0)
            timePicker.setMinute(0)
            // 秒在部分系统上要通过反射或 XML 属性支持，这里先设默认
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 初始状态：显示时间选择器和开始按钮
        showStartState()

        btnStart.setOnClickListener {
            // 从 TimePicker 读取用户设置的时间
            totalSeconds = readTimePickerSeconds()
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

    /** 从 TimePicker 读取时、分、秒，换算成总秒数 */
    private fun readTimePickerSeconds(): Int {
        val hour = timePicker.hour
        val minute = timePicker.minute
        val second = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            timePicker.second
        } else {
            0
        }
        return hour * 3600 + minute * 60 + second
    }

    /** 初始 / 停止后：显示时间选择器和开始按钮 */
    private fun showStartState() {
        isCounting = false
        tvTimer.visibility = TextView.GONE
        btnStop.visibility = Button.GONE
        timePicker.visibility = TimePicker.VISIBLE
        btnStart.visibility = Button.VISIBLE
    }

    /** 倒计时中：只显示数字 */
    private fun showCountingState() {
        isCounting = true
        timePicker.visibility = TimePicker.GONE
        btnStart.visibility = Button.GONE
        btnStop.visibility = Button.GONE
        tvTimer.visibility = TextView.VISIBLE
    }

    /** 响铃中：只显示停止按钮 */
    private fun showRingingState() {
        isCounting = false
        timePicker.visibility = TimePicker.GONE
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

    /** 把秒数格式化成 时:分:秒 */
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
