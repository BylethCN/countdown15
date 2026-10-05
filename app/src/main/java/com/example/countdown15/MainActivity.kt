package com.example.countdown15

import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.FrameLayout
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

        tvVersion.text = "v" + getAppVersion()

        // 初始值
        npHour.setValue(0)
        npMinute.setValue(0)
        npSecond.setValue(15)

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
