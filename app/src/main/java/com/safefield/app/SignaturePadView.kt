package com.safefield.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SignaturePadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val path = Path()
    private var hasDrawing = false
    private var lastX = 0f
    private var lastY = 0f
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(37, 99, 235)
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val bg = Paint().apply { color = Color.WHITE }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)
        canvas.drawPath(path, stroke)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                path.moveTo(lastX, lastY)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = kotlin.math.abs(event.x - lastX)
                val dy = kotlin.math.abs(event.y - lastY)
                if (dx > 1f || dy > 1f) {
                    path.lineTo(event.x, event.y)
                    hasDrawing = true
                    lastX = event.x
                    lastY = event.y
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun clearPad() {
        path.reset()
        hasDrawing = false
        invalidate()
    }

    fun isEmpty(): Boolean = !hasDrawing

    fun exportBitmap(): Bitmap {
        val safeWidth = if (width > 0) width else 1
        val safeHeight = if (height > 0) height else 1
        return Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888).also {
            val c = Canvas(it)
            draw(c)
        }
    }
}
