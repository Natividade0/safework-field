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

class PtPdfGenerator(private val context: Context, private val repo: PtRepository) {
    private val pageWidth: Int = 595
    private val pageHeight: Int = 842
    private val left: Float = 36f
    private val right: Float = 559f
    private val topContent: Float = 104f
    private val bottom: Float = 790f
    private val amber: Int = Color.rgb(245, 158, 11)
    private val dark: Int = Color.rgb(15, 17, 23)
    private val panel: Int = Color.rgb(243, 244, 246)
    private val border: Int = Color.rgb(209, 213, 219)
    private val textColor: Int = Color.rgb(31, 41, 55)
    private val muted: Int = Color.rgb(107, 114, 128)
    private val purple: Int = Color.rgb(147, 51, 234)
    private val generatedAt: String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
    private lateinit var doc: PdfDocument
    private lateinit var canvas: Canvas
    private lateinit var page: PdfDocument.Page
    private lateinit var currentData: PtData
    private var pageNumber: Int = 0
    private var y: Float = topContent

    private val fillPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = border }
    private val titlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 18f; typeface = Typeface.DEFAULT_BOLD }
    private val subtitlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(229, 231, 235); textSize = 9.5f }
    private val sectionPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dark; textSize = 12f; typeface = Typeface.DEFAULT_BOLD }
    private val headerPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 8.8f; typeface = Typeface.DEFAULT_BOLD }
    private val labelPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = muted; textSize = 8.7f; typeface = Typeface.DEFAULT_BOLD }
    private val bodyPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = textColor; textSize = 9.2f }
    private val smallPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = muted; textSize = 8f }
    private val statusPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 10.5f; typeface = Typeface.DEFAULT_BOLD }

    fun generate(data: PtData): File {
        doc = PdfDocument()
        currentData = data
        newPage()
        drawIdentification(data)
        drawDescription(data)
        drawEmergency(data)
        drawChecklist(data)
        drawRisks(data)
        drawApr(data)
        drawWorkers(data)
        drawResponsibleSignature(data)
        drawWorkerSignatures(data)
        drawEvidence(data)
        drawClosure(data)
        finishPage()
        val file = File(context.cacheDir, "SafeField_PT_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun newPage(): Unit {
        pageNumber++
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = topContent
        fillPaint.color = Color.WHITE
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), fillPaint)
        drawHeader()
    }

    private fun drawHeader(): Unit {
        fillPaint.color = dark
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 82f, fillPaint)
        fillPaint.color = amber
        canvas.drawRect(0f, 82f, pageWidth.toFloat(), 90f, fillPaint)
        canvas.drawText("SAFEFIELD", left, 28f, titlePaint)
        canvas.drawText("Seguranca do Trabalho em Campo", left, 45f, subtitlePaint)
        val flow = PtFlowEngine.flow(currentData)
        canvas.drawText("PERMISSAO DE TRABALHO", 318f, 28f, titlePaint)
        canvas.drawText("Gerado em: $generatedAt", 330f, 46f, subtitlePaint)
        canvas.drawText(flow.number, 330f, 62f, subtitlePaint)
        val status = when (flow.status) {
            PtStatus.LIBERADA -> "LIBERADA"
            PtStatus.EXPIRADA -> "EXPIRADA"
            PtStatus.RASCUNHO -> "RASCUNHO"
            PtStatus.BLOQUEADA -> "BLOQUEADA"
        }
        fillPaint.color = when (flow.status) {
            PtStatus.LIBERADA -> Color.rgb(34, 197, 94)
            PtStatus.RASCUNHO -> amber
            else -> Color.rgb(239, 68, 68)
        }
        canvas.drawRoundRect(RectF(470f, 52f, right, 72f), 8f, 8f, fillPaint)
        canvas.drawText(status, 481f, 66f, statusPaint)
    }

    private fun finishPage(): Unit {
        canvas.drawLine(left, 806f, right, 806f, strokePaint)
        canvas.drawText("SafeField", left, 821f, smallPaint)
        canvas.drawText("Gerado em $generatedAt", 235f, 821f, smallPaint)
        canvas.drawText("Pagina $pageNumber", 510f, 821f, smallPaint)
        doc.finishPage(page)
    }

    private fun ensure(space: Float): Unit {
        if (y + space <= bottom) return
        finishPage()
        newPage()
    }

    private fun section(title: String): Unit {
        ensure(34f)
        y += 8f
        fillPaint.color = panel
        canvas.drawRoundRect(RectF(left, y, right, y + 24f), 6f, 6f, fillPaint)
        fillPaint.color = amber
        canvas.drawRect(left, y, left + 5f, y + 24f, fillPaint)
        canvas.drawText(title, left + 12f, y + 16f, sectionPaint)
        y += 34f
    }

    private fun drawIdentification(data: PtData): Unit {
        section("1. Identificacao da PT")
        twoColumnTable(listOf("Numero da PT" to PtFlowEngine.ptNumber(data), "Empresa / Planta" to data.company, "Area / Setor" to data.area, "Local da atividade" to data.place, "Responsavel / Emissor" to data.responsible, "Inicio" to Ui.fmt(data.startMillis), "Validade" to "${data.validityHours}h", "Termino" to Ui.fmt(data.endMillis), "Equipe executante" to data.teamName))
    }

    private fun drawDescription(data: PtData): Unit {
        section("2. Descricao da atividade")
        twoColumnTable(listOf("Descricao detalhada" to data.description, "Ferramentas / equipamentos" to data.tools, "Substancias / produtos" to data.products, "Observacoes gerais" to data.observations))
    }

    private fun drawEmergency(data: PtData): Unit {
        section("3. Emergencia")
        twoColumnTable(listOf("Ponto de encontro / recurso" to data.emergencyPoint, "Telefone / canal" to data.emergencyPhone, "Procedimento" to data.emergencyProcedure))
    }

    private fun drawChecklist(data: PtData): Unit {
        section("4. Lista Geral de Verificacao")
        tableHeader(floatArrayOf(430f, 93f), arrayOf("Item", "Resposta"))
        RiskEngine.checklistItems.forEach { item ->
            val answer = data.checklist[item].orEmpty().ifBlank { "-" }
            tableRow(floatArrayOf(430f, 93f), arrayOf(item, answer), colors = arrayOf(textColor, answerColor(answer)))
        }
    }

    private fun drawRisks(data: PtData): Unit {
        section("5. Riscos e Medidas de Controle")
        tableHeader(floatArrayOf(142f, 293f, 88f), arrayOf("Risco", "Medida de controle", "Resposta"))
        val risks = RiskEngine.risksFor(data)
        if (risks.isEmpty()) { tableRow(floatArrayOf(142f, 293f, 88f), arrayOf("-", "Nenhum risco gerado", "-")); return }
        risks.forEach { risk ->
            RiskEngine.controls[risk].orEmpty().forEach { control ->
                val answer = data.controls[RiskEngine.controlKey(risk, control)].orEmpty().ifBlank { "-" }
                tableRow(floatArrayOf(142f, 293f, 88f), arrayOf(risk, control, answer), colors = arrayOf(textColor, textColor, answerColor(answer)))
            }
        }
    }

    private fun drawWorkers(data: PtData): Unit {
        section("7. Envolvidos")
        tableHeader(floatArrayOf(45f, 210f, 170f, 98f), arrayOf("No", "Nome", "Funcao / empresa", "Assinatura"))
        if (data.workers.isEmpty()) tableRow(floatArrayOf(45f, 210f, 170f, 98f), arrayOf("-", "Nenhum trabalhador informado", "-", "-"))
        else data.workers.forEachIndexed { index, worker ->
            val signed = if (worker.signatureB64.isNotBlank()) "Assinada" else "Pendente"
            tableRow(floatArrayOf(45f, 210f, 170f, 98f), arrayOf("${index + 1}", worker.name, worker.role.ifBlank { "-" }, signed), colors = arrayOf(textColor, textColor, textColor, if (signed == "Assinada") Color.rgb(22, 163, 74) else amber))
        }
    }

    private fun drawResponsibleSignature(data: PtData): Unit {
        section("8. Assinaturas")
        ensure(138f)
        val boxTop = y
        fillPaint.color = Color.WHITE
        canvas.drawRoundRect(RectF(left, boxTop, right, boxTop + 112f), 6f, 6f, fillPaint)
        canvas.drawRoundRect(RectF(left, boxTop, right, boxTop + 112f), 6f, 6f, strokePaint)
        repo.base64ToBitmap(data.signatureB64)?.let { bitmap ->
            drawBitmapInside(bitmap, RectF(left + 18f, boxTop + 12f, left + 248f, boxTop + 90f))
        } ?: canvas.drawText("Assinatura pendente", left + 18f, boxTop + 46f, bodyPaint)
        canvas.drawLine(left + 280f, boxTop + 72f, right - 18f, boxTop + 72f, strokePaint)
        canvas.drawText(data.responsible.ifBlank { "Responsavel nao informado" }, left + 280f, boxTop + 91f, bodyPaint)
        canvas.drawText("Responsavel / emissor", left + 280f, boxTop + 104f, smallPaint)
        y += 128f
    }

    private fun drawWorkerSignatures(data: PtData): Unit {
        if (data.workers.isEmpty()) { twoColumnTable(listOf("Envolvidos" to "Nenhum trabalhador informado")); return }
        data.workers.forEachIndexed { index, worker -> drawWorkerSignature(index + 1, worker) }
    }

    private fun drawWorkerSignature(number: Int, worker: Worker): Unit {
        ensure(120f)
        val boxTop = y
        fillPaint.color = Color.WHITE
        canvas.drawRoundRect(RectF(left, boxTop, right, boxTop + 104f), 6f, 6f, fillPaint)
        canvas.drawRoundRect(RectF(left, boxTop, right, boxTop + 104f), 6f, 6f, strokePaint)
        canvas.drawText("Envolvido $number", left + 10f, boxTop + 16f, labelPaint)
        repo.base64ToBitmap(worker.signatureB64)?.let { bitmap ->
            drawBitmapInside(bitmap, RectF(left + 10f, boxTop + 24f, left + 238f, boxTop + 86f))
        } ?: canvas.drawText("Assinatura pendente", left + 10f, boxTop + 55f, bodyPaint)
        canvas.drawLine(left + 270f, boxTop + 62f, right - 12f, boxTop + 62f, strokePaint)
        canvas.drawText(worker.name.ifBlank { "Nome nao informado" }, left + 270f, boxTop + 80f, bodyPaint)
        canvas.drawText(worker.role.ifBlank { "Funcao nao informada" }, left + 270f, boxTop + 94f, smallPaint)
        if (worker.signedAt.isNotBlank()) canvas.drawText("Assinado em ${worker.signedAt}", left + 10f, boxTop + 98f, smallPaint)
        y += 116f
    }

    private fun drawEvidence(data: PtData): Unit {
        section("9. Evidencias fotograficas")
        twoColumnTable(listOf("Fotos anexadas" to data.photoUris.size.toString()))
        if (data.photoUris.isEmpty()) return
        data.photoUris.forEachIndexed { index, uri -> drawEvidencePhoto(index + 1, uri) }
    }

    private fun drawApr(data: PtData): Unit {
        section("6. APR AUTOMATICA")
        val apr = AprEngine.generate(data)
        tableHeader(floatArrayOf(92f, 112f, 66f, 155f, 98f), arrayOf("Atividade", "Risco", "Nivel", "Controle", "EPI"))
        if (apr.isEmpty()) {
            tableRow(floatArrayOf(92f, 112f, 66f, 155f, 98f), arrayOf("-", "APR nao gerada", "-", "Informe atividade critica ou descricao detalhada", "-"))
            return
        }
        apr.forEach { item ->
            tableRow(
                floatArrayOf(92f, 112f, 66f, 155f, 98f),
                arrayOf(item.activity, item.risk, item.classification, item.control, item.epi),
                colors = arrayOf(textColor, textColor, aprColor(item.classification), textColor, textColor)
            )
        }
    }

    private fun drawClosure(data: PtData): Unit {
        section("10. ENCERRAMENTO DA PT")
        val closed = data.closureAt.isNotBlank()
        twoColumnTable(
            listOf(
                "Status do encerramento" to if (closed) "ENCERRADA" else "Encerramento pendente",
                "Data/hora encerramento" to data.closureAt,
                "Responsavel encerramento" to data.closureResponsible,
                "Condicao final da area" to data.closureAreaCondition,
                "Observacoes finais" to data.closureNotes,
                "Pendencias encontradas" to data.closurePending,
                "Houve incidente?" to if (data.closureIncident) "Sim" else "Nao",
                "Fotos finais" to data.closurePhotoUris.size.toString()
            )
        )
        drawClosureSignature(data)
        if (data.closurePhotoUris.isNotEmpty()) {
            data.closurePhotoUris.forEachIndexed { index, uri -> drawEvidencePhoto(index + 1, uri) }
        }
    }

    private fun drawClosureSignature(data: PtData): Unit {
        ensure(118f)
        val boxTop = y
        fillPaint.color = Color.WHITE
        canvas.drawRoundRect(RectF(left, boxTop, right, boxTop + 104f), 6f, 6f, fillPaint)
        canvas.drawRoundRect(RectF(left, boxTop, right, boxTop + 104f), 6f, 6f, strokePaint)
        canvas.drawText("Assinatura de encerramento", left + 10f, boxTop + 16f, labelPaint)
        repo.base64ToBitmap(data.closureSignatureB64)?.let { bitmap ->
            drawBitmapInside(bitmap, RectF(left + 10f, boxTop + 24f, left + 238f, boxTop + 86f))
        } ?: canvas.drawText("Assinatura pendente", left + 10f, boxTop + 55f, bodyPaint)
        canvas.drawLine(left + 270f, boxTop + 62f, right - 12f, boxTop + 62f, strokePaint)
        canvas.drawText(data.closureResponsible.ifBlank { "Responsavel nao informado" }, left + 270f, boxTop + 80f, bodyPaint)
        canvas.drawText(if (data.closureAt.isNotBlank()) "Encerrado em ${data.closureAt}" else "Encerramento nao registrado", left + 270f, boxTop + 94f, smallPaint)
        y += 116f
    }

    private fun drawEvidencePhoto(number: Int, uriString: String): Unit {
        ensure(150f)
        val boxTop = y
        fillPaint.color = Color.WHITE
        canvas.drawRoundRect(RectF(left, boxTop, right, boxTop + 138f), 6f, 6f, fillPaint)
        canvas.drawRoundRect(RectF(left, boxTop, right, boxTop + 138f), 6f, 6f, strokePaint)
        canvas.drawText("Foto $number", left + 10f, boxTop + 16f, labelPaint)
        val bitmap = loadBitmapFromUri(uriString)
        if (bitmap == null) {
            canvas.drawText("Foto $number: nao foi possivel carregar a imagem", left + 10f, boxTop + 72f, bodyPaint)
        } else {
            drawBitmapInside(bitmap, RectF(left + 10f, boxTop + 24f, right - 10f, boxTop + 128f))
            bitmap.recycle()
        }
        y += 150f
    }

    private fun loadBitmapFromUri(uriString: String): Bitmap? {
        return runCatching {
            val bytes = context.contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
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
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (width / sample > 1400 || height / sample > 1400) sample *= 2
        return sample.coerceAtLeast(1)
    }

    private fun drawBitmapInside(bitmap: Bitmap, bounds: RectF): Unit {
        val ratio = minOf(bounds.width() / bitmap.width.toFloat(), bounds.height() / bitmap.height.toFloat())
        val width = bitmap.width * ratio
        val height = bitmap.height * ratio
        val leftPos = bounds.left + (bounds.width() - width) / 2f
        val topPos = bounds.top + (bounds.height() - height) / 2f
        canvas.drawBitmap(bitmap, null, RectF(leftPos, topPos, leftPos + width, topPos + height), null)
    }

    private fun twoColumnTable(items: List<Pair<String, String>>): Unit {
        val widths = floatArrayOf(160f, 363f)
        items.forEach { item -> tableRow(widths, arrayOf(item.first, item.second.ifBlank { "-" }), labelFirst = true) }
    }

    private fun tableHeader(widths: FloatArray, labels: Array<String>): Unit {
        ensure(24f)
        fillPaint.color = dark
        var x = left
        widths.forEachIndexed { index, width ->
            canvas.drawRect(x, y, x + width, y + 22f, fillPaint)
            canvas.drawRect(x, y, x + width, y + 22f, strokePaint)
            canvas.drawText(labels[index], x + 6f, y + 14f, headerPaint)
            x += width
        }
        y += 22f
    }

    private fun tableRow(widths: FloatArray, values: Array<String>, labelFirst: Boolean = false, colors: Array<Int>? = null): Unit {
        val lines = values.mapIndexed { index, value -> wrapLines(value, widths[index] - 12f, if (labelFirst && index == 0) labelPaint else bodyPaint) }
        val rowHeight = (lines.maxOf { it.size } * 11f + 12f).coerceAtLeast(25f)
        ensure(rowHeight)
        var x = left
        widths.forEachIndexed { index, width ->
            fillPaint.color = if (labelFirst && index == 0) panel else Color.WHITE
            canvas.drawRect(x, y, x + width, y + rowHeight, fillPaint)
            canvas.drawRect(x, y, x + width, y + rowHeight, strokePaint)
            val paint = if (labelFirst && index == 0) labelPaint else bodyPaint
            val previousColor = paint.color
            if (colors != null && index < colors.size) paint.color = colors[index]
            var textY = y + 15f
            lines[index].forEach { line -> canvas.drawText(line, x + 6f, textY, paint); textY += 11f }
            paint.color = previousColor
            x += width
        }
        y += rowHeight
    }

    private fun answerColor(answer: String): Int {
        return when (answer) {
            RiskEngine.NO -> Color.rgb(220, 38, 38)
            "-" -> amber
            else -> textColor
        }
    }

    private fun aprColor(classification: String): Int {
        return when (classification.uppercase(Locale("pt", "BR"))) {
            "BAIXO" -> Color.rgb(22, 163, 74)
            "MEDIO" -> amber
            "ALTO" -> Color.rgb(220, 38, 38)
            "CRITICO" -> purple
            else -> textColor
        }
    }

    private fun wrapLines(value: String, width: Float, paint: Paint): List<String> {
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
}
