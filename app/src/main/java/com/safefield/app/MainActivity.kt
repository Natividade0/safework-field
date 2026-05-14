package com.safefield.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {
    private val amber = Color.rgb(245, 158, 11)
    private val dark = Color.rgb(15, 17, 23)
    private val shell = Color.rgb(5, 7, 11)
    private val panel = Color.rgb(18, 22, 30)
    private val cardColor = Color.rgb(24, 28, 36)
    private val textColor = Color.rgb(230, 237, 243)
    private val muted = Color.rgb(156, 163, 175)
    private lateinit var root: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var content: LinearLayout
    private val photos = mutableListOf<Uri>()
    private val workers = mutableListOf<String>()
    private val risks = mutableSetOf<String>()
    private val checks = mutableMapOf<Int, Boolean>()
    private val fields = mutableMapOf<String, EditText>()

    private val riskBank = mapOf(
        "Içamento de carga" to listOf("Raio de fogo/carga suspensa", "Queda de objetos", "Tombamento", "Prensagem"),
        "Trabalho em altura" to listOf("Queda de pessoas", "Queda de objetos", "Acesso inseguro", "Ancoragem inadequada"),
        "Escavação" to listOf("Desmoronamento", "Interferência subterrânea", "Queda de pessoas", "Atropelamento"),
        "Trabalho a quente" to listOf("Incêndio/explosão", "Queimadura", "Fumos metálicos", "Projeção de partículas"),
        "Eletricidade / LOTO" to listOf("Choque elétrico", "Arco elétrico", "Energia residual", "Bloqueio inadequado"),
        "Montagem industrial" to listOf("Prensagem", "Corte/perfuração", "Queda de objetos", "Ergonômicos")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        }
        buildShell()
        showHome()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun bg(color: Int, radius: Int = 18, strokeColor: Int? = null, strokeWidth: Int = 1): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (strokeColor != null) setStroke(dp(strokeWidth), strokeColor)
        }
    }

    private fun buildShell() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(dark)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundColor(shell)
        }
        val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(this).apply {
            setTextColor(amber)
            textSize = 30f
            text = "SafeField"
            setTypeface(Typeface.DEFAULT_BOLD)
            includeFontPadding = false
        }
        val sub = TextView(this).apply {
            setTextColor(muted)
            textSize = 13f
            text = "Segurança do Trabalho em campo"
            setPadding(0, dp(4), 0, 0)
        }
        titleBox.addView(title)
        titleBox.addView(sub)
        header.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(chipButton("Menu") { showHome() })
        scroll = ScrollView(this).apply { setBackgroundColor(dark) }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(28))
        }
        scroll.addView(content)
        root.addView(header)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun clear() { content.removeAllViews(); fields.clear() }

    private fun addTitle(title: String, subtitle: String? = null) {
        content.addView(TextView(this).apply {
            text = title
            textSize = 27f
            setTextColor(textColor)
            setTypeface(Typeface.DEFAULT_BOLD)
            includeFontPadding = false
            setPadding(0, 0, 0, dp(6))
        })
        if (subtitle != null) content.addView(TextView(this).apply {
            text = subtitle
            textSize = 14f
            setTextColor(muted)
            setPadding(0, 0, 0, dp(14))
        })
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = bg(cardColor, 18, Color.rgb(39, 45, 57), 1)
        setPadding(dp(16), dp(14), dp(16), dp(16))
    }

    private fun addCard(view: LinearLayout) {
        val lp = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(12)) }
        content.addView(view, lp)
    }

    private fun label(s: String): TextView = TextView(this).apply {
        text = s
        textSize = 12f
        setTextColor(muted)
        setTypeface(Typeface.DEFAULT_BOLD)
        setPadding(0, dp(10), 0, dp(5))
    }

    private fun input(parent: LinearLayout, key: String, hint: String, multi: Boolean = false): EditText {
        val e = EditText(this).apply {
            setTextColor(textColor)
            setHintTextColor(Color.rgb(100, 110, 125))
            setSingleLine(!multi)
            minLines = if (multi) 3 else 1
            background = bg(panel, 14, Color.rgb(45, 52, 66), 1)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            this.hint = hint
            textSize = 14f
        }
        fields[key] = e
        parent.addView(label(hint))
        parent.addView(e, LinearLayout.LayoutParams(-1, -2))
        return e
    }

    private fun primaryButton(s: String, onClick: () -> Unit): Button = Button(this).apply {
        text = s
        textSize = 15f
        setTextColor(Color.BLACK)
        setTypeface(Typeface.DEFAULT_BOLD)
        background = bg(amber, 16)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        setOnClickListener { onClick() }
    }

    private fun secondaryButton(s: String, onClick: () -> Unit): Button = Button(this).apply {
        text = s
        textSize = 14f
        setTextColor(textColor)
        background = bg(Color.rgb(28, 33, 43), 16, Color.rgb(49, 57, 72), 1)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        setOnClickListener { onClick() }
    }

    private fun chipButton(s: String, onClick: () -> Unit): Button = Button(this).apply {
        text = s
        textSize = 13f
        setTextColor(amber)
        setTypeface(Typeface.DEFAULT_BOLD)
        background = bg(Color.rgb(20, 24, 32), 18, Color.rgb(50, 56, 70), 1)
        setPadding(dp(12), dp(8), dp(12), dp(8))
        setOnClickListener { onClick() }
    }

    private fun section(parent: LinearLayout, s: String) {
        parent.addView(TextView(this).apply {
            text = s
            textSize = 17f
            setTextColor(amber)
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(0, dp(10), 0, dp(8))
        })
    }

    private fun showHome() {
        clear()
        addTitle("Módulos", "Base nativa Kotlin • sem WebView • pronta para evoluir")
        val modules = listOf(
            Triple("Permissão de Trabalho", "PT inteligente com checklist, riscos, trabalhadores e PDF nativo", true),
            Triple("APR", "Análise preliminar de riscos", false),
            Triple("DDS", "Diálogo diário de segurança", false),
            Triple("EPI", "Controle de entrega e inspeção", false),
            Triple("Inspeção", "Checklist de campo", false),
            Triple("Ocorrência", "Registro de desvios, incidentes e ações", false),
            Triple("Colaboradores", "Cadastro da equipe", false),
            Triple("Dashboard", "Indicadores do sistema", false)
        )
        modules.forEach { item ->
            val c = card()
            val name = TextView(this).apply {
                text = item.first
                textSize = 18f
                setTextColor(if (item.third) amber else textColor)
                setTypeface(Typeface.DEFAULT_BOLD)
            }
            val desc = TextView(this).apply {
                text = item.second
                textSize = 13f
                setTextColor(muted)
                setPadding(0, dp(5), 0, dp(12))
            }
            c.addView(name)
            c.addView(desc)
            c.addView(if (item.third) primaryButton("Abrir") { showPT() } else secondaryButton("Abrir") { showPlaceholder(item.first) })
            addCard(c)
        }
    }

    private fun showPlaceholder(name: String) {
        clear()
        addTitle(name, "Módulo nativo preparado para expansão.")
        val c = card()
        c.addView(TextView(this).apply {
            text = "A estrutura Kotlin já está pronta. O próximo passo é transformar este módulo em formulário completo."
            setTextColor(textColor)
            textSize = 15f
        })
        c.addView(secondaryButton("Voltar ao menu") { showHome() })
        addCard(c)
    }

    private fun showPT() {
        clear()
        photos.clear(); workers.clear(); risks.clear(); checks.clear()
        addTitle("Permissão de Trabalho", "PT nativa com checklist, riscos automáticos e PDF sem páginas brancas")

        val c1 = card(); section(c1, "1. Informações gerais")
        input(c1, "empresa", "Empresa / Planta")
        input(c1, "area", "Área / Setor")
        input(c1, "local", "Local da atividade")
        input(c1, "responsavel", "Responsável / Emissor")
        input(c1, "inicio", "Data/Hora início")
        input(c1, "fim", "Data/Hora fim / validade")
        input(c1, "executantes", "Equipe / executantes")
        input(c1, "descricao", "Descrição detalhada da atividade", true)
        input(c1, "ferramentas", "Ferramentas / equipamentos", true)
        input(c1, "produtos", "Substâncias / produtos")
        addCard(c1)

        val c2 = card(); section(c2, "2. Atividade e riscos")
        riskBank.keys.forEach { act ->
            val cb = CheckBox(this).apply {
                text = act
                textSize = 14f
                setTextColor(textColor)
                buttonTintList = android.content.res.ColorStateList.valueOf(amber)
                setOnCheckedChangeListener { _, checked ->
                    if (checked) risks.addAll(riskBank[act] ?: emptyList()) else risks.removeAll(riskBank[act] ?: emptyList())
                }
            }
            c2.addView(cb)
        }
        input(c2, "atividadeManual", "Outra atividade / observação manual", true)
        addCard(c2)

        val c3 = card(); section(c3, "3. Checklist de pré-requisitos")
        prereqs().forEachIndexed { i, item ->
            val cb = CheckBox(this).apply {
                text = item
                textSize = 14f
                setTextColor(textColor)
                buttonTintList = android.content.res.ColorStateList.valueOf(amber)
                setOnCheckedChangeListener { _, checked -> checks[i] = checked }
            }
            c3.addView(cb)
        }
        addCard(c3)

        val c4 = card(); section(c4, "4. Emergência")
        input(c4, "ponto", "Ponto de encontro")
        input(c4, "telefone", "Ramal / telefone de emergência")
        input(c4, "emergencia", "Procedimento de emergência", true)
        addCard(c4)

        val c5 = card(); section(c5, "5. Trabalhadores e fotos")
        val workerInput = input(c5, "worker", "Nome do trabalhador + função")
        c5.addView(secondaryButton("Adicionar trabalhador") {
            val value = workerInput.text.toString().trim()
            if (value.isNotEmpty()) { workers.add(value); workerInput.setText(""); toast("Trabalhador adicionado") }
        })
        c5.addView(secondaryButton("Adicionar foto") { openPhotoChooser() })
        addCard(c5)

        val c6 = card(); section(c6, "6. Finalizar")
        c6.addView(primaryButton("Gerar e compartilhar PDF") { sharePtPdf() }, LinearLayout.LayoutParams(-1, -2))
        c6.addView(secondaryButton("Voltar ao menu") { showHome() })
        addCard(c6)
    }

    private fun prereqs() = listOf(
        "APR válida emitida para esta atividade",
        "EPIs específicos inspecionados e aprovados",
        "Treinamento NR em dia para todos os envolvidos",
        "Área isolada e sinalizada adequadamente",
        "Responsável presente no local antes do início"
    )

    private fun v(key: String): String = fields[key]?.text?.toString()?.trim().orEmpty()

    private fun sharePtPdf() {
        val pdf = createPtPdf()
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", pdf)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartilhar PT em PDF"))
    }

    private fun createPtPdf(): File {
        val file = File(cacheDir, "SafeField_PT_${System.currentTimeMillis()}.pdf")
        val doc = PdfDocument()
        var pageNo = 1
        var page = newPage(doc, pageNo)
        var canvas = page.canvas
        var y = 70f
        fun ensure(height: Float) {
            if (y + height > 790f) {
                doc.finishPage(page)
                pageNo++
                page = newPage(doc, pageNo)
                canvas = page.canvas
                y = 70f
            }
        }
        fun line(value: String, bold: Boolean = false, size: Float = 12f) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = size
                typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            wrapText(value, 82).forEach {
                ensure(18f)
                canvas.drawText(it, 38f, y, paint)
                y += 18f
            }
        }
        fun title(value: String) {
            ensure(34f)
            val paint = Paint().apply { color = dark }
            canvas.drawRect(30f, y - 18f, 565f, y + 8f, paint)
            paint.color = amber; paint.textSize = 14f; paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(value, 38f, y, paint)
            y += 28f
        }
        title("PERMISSÃO DE TRABALHO")
        if (checks.values.count { it } < prereqs().size) line("PT BLOQUEADA - NÃO INICIAR ATIVIDADE", true, 13f)
        title("1. Informações gerais")
        line("Empresa/Planta: ${v("empresa")}")
        line("Área/Setor: ${v("area")}")
        line("Local: ${v("local")}")
        line("Responsável/Emissor: ${v("responsavel")}")
        line("Início: ${v("inicio")}  Fim/Validade: ${v("fim")}")
        line("Equipe/Executantes: ${v("executantes")}")
        line("Descrição: ${v("descricao")}")
        line("Ferramentas/Equipamentos: ${v("ferramentas")}")
        line("Produtos/Substâncias: ${v("produtos")}")
        title("2. Checklist de pré-requisitos")
        prereqs().forEachIndexed { i, item -> line("${if (checks[i] == true) "[X]" else "[ ]"} $item") }
        title("3. Riscos e medidas de controle")
        if (risks.isEmpty()) line("Nenhum risco selecionado.") else risks.forEach { line("• $it") }
        title("4. Emergência")
        line("Ponto de encontro: ${v("ponto")}")
        line("Telefone/Ramal: ${v("telefone")}")
        line("Procedimento: ${v("emergencia")}")
        title("5. Trabalhadores envolvidos")
        if (workers.isEmpty()) line("Nenhum trabalhador informado.") else workers.forEach { line("• $it") }
        title("6. Encerramento")
        line("Atividade concluída: ___ Sim  ___ Não")
        line("Área limpa e segura: ___ Sim  ___ Não")
        line("Responsável pelo encerramento: ______________________________")
        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun newPage(doc: PdfDocument, pageNo: Int): PdfDocument.Page {
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNo).create())
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = dark; canvas.drawRect(0f, 0f, 595f, 48f, paint)
        paint.color = amber; canvas.drawRect(0f, 0f, 10f, 842f, paint)
        paint.color = amber; paint.textSize = 20f; paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("SAFEFIELD", 36f, 31f, paint)
        paint.color = textColor; paint.textSize = 12f; paint.typeface = Typeface.DEFAULT
        canvas.drawText("Segurança do Trabalho em Campo", 170f, 31f, paint)
        paint.color = Color.GRAY; paint.textSize = 10f
        canvas.drawText("Página $pageNo", 500f, 825f, paint)
        return page
    }

    private fun wrapText(value: String, max: Int): List<String> {
        val words = value.replace("\n", " ").split(" ")
        val out = mutableListOf<String>()
        var line = ""
        for (word in words) {
            if ((line + " " + word).trim().length > max) { out.add(line); line = word } else line = (line + " " + word).trim()
        }
        if (line.isNotEmpty()) out.add(line)
        return if (out.isEmpty()) listOf("-") else out
    }

    private fun openPhotoChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }
        startActivityForResult(Intent.createChooser(intent, "Selecionar foto"), 200)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200 && resultCode == RESULT_OK) {
            data?.data?.let { photos.add(it); toast("Foto adicionada") }
        }
    }

    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
}
