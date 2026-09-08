package com.varedog.reminder

import android.app.Application

class ReminderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
    }
}
