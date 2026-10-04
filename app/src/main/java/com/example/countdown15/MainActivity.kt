package com.example.countdown15

import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvTimer: TextView
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTimer = findViewById(R.id.tvTimer)

        findViewById<android.view.View>(android.R.id.content).setOnClickListener {
            startCountdown()
        }

        startCountdown()
    }

    private fun startCountdown() {
        timer?.cancel()
        tvTimer.text = "15"

        timer = object : CountDownTimer(15_000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt() + 1
                tvTimer.text = seconds.toString()
            }

            override fun onFinish() {
                tvTimer.text = "0"
                playRingtone()
            }
        }.start()
    }

    private fun playRingtone() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(applicationContext, uri).play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
