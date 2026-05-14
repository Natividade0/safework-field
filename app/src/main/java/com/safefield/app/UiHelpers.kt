package com.safefield.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
    const val AMBER = 0xFFF59E0B.toInt()
    const val GREEN = 0xFF22C55E.toInt()
    const val RED = 0xFFEF4444.toInt()
    const val TEXT = 0xFFE6EDF3.toInt()
    const val MUTED = 0xFF9CA3AF.toInt()

    val dateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    fun fmt(millis: Long): String = dateTime.format(Date(millis))
    fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    fun bg(color: Int, radius: Int, strokeColor: Int = Color.TRANSPARENT, strokeWidth: Int = 0): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
        if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
    }

    fun vbox(context: Context, pad: Int = 0): LinearLayout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; if (pad > 0) setPadding(pad, pad, pad, pad) }
    fun row(context: Context): LinearLayout = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
    fun title(context: Context, text: String, size: Float = 22f): TextView = TextView(context).apply { this.text = text; setTextColor(TEXT); textSize = size; typeface = Typeface.DEFAULT_BOLD }
    fun label(context: Context, text: String): TextView = TextView(context).apply { this.text = text; setTextColor(MUTED); textSize = 13f }
    fun value(context: Context, text: String, color: Int = TEXT): TextView = TextView(context).apply { this.text = text; setTextColor(color); textSize = 15f }

    fun input(context: Context, hint: String, multi: Boolean = false): EditText = EditText(context).apply {
        this.hint = hint
        setHintTextColor(MUTED)
        setTextColor(TEXT)
        textSize = 15f
        background = bg(PANEL, dp(context, 10), 0xFF2B313D.toInt(), 1)
        setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8))
        if (multi) { minLines = 3; gravity = android.view.Gravity.TOP }
    }

    fun button(context: Context, text: String, color: Int = AMBER): Button = Button(context).apply {
        this.text = text
        setTextColor(if (color == AMBER) Color.BLACK else Color.WHITE)
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        background = bg(color, dp(context, 12))
        minHeight = dp(context, 48)
        isAllCaps = false
    }

    fun ghostButton(context: Context, text: String): Button = button(context, text, PANEL).apply { setTextColor(TEXT); background = bg(PANEL, dp(context, 12), 0xFF2B313D.toInt(), 1) }
    fun card(context: Context): LinearLayout = vbox(context, dp(context, 14)).apply { background = bg(CARD, dp(context, 16), 0xFF252B36.toInt(), 1) }
}

fun View.margin(all: Int): View {
    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(all, all, all, all) }
    return this
}
