package com.safefield.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
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
    const val SHELL = 0xFF05070B.toInt()
    const val DARK = 0xFF0F1117.toInt()
    const val PANEL = 0xFF12161E.toInt()
    const val CARD = 0xFF181C24.toInt()
    const val CARD_SOFT = 0xFF202632.toInt()
    const val BORDER = 0xFF2A3140.toInt()
    const val BORDER_SOFT = 0xFF353D4D.toInt()
    const val AMBER = 0xFFF59E0B.toInt()
    const val AMBER_SOFT = 0xFFFCD34D.toInt()
    const val GREEN = 0xFF22C55E.toInt()
    const val RED = 0xFFEF4444.toInt()
    const val TEXT = 0xFFE6EDF3.toInt()
    const val MUTED = 0xFF9CA3AF.toInt()

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

    fun gradient(radius: Int, start: Int = 0xFF1B202B.toInt(), end: Int = 0xFF0F1117.toInt(), stroke: Int = BORDER): GradientDrawable {
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
        }
    }

    fun label(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(MUTED)
            textSize = 13f
            includeFontPadding = true
        }
    }

    fun value(context: Context, text: String, color: Int = TEXT): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(color)
            textSize = 15f
            includeFontPadding = true
        }
    }

    fun section(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text.uppercase(Locale("pt", "BR"))
            setTextColor(AMBER_SOFT)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
        }
    }

    fun chip(context: Context, text: String, color: Int = AMBER): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(color)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = bg(0x22111111, dp(context, 999), color, 1)
            setPadding(dp(context, 10), dp(context, 5), dp(context, 10), dp(context, 5))
        }
    }

    fun input(context: Context, hint: String, multi: Boolean = false): EditText {
        return EditText(context).apply {
            this.hint = hint
            setHintTextColor(0xFF6B7280.toInt())
            setTextColor(TEXT)
            textSize = 15f
            background = bg(PANEL, dp(context, 14), BORDER, 1)
            setPadding(dp(context, 14), dp(context, 11), dp(context, 14), dp(context, 11))
            if (multi) {
                minLines = 3
                gravity = Gravity.TOP
            }
        }
    }

    fun button(context: Context, text: String, color: Int = AMBER): Button {
        return Button(context).apply {
            this.text = text
            setTextColor(if (color == AMBER) Color.BLACK else Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            background = bg(color, dp(context, 16))
            minHeight = dp(context, 52)
            isAllCaps = false
            elevation = dp(context, 2).toFloat()
            stateListAnimator = null
        }
    }

    fun ghostButton(context: Context, text: String): Button {
        return button(context, text, PANEL).apply {
            setTextColor(TEXT)
            background = bg(PANEL, dp(context, 16), BORDER_SOFT, 1)
            elevation = 0f
        }
    }

    fun dangerButton(context: Context, text: String): Button {
        return button(context, text, RED)
    }

    fun card(context: Context): LinearLayout {
        return vbox(context, dp(context, 16)).apply {
            background = bg(CARD, dp(context, 22), BORDER, 1)
            elevation = dp(context, 3).toFloat()
            translationZ = dp(context, 2).toFloat()
        }
    }

    fun heroCard(context: Context): LinearLayout {
        return vbox(context, dp(context, 18)).apply {
            background = gradient(dp(context, 26), 0xFF1F2632.toInt(), 0xFF0B0E14.toInt(), 0xFF3A2A12.toInt())
            elevation = dp(context, 5).toFloat()
            translationZ = dp(context, 3).toFloat()
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
            bar.background = bg(if (index < safeDone) AMBER else 0xFF303746.toInt(), dp(context, 999))
            outer.addView(bar, LinearLayout.LayoutParams(0, dp(context, 8), 1f).apply {
                setMargins(dp(context, 2), 0, dp(context, 2), 0)
            })
        }
        return outer
    }

    fun animateIn(view: View): View {
        view.alpha = 0f
        view.scaleX = 0.98f
        view.scaleY = 0.98f
        view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220L).start()
        return view
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
