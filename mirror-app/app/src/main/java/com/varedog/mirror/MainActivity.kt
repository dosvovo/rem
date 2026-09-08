package com.varedog.mirror

import android.app.Activity
import android.content.Intent
import java.net.Inet4Address
import java.net.NetworkInterface
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.text_ip).text = "本机 IP：${localIp()}"
        findViewById<Button>(R.id.button_receiver).setOnClickListener {
            startActivity(Intent(this, ReceiverActivity::class.java))
        }
        findViewById<Button>(R.id.button_sender).setOnClickListener {
            startActivity(Intent(this, SenderActivity::class.java))
        }
    }

    private fun localIp(): String = try {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .flatMap { it.inetAddresses.asSequence() }
            .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
            ?.hostAddress ?: "未知"
    } catch (_: Exception) {
        "未知"
    }
}
