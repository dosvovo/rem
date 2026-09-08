package com.varedog.reminder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.abs
import kotlin.math.min

class OverlayTextView(
    context: Context,
    private val onClose: () -> Unit
) : AppCompatTextView(context) {

    private var windowManager: WindowManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var pressStartMillis = 0L
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var dragging = false

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = 0xFFFFC107.toInt()
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = 0x55FFFFFF
    }

    init {
        val density = resources.displayMetrics.density
        setTextColor(Color.WHITE)
        textSize = 15f
        setShadowLayer(4f, 0f, 1f, Color.BLACK)
        setLineSpacing(density * 2f, 1f)
        val pad = (density * 12).toInt()
        setPadding(pad, pad, pad, pad)
        maxWidth = (density * 280).toInt()
        background = GradientDrawable().apply {
            cornerRadius = density * 14f
            setColor(0xE6181818.toInt())
        }
    }

    fun attach(windowManager: WindowManager, params: WindowManager.LayoutParams) {
        this.windowManager = windowManager
        this.layoutParams = params
        windowManager.addView(this, params)
    }

    fun detach() {
        try {
            windowManager?.removeView(this)
        } catch (_: Exception) {
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (pressStartMillis == 0L) return
        val elapsed = SystemClock.elapsedRealtime() - pressStartMillis
        val fraction = min(1f, elapsed / CLOSE_DURATION_MILLIS.toFloat())
        val density = resources.displayMetrics.density
        val radius = 9f * density
        val cx = width - radius - 4f * density
        val cy = radius + 4f * density
        ringPaint.strokeWidth = 2.5f * density
        trackPaint.strokeWidth = 2.5f * density
        canvas.drawCircle(cx, cy, radius, trackPaint)
        if (fraction > 0f) {
            canvas.drawArc(
                cx - radius, cy - radius, cx + radius, cy + radius,
                -90f, 360f * fraction, false, ringPaint
            )
        }
        if (elapsed < CLOSE_DURATION_MILLIS) {
            postInvalidateDelayed(50)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressStartMillis = SystemClock.elapsedRealtime()
                lastRawX = event.rawX
                lastRawY = event.rawY
                dragging = false
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val pressed = pressStartMillis != 0L
                val dx = event.rawX - lastRawX
                val dy = event.rawY - lastRawY
                if (pressed && (dragging || abs(dx) > TOUCH_SLOP || abs(dy) > TOUCH_SLOP)) {
                    dragging = true
                    val params = layoutParams ?: return true
                    params.x += dx.toInt()
                    params.y += dy.toInt()
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                    try {
                        windowManager?.updateViewLayout(this, params)
                    } catch (_: Exception) {
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val elapsed = SystemClock.elapsedRealtime() - pressStartMillis
                pressStartMillis = 0L
                dragging = false
                invalidate()
                if (elapsed >= CLOSE_DURATION_MILLIS) {
                    performHapticFeedbackSafe()
                    onClose()
                    return true
                }
            }
        }
        return true
    }

    private fun performHapticFeedbackSafe() {
        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
    }

    companion object {
        const val CLOSE_DURATION_MILLIS = 5000L
        private const val TOUCH_SLOP = 6f

        fun buildLayoutParams(context: Context): WindowManager.LayoutParams {
            val metrics = context.resources.displayMetrics
            val density = metrics.density
            return WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (metrics.widthPixels - 40 * density).toInt()
                y = (200 * density).toInt()
            }
        }
    }
}
