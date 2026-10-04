package com.example.countdown15

import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvTimer: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    private var timer: CountDownTimer? = null
    private var ringtone: Ringtone? = null

    // 标记当前是否正在倒计时，用于判断点屏幕要不要重新计时
    private var isCounting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTimer = findViewById(R.id.tvTimer)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)

        // 初始状态：只显示开始按钮
        showStartState()

        btnStart.setOnClickListener {
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

    /** 初始 / 停止后：只显示开始按钮 */
    private fun showStartState() {
        isCounting = false
        tvTimer.visibility = TextView.GONE
        btnStop.visibility = Button.GONE
        btnStart.visibility = Button.VISIBLE
    }

    /** 倒计时中：只显示数字 */
    private fun showCountingState() {
        isCounting = true
        btnStart.visibility = Button.GONE
        btnStop.visibility = Button.GONE
        tvTimer.visibility = TextView.VISIBLE
    }

    /** 响铃中：只显示停止按钮 */
    private fun showRingingState() {
        isCounting = false
        tvTimer.visibility = TextView.GONE
        btnStart.visibility = Button.GONE
        btnStop.visibility = Button.VISIBLE
    }

    private fun startCountdown() {
        stopRingtone()

        timer?.cancel()
        showCountingState()
        tvTimer.text = "15"

        timer = object : CountDownTimer(15_000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt() + 1
                tvTimer.text = seconds.toString()
            }

            override fun onFinish() {
                playRingtone()
                showRingingState()
            }
        }.start()
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
