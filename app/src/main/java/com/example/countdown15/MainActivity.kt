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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTimer = findViewById(R.id.tvTimer)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)

        // 一打开只显示 15，不自动倒计时
        tvTimer.text = "15"
        btnStart.visibility = Button.VISIBLE
        btnStop.visibility = Button.GONE

        btnStart.setOnClickListener {
            startCountdown()
        }

        btnStop.setOnClickListener {
            stopRingtone()
        }
    }

    private fun startCountdown() {
        stopRingtone()

        timer?.cancel()
        tvTimer.text = "15"

        btnStop.visibility = Button.GONE
        btnStart.visibility = Button.VISIBLE
        btnStart.isEnabled = false

        timer = object : CountDownTimer(15_000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt() + 1
                tvTimer.text = seconds.toString()
            }

            override fun onFinish() {
                tvTimer.text = "0"
                playRingtone()

                btnStart.visibility = Button.GONE
                btnStop.visibility = Button.VISIBLE
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

        btnStop.visibility = Button.GONE
        btnStart.visibility = Button.VISIBLE
        btnStart.isEnabled = true
        tvTimer.text = "15"
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        ringtone?.stop()
    }
}
