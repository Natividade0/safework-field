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

        val header = Ui.row(this)
        val titleView = Ui.title(this, title, 20f)
        titleView.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        header.addView(titleView)
        header.addView(Ui.chip(this, "TELA CHEIA", Ui.AMBER))
        root.addView(header.margin(0, 0))

        root.addView(Ui.label(this, "Assine com o dedo. Use salvar para retornar a PT.").margin(0, 6.dp()))

        val pad = SignaturePadView(this)
        pad.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        root.addView(pad.margin(0, 8.dp()))

        val actions = Ui.row(this)
        val clear = Ui.ghostButton(this, "Limpar")
        clear.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        clear.setOnClickListener { pad.clearPad() }
        actions.addView(clear)

        val cancel = Ui.ghostButton(this, "Cancelar")
        cancel.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        cancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        actions.addView(cancel)

        val save = Ui.button(this, "Salvar assinatura")
        save.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
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
        actions.addView(save)
        root.addView(actions.margin(0, 8.dp()))

        setContentView(root)
    }

    override fun onBackPressed(): Unit {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun Int.dp(): Int = Ui.dp(this@SignatureActivity, this)
}
