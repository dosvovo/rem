package com.varedog.mirror

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Base64
import android.widget.Toast
import androidx.annotation.Nullable
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket

class ProjectionService : Service() {

    private val ui = Handler(Looper.getMainLooper())
    private var projection: MediaProjection? = null
    private var encoder: MediaCodec? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var socket: Socket? = null
    private var output: DataOutputStream? = null
    private var pumpThread: Thread? = null

    @Volatile private var running = false
    private var width = 0
    private var height = 0

    @Nullable
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE)
            ?: return failAndStop()
        val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT)
            ?: return failAndStop()
        val targetIp = intent.getStringExtra(EXTRA_IP) ?: return failAndStop()

        startForegroundCompat()
        startMirroring(resultCode, resultData, targetIp)
        return START_NOT_STICKY
    }

    private fun startMirroring(resultCode: Int, resultData: Intent, targetIp: String) {
        try {
            val metrics = resources.displayMetrics
            width = metrics.widthPixels
            height = metrics.heightPixels

            val manager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mp = manager.getMediaProjection(resultCode, resultData)
            projection = mp
            mp.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopSelf()
                }
            }, ui)

            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            this.encoder = encoder
            val format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC, width, height
            ).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                )
                setInteger(MediaFormat.KEY_BIT_RATE, 8_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
            }
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()

            virtualDisplay = mp.createVirtualDisplay(
                "mirror", width, height, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                inputSurface, null, null
            )

            val socket = Socket()
            socket.tcpNoDelay = true
            socket.connect(InetSocketAddress(targetIp, ReceiverActivity.PORT), 5000)
            this.socket = socket
            output = DataOutputStream(BufferedOutputStream(socket.getOutputStream(), 256 * 1024))

            encoder.start()

            running = true
            pumpThread = Thread { pump(encoder) }.apply { start() }
        } catch (e: Exception) {
            toast("投屏失败：${e.message}")
            stopSelfWithCleanup()
        }
    }

    private fun pump(encoder: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        var headerSent = false
        try {
            while (running) {
                val idx = encoder.dequeueOutputBuffer(info, 10_000)
                when {
                    idx == MediaCodec.INFO_TRY_AGAIN_LATER -> continue
                    idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> continue
                    idx < 0 -> continue
                    else -> {
                        val buffer = encoder.getOutputBuffer(idx) ?: run {
                            encoder.releaseOutputBuffer(idx, false)
                            continue
                        }
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            if (!headerSent) {
                                val payload = ByteArray(info.size)
                                buffer.get(payload)
                                val json = JSONObject()
                                    .put("w", width)
                                    .put("h", height)
                                    .put("csd", Base64.encodeToString(payload, Base64.NO_WRAP))
                                val header = json.toString().toByteArray(Charsets.UTF_8)
                                output!!.writeInt(header.size)
                                output!!.write(header)
                                output!!.flush()
                                headerSent = true
                            }
                        } else {
                            val chunk = ByteArray(info.size)
                            buffer.get(chunk)
                            output!!.writeInt(chunk.size)
                            output!!.write(chunk)
                            output!!.flush()
                        }
                        encoder.releaseOutputBuffer(idx, false)
                    }
                }
            }
        } catch (e: Exception) {
            if (running) {
                toast("电视连接中断：${e.message}")
                stopSelfWithCleanup()
                stopSelf()
            }
        }
    }

    private fun startForegroundCompat() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "投屏进行中", NotificationManager.IMPORTANCE_LOW)
        )
        val notification: Notification =
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_share)
                .setContentTitle("正在投屏")
                .setContentText("屏幕画面正在发送到电视")
                .setOngoing(true)
                .build()
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIFICATION_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun failAndStop(): Int {
        stopSelfWithCleanup()
        stopSelf()
        return START_NOT_STICKY
    }

    private fun stopSelfWithCleanup() {
        running = false
        try { virtualDisplay?.release() } catch (_: Exception) {}
        try { encoder?.stop() } catch (_: Exception) {}
        try { encoder?.release() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        try { projection?.stop() } catch (_: Exception) {}
        virtualDisplay = null
        encoder = null
        socket = null
        output = null
        projection = null
    }

    override fun onDestroy() {
        stopSelfWithCleanup()
        super.onDestroy()
    }

    private fun toast(text: String) {
        ui.post { Toast.makeText(this, text, Toast.LENGTH_LONG).show() }
    }

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT = "result"
        const val EXTRA_IP = "ip"
        private const val CHANNEL_ID = "mirror_service"
        private const val NOTIFICATION_ID = 2001
    }
}
