package com.safefield.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Ui {
    const val SHELL = 0xFF07111F.toInt()
    const val DARK = 0xFF0B1B2B.toInt()
    const val PANEL = 0xFF102033.toInt()
    const val CARD = 0xFF13263A.toInt()
    const val CARD_SOFT = 0xFF18324B.toInt()
    const val BORDER = 0xFF2E4A63.toInt()
    const val BORDER_SOFT = 0xFF3A5873.toInt()
    const val AMBER = 0xFFF59E0B.toInt()
    const val AMBER_SOFT = 0xFFFCD34D.toInt()
    const val GREEN = 0xFF10B981.toInt()
    const val RED = 0xFFEF4444.toInt()
    const val BLUE = 0xFF38BDF8.toInt()
    const val BLUE_DARK = 0xFF0284C7.toInt()
    const val TEXT = 0xFFFFFFFF.toInt()
    const val MUTED = 0xFFCBD5E1.toInt()

    val dateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

    fun fmt(millis: Long): String = dateTime.format(Date(millis))

    fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    fun bg(color: Int, radius: Int, strokeColor: Int = Color.TRANSPARENT, strokeWidth: Int = 0): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
        }
    }

    fun ripple(color: Int, radius: Int, strokeColor: Int = Color.TRANSPARENT, strokeWidth: Int = 0): RippleDrawable {
        val normal = bg(color, radius, strokeColor, strokeWidth)
        val mask = bg(Color.WHITE, radius)
        return RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), normal, mask)
    }

    fun gradient(radius: Int, start: Int = 0xFF0B2A42.toInt(), end: Int = 0xFF102033.toInt(), stroke: Int = 0xFF25637F.toInt()): GradientDrawable {
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(start, end)).apply {
            cornerRadius = radius.toFloat()
            setStroke(1, stroke)
        }
    }

    fun vbox(context: Context, pad: Int = 0): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            if (pad > 0) setPadding(pad, pad, pad, pad)
        }
    }

    fun row(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    fun title(context: Context, text: String, size: Float = 22f): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(TEXT)
            textSize = size
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = true
            setLineSpacing(0f, 1.08f)
        }
    }

    fun label(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(MUTED)
            textSize = 13.5f
            typeface = Typeface.DEFAULT
            includeFontPadding = true
            setLineSpacing(0f, 1.12f)
        }
    }

    fun value(context: Context, text: String, color: Int = TEXT): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(color)
            textSize = 15.5f
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = true
            setLineSpacing(0f, 1.1f)
        }
    }

    fun section(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text.uppercase(Locale("pt", "BR"))
            setTextColor(BLUE)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = true
            letterSpacing = 0.08f
        }
    }

    fun chip(context: Context, text: String, color: Int = BLUE): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            includeFontPadding = true
            background = bg(color, dp(context, 999))
            setPadding(dp(context, 10), dp(context, 5), dp(context, 10), dp(context, 5))
        }
    }

    fun iconBubble(context: Context, text: String, color: Int = BLUE): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            includeFontPadding = true
            background = bg(color, dp(context, 16))
            minWidth = dp(context, 44)
            minHeight = dp(context, 44)
            setPadding(dp(context, 10), dp(context, 8), dp(context, 10), dp(context, 8))
        }
    }

    fun input(context: Context, hint: String, multi: Boolean = false): EditText {
        return EditText(context).apply {
            this.hint = hint
            setHintTextColor(0xFF94A3B8.toInt())
            setTextColor(TEXT)
            textSize = 15f
            typeface = Typeface.DEFAULT
            background = bg(PANEL, dp(context, 14), BORDER_SOFT, 1)
            setPadding(dp(context, 14), dp(context, 11), dp(context, 14), dp(context, 11))
            minHeight = dp(context, 52)
            if (multi) {
                minLines = 3
                gravity = Gravity.TOP
            }
            setOnFocusChangeListener { view, focused ->
                view.background = if (focused) bg(PANEL, dp(context, 14), BLUE, 2) else bg(PANEL, dp(context, 14), BORDER_SOFT, 1)
            }
        }
    }

    fun button(context: Context, text: String, color: Int = BLUE_DARK): Button {
        return Button(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            background = ripple(color, dp(context, 15))
            minHeight = dp(context, 56)
            isAllCaps = false
            elevation = 0f
            stateListAnimator = null
            setPadding(dp(context, 14), dp(context, 8), dp(context, 14), dp(context, 8))
            pressFeedback(this)
        }
    }

    fun ghostButton(context: Context, text: String): Button {
        return button(context, text, CARD_SOFT).apply {
            setTextColor(TEXT)
            background = ripple(CARD_SOFT, dp(context, 15), BORDER_SOFT, 1)
            elevation = 0f
        }
    }

    fun dangerButton(context: Context, text: String): Button {
        return button(context, text, RED)
    }

    fun card(context: Context): LinearLayout {
        return vbox(context, dp(context, 14)).apply {
            background = ripple(CARD, dp(context, 20), BORDER, 1)
            elevation = 0f
            translationZ = 0f
            pressFeedback(this)
        }
    }

    fun heroCard(context: Context): LinearLayout {
        return vbox(context, dp(context, 18)).apply {
            background = gradient(dp(context, 24))
            elevation = 0f
            translationZ = 0f
            pressFeedback(this)
        }
    }

    fun divider(context: Context): View {
        return View(context).apply {
            setBackgroundColor(BORDER)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
        }
    }

    fun progress(context: Context, done: Int, total: Int): LinearLayout {
        val outer = LinearLayout(context)
        outer.orientation = LinearLayout.HORIZONTAL
        outer.background = bg(PANEL, dp(context, 999), BORDER, 1)
        outer.setPadding(dp(context, 3), dp(context, 3), dp(context, 3), dp(context, 3))
        val safeTotal = if (total <= 0) 1 else total
        val safeDone = done.coerceIn(0, safeTotal)
        repeat(safeTotal) { index ->
            val bar = View(context)
            bar.background = bg(if (index < safeDone) BLUE else 0xFF263244.toInt(), dp(context, 999))
            outer.addView(bar, LinearLayout.LayoutParams(0, dp(context, 8), 1f).apply {
                setMargins(dp(context, 2), 0, dp(context, 2), 0)
            })
        }
        return outer
    }

    fun animateIn(view: View): View {
        view.alpha = 0f
        view.translationY = 8f
        view.animate().alpha(1f).translationY(0f).setDuration(180L).start()
        return view
    }

    fun pressFeedback(view: View): Unit {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.99f).scaleY(0.99f).setDuration(70L).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(100L).start()
            }
            false
        }
    }
}

fun View.margin(all: Int): View {
    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        setMargins(all, all, all, all)
    }
    return this
}

fun View.margin(horizontal: Int, vertical: Int): View {
    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        setMargins(horizontal, vertical, horizontal, vertical)
    }
    return this
}
