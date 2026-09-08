package com.varedog.reminder

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class CrashHandler(private val appContext: Context) : Thread.UncaughtExceptionHandler {

    private val default = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val text = buildString {
                append("Thread: ").append(thread.name).append('\n')
                append(Log.getStackTraceString(throwable))
            }
            File(appContext.filesDir, CRASH_FILE).writeText(text)
        } catch (_: Throwable) {
        }
        default?.uncaughtException(thread, throwable)
    }

    companion object {
        private const val CRASH_FILE = "crash.txt"

        fun install(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context.applicationContext))
        }

        fun consume(context: Context): String? {
            val file = File(context.filesDir, CRASH_FILE)
            if (!file.exists()) return null
            val text = runCatching { file.readText() }.getOrNull()
            file.delete()
            return text?.takeIf { it.isNotBlank() }
        }
    }
}

class CrashReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this).apply {
            setPadding(48, 64, 48, 48)
            setTextIsSelectable(true)
            text = intent.getStringExtra(EXTRA_TEXT) ?: "无日志"
            textSize = 12f
        }
        setContentView(ScrollView(this).apply { addView(tv) })
        title = "错误详情（点内容任意处可选中复制，返回键关闭）"
    }

    companion object {
        const val EXTRA_TEXT = "text"
    }
}
