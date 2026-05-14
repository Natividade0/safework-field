package com.safefield.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val amber = Color.rgb(245, 158, 11)
    private val dark = Color.rgb(15, 17, 23)
    private val panel = Color.rgb(18, 22, 30)
    private val text = Color.rgb(230, 237, 243)
    private val muted = Color.rgb(156, 163, 175)
    private lateinit var root: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var content: LinearLayout
    private var cameraUri: Uri? = null
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

    private fun buildShell() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(dark)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(22, 18, 22, 18)
            setBackgroundColor(Color.rgb(5, 7, 11))
        }
        val title = TextView(this).apply {
            setTextColor(amber)
            textSize = 22f
            text = "SafeField"
            setTypeface(Typeface.DEFAULT_BOLD)
        }
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        val dash = Button(this).apply { text = "Menu"; setOnClickListener { showHome() } }
        header.addView(dash)
        scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 18, 18, 28) }
        scroll.addView(content)
        root.addView(header)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun clear() { content.removeAllViews(); fields.clear() }

    private fun label(s: String): TextView = TextView(this).apply {
        text = s
        textSize = 13f
        setTextColor(muted)
        setPadding(0, 14, 0, 5)
    }

    private fun input(key: String, hint: String, multi: Boolean = false): EditText {
        val e = EditText(this).apply {
            setTextColor(text)
            setHintTextColor(muted)
            setSingleLine(!multi)
            minLines = if (multi) 3 else 1
            setBackgroundColor(panel)
            setPadding(14, 10, 14, 10)
            this.hint = hint
        }
        fields[key] = e
        content.addView(label(hint))
        content.addView(e, LinearLayout.LayoutParams(-1, -2))
        return e
    }

    private fun button(s: String, onClick: () -> Unit): Button = Button(this).apply {
        text = s
        setOnClickListener { onClick() }
    }

    private fun section(s: String) {
        content.addView(TextView(this).apply {
            text = s
            textSize = 18f
            setTextColor(amber)
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(0, 22, 0, 10)
        })
    }

    private fun showHome() {
        clear()
        content.addView(TextView(this).apply {
            text = "Módulos"
            textSize = 24f
            setTextColor(text)
            setTypeface(Typeface.DEFAULT_BOLD)
        })
        val modules = listOf("Permissão de Trabalho", "APR", "DDS", "EPI", "Inspeção", "Ocorrência", "Colaboradores", "Dashboard")
        modules.forEach { m ->
            content.addView(button(m) { if (m == "Permissão de Trabalho") showPT() else showPlaceholder(m) }, LinearLayout.LayoutParams(-1, -2))
        }
    }

    private fun showPlaceholder(name: String) {
        clear()
        section(name)
        content.addView(TextView(this).apply {
            text = "Módulo nativo Kotlin preparado. A próxima etapa é detalhar este formulário."
            setTextColor(text)
            textSize = 15f
        })
        content.addView(button("Voltar") { showHome() })
    }

    private fun showPT() {
        clear()
        photos.clear(); workers.clear(); risks.clear(); checks.clear()
        section("Permissão de Trabalho")
        input("empresa", "Empresa / Planta")
        input("area", "Área / Setor")
        input("local", "Local da atividade")
        input("responsavel", "Responsável / Emissor")
        input("inicio", "Data/Hora início")
        input("fim", "Data/Hora fim / validade")
        input("executantes", "Equipe / executantes")
        input("descricao", "Descrição detalhada da atividade", true)
        input("ferramentas", "Ferramentas / equipamentos", true)
        input("produtos", "Substâncias / produtos")
        section("Tipo de atividade")
        riskBank.keys.forEach { act ->
            val cb = CheckBox(this).apply {
                text = act
                setTextColor(text)
                setOnCheckedChangeListener { _, checked ->
                    if (checked) risks.addAll(riskBank[act] ?: emptyList()) else risks.removeAll(riskBank[act] ?: emptyList())
                }
            }
            content.addView(cb)
        }
        input("atividadeManual", "Outra atividade / observação manual", true)
        section("Checklist de pré-requisitos")
        prereqs().forEachIndexed { i, item ->
            val cb = CheckBox(this).apply {
                text = item
                setTextColor(text)
                setOnCheckedChangeListener { _, checked -> checks[i] = checked }
            }
            content.addView(cb)
        }
        section("Emergência")
        input("ponto", "Ponto de encontro")
        input("telefone", "Ramal / telefone de emergência")
        input("emergencia", "Procedimento de emergência", true)
        section("Trabalhadores")
        val workerInput = input("worker", "Nome do trabalhador + função")
        content.addView(button("Adicionar trabalhador") {
            val v = workerInput.text.toString().trim()
            if (v.isNotEmpty()) { workers.add(v); workerInput.setText(""); toast("Trabalhador adicionado") }
        })
        section("Fotos")
        content.addView(button("Adicionar foto") { openPhotoChooser() })
        section("Finalizar")
        content.addView(button("Gerar e compartilhar PDF") { sharePtPdf() }, LinearLayout.LayoutParams(-1, -2))
        content.addView(button("Voltar") { showHome() })
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
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(i, "Compartilhar PT em PDF"))
    }

    private fun createPtPdf(): File {
        val file = File(cacheDir, "SafeField_PT_${System.currentTimeMillis()}.pdf")
        val doc = PdfDocument()
        var pageNo = 1
        var page = newPage(doc, pageNo)
        var canvas = page.canvas
        var y = 70f
        fun ensure(h: Float) {
            if (y + h > 790f) {
                doc.finishPage(page)
                pageNo++
                page = newPage(doc, pageNo)
                canvas = page.canvas
                y = 70f
            }
        }
        fun line(s: String, bold: Boolean = false, size: Float = 12f) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = size
                typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            wrapText(s, 82).forEach {
                ensure(18f)
                canvas.drawText(it, 38f, y, p)
                y += 18f
            }
        }
        fun title(s: String) {
            ensure(34f)
            val p = Paint().apply { color = dark }
            canvas.drawRect(30f, y - 18f, 565f, y + 8f, p)
            p.color = amber; p.textSize = 14f; p.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(s, 38f, y, p)
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
        val c = page.canvas
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = dark; c.drawRect(0f, 0f, 595f, 48f, p)
        p.color = amber; c.drawRect(0f, 0f, 10f, 842f, p)
        p.color = amber; p.textSize = 20f; p.typeface = Typeface.DEFAULT_BOLD
        c.drawText("SAFEFIELD", 36f, 31f, p)
        p.color = text; p.textSize = 12f; p.typeface = Typeface.DEFAULT
        c.drawText("Segurança do Trabalho em Campo", 170f, 31f, p)
        p.color = Color.GRAY; p.textSize = 10f
        c.drawText("Página $pageNo", 500f, 825f, p)
        return page
    }

    private fun wrapText(s: String, max: Int): List<String> {
        val words = s.replace("\n", " ").split(" ")
        val out = mutableListOf<String>()
        var line = ""
        for (w in words) {
            if ((line + " " + w).trim().length > max) { out.add(line); line = w } else line = (line + " " + w).trim()
        }
        if (line.isNotEmpty()) out.add(line)
        return if (out.isEmpty()) listOf("-") else out
    }

    private fun openPhotoChooser() {
        val i = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }
        startActivityForResult(Intent.createChooser(i, "Selecionar foto"), 200)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200 && resultCode == RESULT_OK) {
            data?.data?.let { photos.add(it); toast("Foto adicionada") }
        }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
