package com.varedog.mirror

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.net.Inet4Address
import java.net.NetworkInterface

class SenderActivity : Activity() {

    private lateinit var ipInput: EditText
    private lateinit var status: TextView
    private var pendingIp = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sender)

        findViewById<TextView>(R.id.text_local_ip).text = "本机 IP：${localIp()}"
        ipInput = findViewById(R.id.input_ip)
        status = findViewById(R.id.status)

        val prefs = getSharedPreferences("mirror_prefs", Context.MODE_PRIVATE)
        ipInput.setText(prefs.getString("last_ip", ""))

        findViewById<Button>(R.id.button_start).setOnClickListener { onStartClicked() }
        findViewById<Button>(R.id.button_stop).setOnClickListener {
            stopService(Intent(this, ProjectionService::class.java))
            status.text = "已停止"
        }
    }

    private fun onStartClicked() {
        val ip = ipInput.text.toString().trim()
        if (ip.isEmpty()) {
            Toast.makeText(this, "先输入电视的 IP", Toast.LENGTH_SHORT).show()
            return
        }
        pendingIp = ip
        getSharedPreferences("mirror_prefs", Context.MODE_PRIVATE)
            .edit().putString("last_ip", ip).apply()

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            return
        }
        startCapture()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) startCapture()
    }

    private fun startCapture() {
        val manager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 1001) return
        if (resultCode != RESULT_OK || data == null) {
            status.text = "未授权屏幕采集"
            return
        }
        val service = Intent(this, ProjectionService::class.java)
            .putExtra(ProjectionService.EXTRA_RESULT_CODE, resultCode)
            .putExtra(ProjectionService.EXTRA_RESULT, data)
            .putExtra(ProjectionService.EXTRA_IP, pendingIp)
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(service)
        } else {
            startService(service)
        }
        status.text = "投屏中：$pendingIp（可退出本页面）"
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
