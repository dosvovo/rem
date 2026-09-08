package com.varedog.reminder

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.runBlocking

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val problems = StringBuilder()

        CrashHandler.consume(this)?.let {
            problems.append("上次崩溃日志：\n").append(it)
        }

        try {
            LayoutInflater.from(this).inflate(R.layout.activity_main, null)
        } catch (t: Throwable) {
            problems.append("\n主界面布局加载失败：\n").append(Log.getStackTraceString(t))
        }

        try {
            runBlocking {
                AppDatabase.get(this@LauncherActivity).reminderDao()
                    .getUpcoming(System.currentTimeMillis())
            }
        } catch (t: Throwable) {
            problems.append("\n数据库读取失败：\n").append(Log.getStackTraceString(t))
        }

        if (problems.isNotBlank()) {
            startActivity(
                Intent(this, CrashReportActivity::class.java)
                    .putExtra(CrashReportActivity.EXTRA_TEXT, problems.toString())
            )
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
