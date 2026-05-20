package com.safefield.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AprPdfGenerator(private val context: Context, private val repo: AprRepository) {
    private val pageWidth = 595
    private val pageHeight = 842
    private val left = 36f
    private val right = 559f
    private val bottom = 790f
    private val topContent = 104f
    private val amber = Color.rgb(245, 158, 11)
    private val dark = Color.rgb(15, 17, 23)
    private val panel = Color.rgb(243, 244, 246)
    private val border = Color.rgb(209, 213, 219)
    private val textColor = Color.rgb(31, 41, 55)
    private val muted = Color.rgb(107, 114, 128)
    private val generatedAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
    private lateinit var doc: PdfDocument
    private lateinit var page: PdfDocument.Page
    private lateinit var canvas: Canvas
    private lateinit var currentData: AprData
    private var pageNumber = 0
    private var y = topContent

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = border }
    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 18f; typeface = Typeface.DEFAULT_BOLD }
    private val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(229, 231, 235); textSize = 9.5f }
    private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dark; textSize = 12f; typeface = Typeface.DEFAULT_BOLD }
    private val header = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 8.2f; typeface = Typeface.DEFAULT_BOLD }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = muted; textSize = 8.6f; typeface = Typeface.DEFAULT_BOLD }
    private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = textColor; textSize = 8.8f }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = muted; textSize = 8f }

    fun generate(data: AprData): File {
        doc = PdfDocument()
        currentData = data
        newPage()
        drawIdentification(data)
        drawItems(data)
        drawWorkers(data)
        drawSignatures(data)
        drawEvidence(data)
        finishPage()
        val file = File(context.cacheDir, "SafeField_APR_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun newPage(): Unit {
        pageNumber++
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = topContent
        fill.color = Color.WHITE
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), fill)
        drawHeader()
    }

    private fun drawHeader(): Unit {
        fill.color = dark
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 82f, fill)
        fill.color = amber
        canvas.drawRect(0f, 82f, pageWidth.toFloat(), 90f, fill)
        canvas.drawText("SAFEFIELD", left, 28f, title)
        canvas.drawText("Seguranca do Trabalho em Campo", left, 45f, sub)
        canvas.drawText("ANALISE PRELIMINAR DE RISCO", 280f, 28f, title)
        canvas.drawText("Gerado em: $generatedAt", 330f, 46f, sub)
        canvas.drawText(currentData.aprNumber.ifBlank { AprEngine.aprNumber(currentData.dateMillis) }, 330f, 62f, sub)
        fill.color = if (AprValidator.pending(currentData).isEmpty()) Color.rgb(34, 197, 94) else amber
        canvas.drawRoundRect(RectF(470f, 52f, right, 72f), 8f, 8f, fill)
        canvas.drawText(if (AprValidator.pending(currentData).isEmpty()) "PRONTA" else "RASCUNHO", 481f, 66f, header)
    }

    private fun finishPage(): Unit {
        canvas.drawLine(left, 806f, right, 806f, stroke)
        canvas.drawText("SafeField", left, 821f, small)
        canvas.drawText("Gerado em $generatedAt", 235f, 821f, small)
        canvas.drawText("Pagina $pageNumber", 510f, 821f, small)
        doc.finishPage(page)
    }

    private fun ensure(space: Float): Unit {
        if (y + space <= bottom) return
        finishPage()
        newPage()
    }

    private fun section(text: String): Unit {
        ensure(34f)
        y += 8f
        fill.color = panel
        canvas.drawRoundRect(RectF(left, y, right, y + 24f), 6f, 6f, fill)
        fill.color = amber
        canvas.drawRect(left, y, left + 5f, y + 24f, fill)
        canvas.drawText(text, left + 12f, y + 16f, sectionPaint)
        y += 34f
    }

    private fun drawIdentification(data: AprData): Unit {
        section("1. Identificacao")
        twoColumn(listOf(
            "Numero da APR" to data.aprNumber,
            "Empresa / Planta" to data.company,
            "Area / Setor" to data.area,
            "Local" to data.place,
            "Responsavel" to data.responsible,
            "Data/hora" to Ui.fmt(data.dateMillis),
            "Descricao da atividade" to data.activityDescription,
            "Ferramentas / equipamentos" to data.tools,
            "Observacoes" to data.observations
        ))
    }

    private fun drawItems(data: AprData): Unit {
        section("2. Itens da APR")
        val widths = floatArrayOf(70f, 74f, 74f, 86f, 105f, 58f, 56f)
        tableHeader(widths, arrayOf("Atividade", "Perigo", "Risco", "Consequencia", "Controle", "EPI", "Nivel"))
        if (data.items.isEmpty()) tableRow(widths, arrayOf("-", "-", "Nenhum item APR", "-", "-", "-", "-"))
        data.items.forEach { item ->
            tableRow(widths, arrayOf(item.activity, item.danger, item.risk, item.consequence, item.control, item.epi, item.classification), colors = arrayOf(textColor, textColor, textColor, textColor, textColor, textColor, AprEngine.statusColor(item.classification)))
        }
    }

    private fun drawWorkers(data: AprData): Unit {
        section("3. Envolvidos")
        val widths = floatArrayOf(45f, 250f, 150f, 78f)
        tableHeader(widths, arrayOf("No", "Nome", "Funcao", "Assinatura"))
        if (data.workers.isEmpty()) tableRow(widths, arrayOf("-", "Nenhum envolvido", "-", "-"))
        data.workers.forEachIndexed { index, worker ->
            tableRow(widths, arrayOf("${index + 1}", worker.name, worker.role, if (worker.signatureB64.isNotBlank()) "Assinada" else "Pendente"))
        }
    }

    private fun drawSignatures(data: AprData): Unit {
        section("4. Assinaturas")
        drawSignature("Responsavel pela APR", data.responsible, data.responsibleSignatureB64)
        data.workers.forEachIndexed { index, worker -> drawSignature("Envolvido ${index + 1}", worker.name, worker.signatureB64, worker.signedAt) }
    }

    private fun drawSignature(titleText: String, name: String, signature: String, signedAt: String = ""): Unit {
        ensure(112f)
        val top = y
        fill.color = Color.WHITE
        canvas.drawRoundRect(RectF(left, top, right, top + 96f), 6f, 6f, fill)
        canvas.drawRoundRect(RectF(left, top, right, top + 96f), 6f, 6f, stroke)
        canvas.drawText(titleText, left + 10f, top + 16f, label)
        repo.base64ToBitmap(signature)?.let { bitmap -> drawBitmapInside(bitmap, RectF(left + 10f, top + 22f, left + 238f, top + 82f)) } ?: canvas.drawText("Assinatura pendente", left + 10f, top + 54f, body)
        canvas.drawLine(left + 270f, top + 58f, right - 12f, top + 58f, stroke)
        canvas.drawText(name.ifBlank { "Nome nao informado" }, left + 270f, top + 76f, body)
        if (signedAt.isNotBlank()) canvas.drawText("Assinado em $signedAt", left + 270f, top + 90f, small)
        y += 108f
    }

    private fun drawEvidence(data: AprData): Unit {
        section("5. Evidencias")
        twoColumn(listOf("Fotos anexadas" to data.photos.size.toString()))
        data.photos.forEachIndexed { index, uri -> drawPhoto(index + 1, uri) }
    }

    private fun drawPhoto(number: Int, uri: String): Unit {
        ensure(150f)
        val top = y
        fill.color = Color.WHITE
        canvas.drawRoundRect(RectF(left, top, right, top + 138f), 6f, 6f, fill)
        canvas.drawRoundRect(RectF(left, top, right, top + 138f), 6f, 6f, stroke)
        canvas.drawText("Foto $number", left + 10f, top + 16f, label)
        val bitmap = loadBitmap(uri)
        if (bitmap == null) canvas.drawText("Foto $number: nao foi possivel carregar a imagem", left + 10f, top + 72f, body) else {
            drawBitmapInside(bitmap, RectF(left + 10f, top + 24f, right - 10f, top + 128f))
            bitmap.recycle()
        }
        y += 150f
    }

    private fun twoColumn(items: List<Pair<String, String>>): Unit {
        items.forEach { tableRow(floatArrayOf(160f, 363f), arrayOf(it.first, it.second.ifBlank { "-" }), labelFirst = true) }
    }

    private fun tableHeader(widths: FloatArray, labels: Array<String>): Unit {
        ensure(24f)
        fill.color = dark
        var x = left
        widths.forEachIndexed { index, width ->
            canvas.drawRect(x, y, x + width, y + 22f, fill)
            canvas.drawRect(x, y, x + width, y + 22f, stroke)
            canvas.drawText(labels[index], x + 4f, y + 14f, header)
            x += width
        }
        y += 22f
    }

    private fun tableRow(widths: FloatArray, values: Array<String>, labelFirst: Boolean = false, colors: Array<Int>? = null): Unit {
        val lines = values.mapIndexed { index, value -> wrap(value, widths[index] - 8f, if (labelFirst && index == 0) label else body) }
        val height = (lines.maxOf { it.size } * 10f + 12f).coerceAtLeast(25f)
        ensure(height)
        var x = left
        widths.forEachIndexed { index, width ->
            fill.color = if (labelFirst && index == 0) panel else Color.WHITE
            canvas.drawRect(x, y, x + width, y + height, fill)
            canvas.drawRect(x, y, x + width, y + height, stroke)
            val paint = if (labelFirst && index == 0) label else body
            val old = paint.color
            if (colors != null && index < colors.size) paint.color = colors[index]
            var ty = y + 14f
            lines[index].forEach { canvas.drawText(it, x + 4f, ty, paint); ty += 10f }
            paint.color = old
            x += width
        }
        y += height
    }

    private fun wrap(value: String, width: Float, paint: Paint): List<String> {
        val words = value.replace("\n", " ").trim().ifBlank { "-" }.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var line = ""
        words.forEach { word ->
            val next = if (line.isBlank()) word else "$line $word"
            if (paint.measureText(next) > width && line.isNotBlank()) { lines.add(line); line = word } else line = next
        }
        if (line.isNotBlank()) lines.add(line)
        return lines.ifEmpty { listOf("-") }
    }

    private fun loadBitmap(uri: String): Bitmap? = runCatching {
        val bytes = context.contentResolver.openInputStream(Uri.parse(uri))?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                out.write(buffer, 0, read)
            }
            out.toByteArray()
        } ?: return@runCatching null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight) }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }.getOrNull()

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (width / sample > 1400 || height / sample > 1400) sample *= 2
        return sample.coerceAtLeast(1)
    }

    private fun drawBitmapInside(bitmap: Bitmap, bounds: RectF): Unit {
        val ratio = minOf(bounds.width() / bitmap.width.toFloat(), bounds.height() / bitmap.height.toFloat())
        val w = bitmap.width * ratio
        val h = bitmap.height * ratio
        val l = bounds.left + (bounds.width() - w) / 2f
        val t = bounds.top + (bounds.height() - h) / 2f
        canvas.drawBitmap(bitmap, null, RectF(l, t, l + w, t + h), null)
    }
}

object AprValidator {
    fun pending(data: AprData): List<String> {
        val list = mutableListOf<String>()
        if (data.company.isBlank()) list.add("Empresa pendente")
        if (data.place.isBlank()) list.add("Local pendente")
        if (data.responsible.isBlank()) list.add("Responsavel pendente")
        if (data.activityDescription.isBlank()) list.add("Descricao da atividade pendente")
        if (data.selectedActivities.isEmpty() && data.manualActivity.isBlank() && data.items.isEmpty()) list.add("Nenhuma atividade informada")
        if (data.items.isEmpty()) list.add("Nenhum risco APR gerado")
        if (data.responsibleSignatureB64.isBlank()) list.add("Assinatura do responsavel pendente")
        val unsigned = data.workers.count { it.signatureB64.isBlank() }
        if (unsigned > 0) list.add("$unsigned envolvido(s) sem assinatura")
        return list
    }
}
