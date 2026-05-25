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

class InspectionPdfGenerator(private val context: Context, private val repo: InspectionRepository) {
    private val pageWidth = 595
    private val pageHeight = 842
    private val left = 36f
    private val right = 559f
    private val topContent = 104f
    private val bottom = 790f
    private val amber = Color.rgb(245, 158, 11)
    private val dark = Color.rgb(15, 17, 23)
    private val panel = Color.rgb(243, 244, 246)
    private val border = Color.rgb(209, 213, 219)
    private val textColor = Color.rgb(31, 41, 55)
    private val muted = Color.rgb(107, 114, 128)
    private val purple = Color.rgb(147, 51, 234)
    private val generatedAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
    private lateinit var doc: PdfDocument
    private lateinit var page: PdfDocument.Page
    private lateinit var canvas: Canvas
    private lateinit var currentData: InspectionData
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

    fun generate(data: InspectionData): File {
        doc = PdfDocument()
        currentData = data
        newPage()
        drawIdentification(data)
        drawSummary(data)
        drawRecords(data)
        drawSignature(data)
        finishPage()
        val file = File(context.cacheDir, "SafeField_Inspecao_${System.currentTimeMillis()}.pdf")
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
        canvas.drawText("RELATORIO DE INSPECAO", 300f, 28f, title)
        canvas.drawText("Gerado em: $generatedAt", 330f, 46f, sub)
        canvas.drawText(currentData.number.ifBlank { InspectionEngine.number(currentData.dateMillis) }, 330f, 62f, sub)
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

    private fun drawIdentification(data: InspectionData): Unit {
        section("1. Identificacao")
        twoColumn(listOf(
            "Numero" to data.number,
            "Empresa / cliente" to data.company,
            "Area / setor" to data.area,
            "Local" to data.place,
            "Inspetor" to data.inspector,
            "Data/hora" to Ui.fmt(data.dateMillis),
            "Objetivo" to data.objective
        ))
    }

    private fun drawSummary(data: InspectionData): Unit {
        section("2. Resumo executivo")
        val s = InspectionEngine.summary(data)
        twoColumn(listOf(
            "Total de registros" to s.total.toString(),
            "Registros abertos" to s.open.toString(),
            "Resolvidos / arquivados" to s.resolved.toString(),
            "Prioridade alta/critica" to s.highCritical.toString(),
            "Fotos anexadas" to s.photos.toString()
        ))
    }

    private fun drawRecords(data: InspectionData): Unit {
        section("3. Registros tecnicos de campo")
        if (data.records.isEmpty()) {
            tableRow(floatArrayOf(523f), arrayOf("Nenhum registro adicionado."))
            return
        }
        data.records.forEachIndexed { index, record -> drawRecord(index + 1, record) }
    }

    private fun drawRecord(number: Int, record: InspectionRecord): Unit {
        ensure(130f)
        fill.color = Color.WHITE
        val top = y
        canvas.drawRoundRect(RectF(left, top, right, top + 108f), 6f, 6f, fill)
        canvas.drawRoundRect(RectF(left, top, right, top + 108f), 6f, 6f, stroke)
        canvas.drawText("Registro $number", left + 10f, top + 16f, label)
        drawChip(record.priority, priorityColor(record.priority), right - 112f, top + 8f)
        canvas.drawText(record.category.ifBlank { "Categoria" }, left + 10f, top + 34f, body)
        canvas.drawText("Local: ${record.location.ifBlank { "-" }}", left + 10f, top + 49f, small)
        drawWrapped("Achado: ${record.description.ifBlank { "-" }}", left + 10f, top + 65f, 245f, body)
        drawWrapped("Risco: ${record.risk.ifBlank { "-" }}", left + 270f, top + 34f, 270f, body)
        drawWrapped("Acao: ${record.recommendation.ifBlank { "-" }}", left + 270f, top + 61f, 270f, body)
        canvas.drawText("Responsavel: ${record.responsible.ifBlank { "-" }}", left + 10f, top + 98f, small)
        canvas.drawText("Prazo: ${record.deadline.ifBlank { "-" }} | Status: ${record.status}", left + 270f, top + 98f, small)
        y += 120f
        record.photos.forEachIndexed { photoIndex, uri -> drawPhoto(number, photoIndex + 1, uri) }
    }

    private fun drawSignature(data: InspectionData): Unit {
        section("4. Assinatura")
        ensure(112f)
        val top = y
        fill.color = Color.WHITE
        canvas.drawRoundRect(RectF(left, top, right, top + 96f), 6f, 6f, fill)
        canvas.drawRoundRect(RectF(left, top, right, top + 96f), 6f, 6f, stroke)
        canvas.drawText("Inspetor responsavel", left + 10f, top + 16f, label)
        repo.base64ToBitmap(data.inspectorSignatureB64)?.let { bitmap -> drawBitmapInside(bitmap, RectF(left + 10f, top + 22f, left + 238f, top + 82f)) } ?: canvas.drawText("Assinatura pendente", left + 10f, top + 54f, body)
        canvas.drawLine(left + 270f, top + 58f, right - 12f, top + 58f, stroke)
        canvas.drawText(data.inspector.ifBlank { "Nome nao informado" }, left + 270f, top + 76f, body)
        y += 108f
    }

    private fun drawPhoto(recordNumber: Int, photoNumber: Int, uri: String): Unit {
        ensure(150f)
        val top = y
        fill.color = Color.WHITE
        canvas.drawRoundRect(RectF(left, top, right, top + 138f), 6f, 6f, fill)
        canvas.drawRoundRect(RectF(left, top, right, top + 138f), 6f, 6f, stroke)
        canvas.drawText("Registro $recordNumber - Foto $photoNumber", left + 10f, top + 16f, label)
        val bitmap = loadBitmap(uri)
        if (bitmap == null) canvas.drawText("Nao foi possivel carregar a imagem", left + 10f, top + 72f, body) else {
            drawBitmapInside(bitmap, RectF(left + 10f, top + 24f, right - 10f, top + 128f))
            bitmap.recycle()
        }
        y += 150f
    }

    private fun twoColumn(items: List<Pair<String, String>>): Unit {
        items.forEach { tableRow(floatArrayOf(160f, 363f), arrayOf(it.first, it.second.ifBlank { "-" }), labelFirst = true) }
    }

    private fun tableRow(widths: FloatArray, values: Array<String>, labelFirst: Boolean = false): Unit {
        val lines = values.mapIndexed { index, value -> wrap(value, widths[index] - 8f, if (labelFirst && index == 0) label else body) }
        val height = (lines.maxOf { it.size } * 10f + 12f).coerceAtLeast(25f)
        ensure(height)
        var x = left
        widths.forEachIndexed { index, width ->
            fill.color = if (labelFirst && index == 0) panel else Color.WHITE
            canvas.drawRect(x, y, x + width, y + height, fill)
            canvas.drawRect(x, y, x + width, y + height, stroke)
            val paint = if (labelFirst && index == 0) label else body
            var ty = y + 14f
            lines[index].forEach { canvas.drawText(it, x + 4f, ty, paint); ty += 10f }
            x += width
        }
        y += height
    }

    private fun drawChip(text: String, color: Int, x: Float, yTop: Float): Unit {
        fill.color = color
        canvas.drawRoundRect(RectF(x, yTop, x + 96f, yTop + 20f), 8f, 8f, fill)
        header.color = Color.WHITE
        canvas.drawText(text.uppercase(Locale("pt", "BR")), x + 8f, yTop + 14f, header)
    }

    private fun drawWrapped(text: String, x: Float, yStart: Float, width: Float, paint: Paint): Unit {
        var ty = yStart
        wrap(text, width, paint).take(3).forEach { line -> canvas.drawText(line, x, ty, paint); ty += 10f }
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

    private fun priorityColor(priority: String): Int {
        return when (priority) {
            "Baixa" -> Color.rgb(22, 163, 74)
            "Média" -> amber
            "Alta" -> Color.rgb(220, 38, 38)
            "Crítica" -> purple
            else -> muted
        }
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
