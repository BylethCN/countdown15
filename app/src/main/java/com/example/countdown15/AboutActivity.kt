package com.example.countdown15

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AboutActivity : AppCompatActivity() {

    private lateinit var root: FrameLayout
    private var isDarkTheme = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 读取主题
        val prefs = getSharedPreferences("countdown15_prefs", Context.MODE_PRIVATE)
        isDarkTheme = prefs.getBoolean("dark_theme", false)

        @Suppress("DEPRECATION")
        var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isDarkTheme) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = flags

        setContentView(R.layout.activity_about)

        root = findViewById(R.id.aboutRoot)

        // 应用主题
        val bgColor = if (isDarkTheme) 0xFF000000.toInt() else 0xFFF5F5F5.toInt()
        val textColor = if (isDarkTheme) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        val subTextColor = if (isDarkTheme) 0xFF888888.toInt() else 0xFF999999.toInt()

        root.setBackgroundColor(bgColor)

        findViewById<TextView>(R.id.aboutTitle).setTextColor(textColor)
        findViewById<TextView>(R.id.aboutBack).setTextColor(textColor)
        findViewById<TextView>(R.id.aboutAppName).setTextColor(textColor)
        findViewById<TextView>(R.id.aboutAppVersion).setTextColor(subTextColor)

        findViewById<TextView>(R.id.itemOpenSource).setTextColor(textColor)
        findViewById<TextView>(R.id.itemFeedback).setTextColor(textColor)
        findViewById<TextView>(R.id.itemExportLog).setTextColor(textColor)

        findViewById<TextView>(R.id.aboutAppVersion).text = "v" + getAppVersion()

        // 返回
        findViewById<View>(R.id.aboutBack).setOnClickListener {
            finish()
        }

        // 开源代码
        findViewById<View>(R.id.itemOpenSource).setOnClickListener {
            openUrl("https://github.com/BylethCN/countdown15")
        }

        // 问题反馈
        findViewById<View>(R.id.itemFeedback).setOnClickListener {
            openUrl("https://github.com/BylethCN/countdown15/issues")
        }

        // 导出日志
        findViewById<View>(R.id.itemExportLog).setOnClickListener {
            exportLog()
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开链接", Toast.LENGTH_SHORT).show()
        }
    }

    /** 导出日志到系统下载目录 */
    private fun exportLog() {
        try {
            // 抓取当前应用日志（需要 root 或 READ_LOGS 权限，普通应用只能抓自己进程）
            val log = collectAppLog()
            if (log.isNullOrBlank()) {
                Toast.makeText(this, "无日志可导出", Toast.LENGTH_SHORT).show()
                return
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "countdown15_log_$timestamp.txt"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 用 MediaStore 写入下载目录
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(log.toByteArray())
                    }
                    Toast.makeText(this, "日志已导出到下载目录：$fileName", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "导出失败：无法创建文件", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Android 9 及以下
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloads.exists()) downloads.mkdirs()
                val file = File(downloads, fileName)
                FileOutputStream(file).use { os ->
                    os.write(log.toByteArray())
                }
                Toast.makeText(this, "日志已导出到下载目录：$fileName", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "导出失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** 收集 App 自己进程的日志 */
    private fun collectAppLog(): String {
        val sb = StringBuilder()
        try {
            val process = Runtime.getRuntime().exec("logcat -d -v time")
            val reader = process.inputStream.bufferedReader()
            var line: String?
            val myPid = android.os.Process.myPid().toString()
            while (reader.readLine().also { line = it } != null) {
                line?.let {
                    // 只保留本进程日志，格式包含 pid
                    if (it.contains("($myPid)") || it.contains(" $myPid ")) {
                        sb.append(it).append("\n")
                    }
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (sb.isEmpty()) {
            // 如果抓不到，写一些基本信息
            sb.append("App: 倒计时15秒\n")
            sb.append("Version: ${getAppVersion()}\n")
            sb.append("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            sb.append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            sb.append("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            sb.append("\n（未能抓取 logcat 日志，可能是系统权限限制）\n")
        }
        return sb.toString()
    }

    private fun getAppVersion(): String {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: "0.0.1"
        } catch (e: Exception) {
            "0.0.1"
        }
    }
}