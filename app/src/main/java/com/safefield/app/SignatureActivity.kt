package com.safefield.app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import java.io.ByteArrayOutputStream

class SignatureActivity : Activity() {

    companion object {
        const val EXTRA_TITLE = "signature_title"
        const val EXTRA_SIGNATURE_B64 = "signature_b64"
    }

    override fun onCreate(savedInstanceState: Bundle?): Unit {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Assinatura" }
        val root = Ui.vbox(this, 14.dp())
        root.setBackgroundColor(Ui.SHELL)

        val topBar = Ui.row(this)
        topBar.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val back = Ui.ghostButton(this, "Voltar")
        back.layoutParams = LinearLayout.LayoutParams(96.dp(), ViewGroup.LayoutParams.WRAP_CONTENT)
        back.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        topBar.addView(back)

        val titleView = Ui.title(this, title, 19f)
        titleView.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(10.dp(), 0, 10.dp(), 0)
        }
        topBar.addView(titleView)

        val clear = Ui.ghostButton(this, "Limpar")
        clear.layoutParams = LinearLayout.LayoutParams(96.dp(), ViewGroup.LayoutParams.WRAP_CONTENT)
        topBar.addView(clear)

        val save = Ui.button(this, "Salvar assinatura")
        save.layoutParams = LinearLayout.LayoutParams(176.dp(), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(8.dp(), 0, 0, 0)
        }
        topBar.addView(save)
        root.addView(topBar)

        root.addView(Ui.label(this, "Assine no campo branco abaixo.").margin(0, 6.dp()))

        val pad = SignaturePadView(this)
        val padParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        padParams.setMargins(0, 8.dp(), 0, 0)
        pad.layoutParams = padParams
        root.addView(pad)

        clear.setOnClickListener { pad.clearPad() }
        save.setOnClickListener {
            if (pad.isEmpty()) {
                Toast.makeText(this, "Assine antes de salvar", Toast.LENGTH_SHORT).show()
            } else {
                val result = Intent()
                result.putExtra(EXTRA_SIGNATURE_B64, bitmapToBase64(pad.exportBitmap()))
                setResult(RESULT_OK, result)
                finish()
            }
        }

        setContentView(root)
    }

    override fun onBackPressed(): Unit {
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun Int.dp(): Int = Ui.dp(this@SignatureActivity, this)
}
