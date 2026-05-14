package com.safefield.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PtPdfGenerator(private val context: Context, private val repo: PtRepository) {
    private val pageWidth = 595
    private val pageHeight = 842
    private val left = 42f
    private val right = 553f
    private val bottom = 790f
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(15, 17, 23); textSize = 18f; typeface = Typeface.DEFAULT_BOLD }
    private val hPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(17, 24, 39); textSize = 12f; typeface = Typeface.DEFAULT_BOLD }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(31, 41, 55); textSize = 10f }
    private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(107, 114, 128); textSize = 8.5f }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(209, 213, 219); strokeWidth = 1f }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(209, 213, 219); strokeWidth = 1f; style = Paint.Style.STROKE }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private lateinit var doc: PdfDocument
    private lateinit var canvas: Canvas
    private lateinit var currentData: PtData
    private var pageNumber = 0
    private var page: PdfDocument.Page? = null
    private var y = 0f
    private val generatedAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())

    fun generate(data: PtData): File {
        doc = PdfDocument(); currentData = data; newPage(data)
        section("Identificação"); field("Empresa / Planta", data.company); field("Área / Setor", data.area); field("Local da atividade", data.place); field("Responsável / Emissor", data.responsible)
        field("Início", Ui.fmt(data.startMillis)); field("Validade", "${data.validityHours}h"); field("Término", Ui.fmt(data.endMillis)); field("Equipe executante", data.teamName)
        section("Descrição da atividade"); paragraph(data.description); field("Ferramentas / equipamentos", data.tools); field("Substâncias / produtos", data.products)
        section("Checklist"); RiskEngine.checklistItems.forEach { field(it, data.checklist[it].orEmpty()) }
        section("Riscos e controles")
        val risks = RiskEngine.risksFor(data)
        if (risks.isEmpty()) paragraph("Nenhum risco gerado.")
        risks.forEach { risk -> ensure(48f); canvas.drawText(risk, left, y, hPaint); y += 14f; RiskEngine.controls[risk].orEmpty().forEach { control -> field("- $control", data.controls[RiskEngine.controlKey(risk, control)].orEmpty()) }; y += 4f }
        section("Trabalhadores"); data.workers.forEachIndexed { index, worker -> field("${index + 1}. ${worker.name}", worker.role) }
        section("Evidências"); field("Fotos anexadas", data.photoUris.size.toString())
        section("Assinatura do responsável"); repo.base64ToBitmap(data.signatureB64)?.let { drawSignature(it) } ?: paragraph("Assinatura pendente"); field("Nome do responsável", data.responsible)
        finishPage()
        val file = File(context.cacheDir, "SafeField_PT_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun newPage(data: PtData) {
        pageNumber++; page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()); canvas = page!!.canvas; y = 42f
        fillPaint.color = Color.WHITE; canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), fillPaint)
        fillPaint.color = Color.rgb(245, 158, 11); canvas.drawRect(0f, 0f, pageWidth.toFloat(), 78f, fillPaint)
        canvas.drawText("SAFEFIELD", left, 33f, titlePaint); canvas.drawText("Segurança do Trabalho em Campo", left, 51f, hPaint)
        canvas.drawText("Permissão de Trabalho", 382f, 33f, hPaint); canvas.drawText("Emissão: $generatedAt", 382f, 50f, textPaint)
        canvas.drawText("Status: ${if (RiskEngine.isReleased(data)) "PT LIBERADA" else "PT BLOQUEADA"}", 382f, 65f, textPaint); y = 104f
    }

    private fun finishPage() { canvas.drawLine(left, 806f, right, 806f, linePaint); canvas.drawText("SafeField", left, 820f, smallPaint); canvas.drawText("Gerado em $generatedAt", 245f, 820f, smallPaint); canvas.drawText("Página $pageNumber", 510f, 820f, smallPaint); doc.finishPage(page) }
    private fun ensure(space: Float) { if (y + space <= bottom) return; finishPage(); newPage(currentData) }
    private fun section(text: String) { ensure(32f); y += 8f; fillPaint.color = Color.rgb(243, 244, 246); canvas.drawRoundRect(left - 6f, y - 16f, right, y + 6f, 6f, 6f, fillPaint); canvas.drawText(text, left, y, hPaint); y += 20f }
    private fun field(label: String, value: String) { ensure(28f); val splitX = 218f; canvas.drawText(label, left, y, smallPaint); y += wrap(value.ifBlank { "-" }, splitX, y, right - splitX, textPaint); canvas.drawLine(left, y + 2f, right, y + 2f, linePaint); y += 12f }
    private fun paragraph(text: String) { ensure(34f); y += wrap(text.ifBlank { "-" }, left, y, right - left, textPaint); y += 10f }
    private fun wrap(text: String, x: Float, startY: Float, width: Float, paint: Paint): Float { var yy = startY; val words = text.replace("\n", " ").split(" "); var line = ""; words.forEach { word -> val next = if (line.isEmpty()) word else "$line $word"; if (paint.measureText(next) > width && line.isNotEmpty()) { ensure(14f); canvas.drawText(line, x, yy, paint); yy += 12f; line = word } else line = next }; if (line.isNotEmpty()) canvas.drawText(line, x, yy, paint); return yy - startY + 12f }
    private fun drawSignature(bitmap: Bitmap) { ensure(110f); fillPaint.color = Color.WHITE; canvas.drawRect(left, y, left + 230f, y + 88f, fillPaint); canvas.drawRect(left, y, left + 230f, y + 88f, borderPaint); val scaled = Bitmap.createScaledBitmap(bitmap, 220, 78, true); canvas.drawBitmap(scaled, left + 5f, y + 5f, null); y += 104f }
}
