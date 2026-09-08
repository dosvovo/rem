package com.varedog.mirror

import android.app.Activity
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.Gravity
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlin.math.min

class ReceiverActivity : Activity() {

    private lateinit var surfaceView: SurfaceView
    private lateinit var status: TextView
    private val ui = Handler(Looper.getMainLooper())

    @Volatile private var running = false
    private var server: ServerSocket? = null
    private var currentSocket: Socket? = null
    private var acceptThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receiver)
        surfaceView = findViewById(R.id.surface)
        status = findViewById(R.id.status)

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) = startServer(holder)
            override fun surfaceChanged(holder: SurfaceHolder, f: Int, w: Int, h: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) = stopServer()
        })
    }

    private fun startServer(holder: SurfaceHolder) {
        running = true
        acceptThread = Thread {
            try {
                server = ServerSocket(PORT)
            } catch (e: Exception) {
                postStatus("监听失败：${e.message}")
                return@Thread
            }
            while (running) {
                try {
                    postStatus("等待连接…\n本机 IP：${localIp()}")
                    val socket = server!!.accept()
                    currentSocket = socket
                    socket.tcpNoDelay = true
                    serve(socket, holder.surface)
                    currentSocket = null
                } catch (e: Exception) {
                    if (running) postStatus("连接中断，重新等待…")
                }
            }
        }.apply { start() }
    }

    private fun stopServer() {
        running = false
        try { server?.close() } catch (_: Exception) {}
        try { currentSocket?.close() } catch (_: Exception) {}
        try { acceptThread?.join(1500) } catch (_: Exception) {}
        server = null
        acceptThread = null
    }

    private fun serve(socket: Socket, surface: Surface) {
        val input = DataInputStream(BufferedInputStream(socket.getInputStream(), 256 * 1024))

        val headerLen = input.readInt()
        if (headerLen <= 0 || headerLen > 1024 * 1024) throw IllegalStateException("bad header")
        val headerBytes = ByteArray(headerLen)
        input.readFully(headerBytes)
        val json = JSONObject(String(headerBytes))
        val w = json.getInt("w")
        val h = json.getInt("h")
        val csd = Base64.decode(json.getString("csd"), Base64.NO_WRAP)

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
            setByteBuffer("csd-0", java.nio.ByteBuffer.wrap(csd))
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 2 * 1024 * 1024)
        }
        val decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        decoder.configure(format, surface, null, 0)
        decoder.start()
        adjustSurfaceRatio(w, h)
        postStatus("投屏中")

        val inputThread = Thread {
            try {
                while (running) {
                    val n = input.readInt()
                    if (n <= 0 || n > 4 * 1024 * 1024) break
                    val data = ByteArray(n)
                    input.readFully(data)
                    val idx = decoder.dequeueInputBuffer(-1)
                    if (idx >= 0) {
                        val buffer = decoder.getInputBuffer(idx)!!
                        buffer.clear()
                        buffer.put(data)
                        decoder.queueInputBuffer(idx, 0, n, System.nanoTime() / 1000, 0)
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { socket.close() } catch (_: Exception) {}
            }
        }
        val outputThread = Thread {
            val info = MediaCodec.BufferInfo()
            try {
                while (running) {
                    val idx = decoder.dequeueOutputBuffer(info, 10_000)
                    if (idx >= 0) {
                        decoder.releaseOutputBuffer(idx, true)
                    }
                }
            } catch (_: Exception) {
            }
        }
        inputThread.start()
        outputThread.start()
        inputThread.join()

        try { socket.close() } catch (_: Exception) {}
        try { decoder.stop() } catch (_: Exception) {}
        try { decoder.release() } catch (_: Exception) {}
        postStatus("连接已断开，等待重连…")
    }

    private fun adjustSurfaceRatio(w: Int, h: Int) {
        ui.postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed
            val root = surfaceView.parent as? FrameLayout ?: return@postDelayed
            val rw = root.width
            val rh = root.height
            if (rw == 0 || rh == 0) return@postDelayed
            val scale = min(rw.toFloat() / w, rh.toFloat() / h)
            val lp = surfaceView.layoutParams as FrameLayout.LayoutParams
            lp.width = (w * scale).toInt()
            lp.height = (h * scale).toInt()
            lp.gravity = Gravity.CENTER
            surfaceView.layoutParams = lp
        }, 100)
    }

    private fun postStatus(text: String) {
        ui.post { if (!isFinishing && !isDestroyed) status.text = text }
    }

    private fun localIp(): String = try {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .flatMap { it.inetAddresses.asSequence() }
            .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
            ?.hostAddress ?: "未知"
    } catch (_: Exception) {
        "未知"
    }

    companion object {
        const val PORT = 8999
    }
}
