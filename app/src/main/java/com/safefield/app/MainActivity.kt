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
    private val green = Color.rgb(34, 197, 94)
    private val red = Color.rgb(239, 68, 68)
    private lateinit var root: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var content: LinearLayout
    private var statusView: TextView? = null
    private var summaryView: TextView? = null
    private var workerListView: TextView? = null
    private var photoStatusView: TextView? = null
    private val photos = mutableListOf<Uri>()
    private val workers = mutableListOf<String>()
    private val selectedActivities = mutableSetOf<String>()
    private val risks = mutableSetOf<String>()
    private val controlStatus = mutableMapOf<String, String>()
    private val checklistStatus = mutableMapOf<Int, String>()
    private val fields = mutableMapOf<String, EditText>()

    private val riskBank = mapOf(
        "Içamento de carga" to listOf("Raio de fogo/carga suspensa", "Queda de objetos", "Tombamento", "Prensagem"),
        "Trabalho em altura" to listOf("Queda de pessoas", "Queda de objetos", "Acesso inseguro", "Ancoragem inadequada"),
        "Escavação" to listOf("Desmoronamento", "Interferência subterrânea", "Queda de pessoas", "Atropelamento"),
        "Trabalho a quente" to listOf("Incêndio/explosão", "Queimadura", "Fumos metálicos", "Projeção de partículas"),
        "Eletricidade / LOTO" to listOf("Choque elétrico", "Arco elétrico", "Energia residual", "Bloqueio inadequado"),
        "Montagem industrial" to listOf("Prensagem", "Corte/perfuração", "Queda de objetos", "Ergonômicos")
    )
    private val controlBank = mapOf(
        "Raio de fogo/carga suspensa" to listOf("Isolar área de içamento", "Proibir permanência sob carga", "Comunicação com operador e sinaleiro"),
        "Queda de objetos" to listOf("Isolamento inferior", "Amarração de ferramentas", "Capacete com jugular"),
        "Tombamento" to listOf("Verificar patolamento/base", "Avaliar solo e nivelamento", "Respeitar capacidade do equipamento"),
        "Prensagem" to listOf("Manter mãos fora da linha de fogo", "Usar luvas adequadas", "Comunicação entre executantes"),
        "Queda de pessoas" to listOf("Cinto tipo paraquedista", "Ancoragem válida", "Acesso seguro"),
        "Acesso inseguro" to listOf("Inspecionar escadas/andaimes", "Manter circulação desobstruída", "Bloquear acesso não autorizado"),
        "Ancoragem inadequada" to listOf("Verificar ponto de ancoragem", "Usar talabarte adequado", "Registrar liberação do acesso"),
        "Desmoronamento" to listOf("Taludamento/escoramento", "Afastar material da borda", "Inspecionar solo"),
        "Interferência subterrânea" to listOf("Consultar interferências", "Escavação manual próxima a redes", "Sinalizar interferências"),
        "Atropelamento" to listOf("Sinalização de tráfego", "Colete refletivo", "Rotas separadas"),
        "Incêndio/explosão" to listOf("Remover inflamáveis", "Extintor no local", "Vigia de fogo"),
        "Queimadura" to listOf("Luvas de raspa", "Proteção facial", "Delimitar área quente"),
        "Fumos metálicos" to listOf("Ventilação/exaustão", "Máscara adequada", "Avaliar exposição"),
        "Projeção de partículas" to listOf("Óculos de segurança", "Protetor facial", "Anteparo físico"),
        "Choque elétrico" to listOf("Bloqueio e etiquetagem", "Teste de ausência de tensão", "Ferramenta isolada"),
        "Arco elétrico" to listOf("EPI NR-10 adequado", "Distância segura", "Painel bloqueado"),
        "Energia residual" to listOf("Alívio de energia residual", "Teste após bloqueio", "Controle de religamento"),
        "Bloqueio inadequado" to listOf("Cadeado individual", "Etiqueta identificada", "Lista de pontos bloqueados"),
        "Corte/perfuração" to listOf("Luvas adequadas", "Ferramenta em bom estado", "Manter proteção da ferramenta"),
        "Ergonômicos" to listOf("Revezamento", "Postura adequada", "Uso de apoio mecânico")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        }
        buildShell(); showHome()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun bg(color: Int, radius: Int = 18, strokeColor: Int? = null, strokeWidth: Int = 1) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); if (strokeColor != null) setStroke(dp(strokeWidth), strokeColor)
    }

    private fun buildShell() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(dark) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18)); setBackgroundColor(shell) }
        val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleBox.addView(TextView(this).apply { setTextColor(amber); textSize = 30f; text = "SafeField"; setTypeface(Typeface.DEFAULT_BOLD); includeFontPadding = false })
        titleBox.addView(TextView(this).apply { setTextColor(muted); textSize = 13f; text = "Segurança do Trabalho em campo"; setPadding(0, dp(4), 0, 0) })
        header.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(chipButton("Menu") { showHome() })
        scroll = ScrollView(this).apply { setBackgroundColor(dark) }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(18), dp(16), dp(28)) }
        scroll.addView(content); root.addView(header); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
    }

    private fun clear() { content.removeAllViews(); fields.clear() }
    private fun addTitle(title: String, subtitle: String? = null) {
        content.addView(TextView(this).apply { text = title; textSize = 27f; setTextColor(textColor); setTypeface(Typeface.DEFAULT_BOLD); includeFontPadding = false; setPadding(0, 0, 0, dp(6)) })
        if (subtitle != null) content.addView(TextView(this).apply { text = subtitle; textSize = 14f; setTextColor(muted); setPadding(0, 0, 0, dp(14)) })
    }
    private fun card() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = bg(cardColor, 18, Color.rgb(39, 45, 57), 1); setPadding(dp(16), dp(14), dp(16), dp(16)) }
    private fun addCard(view: LinearLayout) { content.addView(view, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(12)) }) }
    private fun label(s: String) = TextView(this).apply { text = s; textSize = 12f; setTextColor(muted); setTypeface(Typeface.DEFAULT_BOLD); setPadding(0, dp(10), 0, dp(5)) }
    private fun input(parent: LinearLayout, key: String, hint: String, multi: Boolean = false): EditText {
        val e = EditText(this).apply { setTextColor(textColor); setHintTextColor(Color.rgb(100, 110, 125)); setSingleLine(!multi); minLines = if (multi) 3 else 1; background = bg(panel, 14, Color.rgb(45, 52, 66), 1); setPadding(dp(14), dp(10), dp(14), dp(10)); this.hint = hint; textSize = 14f }
        fields[key] = e; parent.addView(label(hint)); parent.addView(e, LinearLayout.LayoutParams(-1, -2)); return e
    }
    private fun primaryButton(s: String, onClick: () -> Unit) = Button(this).apply { text = s; textSize = 15f; setTextColor(Color.BLACK); setTypeface(Typeface.DEFAULT_BOLD); background = bg(amber, 16); setPadding(dp(12), dp(10), dp(12), dp(10)); setOnClickListener { onClick() } }
    private fun secondaryButton(s: String, onClick: () -> Unit) = Button(this).apply { text = s; textSize = 14f; setTextColor(textColor); background = bg(Color.rgb(28, 33, 43), 16, Color.rgb(49, 57, 72), 1); setPadding(dp(12), dp(10), dp(12), dp(10)); setOnClickListener { onClick() } }
    private fun chipButton(s: String, onClick: () -> Unit) = Button(this).apply { text = s; textSize = 13f; setTextColor(amber); setTypeface(Typeface.DEFAULT_BOLD); background = bg(Color.rgb(20, 24, 32), 18, Color.rgb(50, 56, 70), 1); setPadding(dp(12), dp(8), dp(12), dp(8)); setOnClickListener { onClick() } }
    private fun section(parent: LinearLayout, s: String) { parent.addView(TextView(this).apply { text = s; textSize = 17f; setTextColor(amber); setTypeface(Typeface.DEFAULT_BOLD); setPadding(0, dp(10), 0, dp(8)) }) }

    private fun showHome() {
        clear(); addTitle("Módulos", "Base nativa Kotlin • sem WebView • foco na PT")
        listOf("Permissão de Trabalho", "APR", "DDS", "EPI", "Inspeção", "Ocorrência", "Colaboradores", "Dashboard").forEach { name ->
            val c = card(); c.addView(TextView(this).apply { text = name; textSize = 18f; setTextColor(if (name == "Permissão de Trabalho") amber else textColor); setTypeface(Typeface.DEFAULT_BOLD) })
            c.addView(TextView(this).apply { text = if (name == "Permissão de Trabalho") "PT inteligente com riscos, controles, checklist e PDF nativo" else "Módulo preparado para expansão"; textSize = 13f; setTextColor(muted); setPadding(0, dp(5), 0, dp(12)) })
            c.addView(if (name == "Permissão de Trabalho") primaryButton("Abrir") { showPT() } else secondaryButton("Abrir") { showPlaceholder(name) }); addCard(c)
        }
    }
    private fun showPlaceholder(name: String) { clear(); addTitle(name, "Módulo nativo preparado para expansão."); val c = card(); c.addView(TextView(this).apply { text = "A estrutura Kotlin já está pronta."; setTextColor(textColor); textSize = 15f }); c.addView(secondaryButton("Voltar ao menu") { showHome() }); addCard(c) }

    private fun showPT() {
        clear(); photos.clear(); workers.clear(); risks.clear(); selectedActivities.clear(); controlStatus.clear(); checklistStatus.clear(); statusView = null; summaryView = null; workerListView = null; photoStatusView = null
        addTitle("Permissão de Trabalho", "PT nativa seguindo o modelo que vínhamos construindo")
        val statusCard = card(); statusView = TextView(this).apply { textSize = 16f; setTypeface(Typeface.DEFAULT_BOLD); setPadding(0, 0, 0, dp(8)) }; summaryView = TextView(this).apply { setTextColor(muted); textSize = 13f }
        statusCard.addView(statusView); statusCard.addView(summaryView); addCard(statusCard)
        val c1 = card(); section(c1, "1. Informações gerais"); input(c1, "empresa", "Empresa / Planta"); input(c1, "area", "Área / Setor"); input(c1, "local", "Local da atividade"); input(c1, "responsavel", "Responsável / Emissor"); input(c1, "inicio", "Data/Hora início"); input(c1, "fim", "Data/Hora fim / validade"); input(c1, "executantes", "Equipe / executantes"); input(c1, "descricao", "Descrição detalhada da atividade", true); input(c1, "ferramentas", "Ferramentas / equipamentos", true); input(c1, "produtos", "Substâncias / produtos"); addCard(c1)
        val c2 = card(); section(c2, "2. Atividades críticas e riscos automáticos")
        riskBank.keys.forEach { act -> c2.addView(CheckBox(this).apply { text = act; textSize = 14f; setTextColor(textColor); buttonTintList = android.content.res.ColorStateList.valueOf(amber); setOnCheckedChangeListener { _, checked -> if (checked) selectedActivities.add(act) else selectedActivities.remove(act); rebuildRisks(); refreshStatus() } }) }
        input(c2, "atividadeManual", "Outra atividade / observação manual", true); addCard(c2)
        val c3 = card(); section(c3, "3. Checklist geral — Sim / Não / N/A"); prereqs().forEachIndexed { i, item -> c3.addView(statusRow(item) { value -> checklistStatus[i] = value; refreshStatus() }) }; addCard(c3)
        val c4 = card(); section(c4, "4. Medidas de controle por risco"); val risksBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; c4.addView(risksBox); c4.tag = risksBox; addCard(c4)
        val c5 = card(); section(c5, "5. Emergência"); input(c5, "ponto", "Ponto de encontro"); input(c5, "telefone", "Ramal / telefone de emergência"); input(c5, "emergencia", "Procedimento de emergência", true); addCard(c5)
        val c6 = card(); section(c6, "6. Trabalhadores e fotos"); val workerInput = input(c6, "worker", "Nome do trabalhador + função"); c6.addView(secondaryButton("Adicionar trabalhador") { val value = workerInput.text.toString().trim(); if (value.isNotEmpty()) { workers.add(value); workerInput.setText(""); updateLists(); refreshStatus() } }); workerListView = TextView(this).apply { setTextColor(muted); textSize = 13f; setPadding(0, dp(8), 0, dp(8)) }; c6.addView(workerListView); c6.addView(secondaryButton("Adicionar foto") { openPhotoChooser() }); photoStatusView = TextView(this).apply { setTextColor(muted); textSize = 13f; setPadding(0, dp(8), 0, 0) }; c6.addView(photoStatusView); addCard(c6)
        val c7 = card(); section(c7, "7. Finalizar"); c7.addView(primaryButton("Gerar e compartilhar PDF") { sharePtPdf() }, LinearLayout.LayoutParams(-1, -2)); c7.addView(secondaryButton("Voltar ao menu") { showHome() }); addCard(c7)
        rebuildRisks(); updateLists(); refreshStatus()
    }

    private fun statusRow(label: String, onChange: (String) -> Unit): LinearLayout {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = bg(panel, 14, Color.rgb(45, 52, 66), 1); setPadding(dp(10), dp(8), dp(10), dp(8)) }
        box.addView(TextView(this).apply { text = label; setTextColor(textColor); textSize = 13f })
        val row = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        listOf("sim" to "Sim", "nao" to "Não", "na" to "N/A").forEach { (value, title) -> row.addView(RadioButton(this).apply { text = title; setTextColor(textColor); buttonTintList = android.content.res.ColorStateList.valueOf(amber); setOnClickListener { onChange(value) } }) }
        box.addView(row); return box
    }
    private fun rebuildRisks() { risks.clear(); selectedActivities.forEach { risks.addAll(riskBank[it] ?: emptyList()) } }
    private fun updateLists() { workerListView?.text = if (workers.isEmpty()) "Nenhum trabalhador adicionado." else workers.joinToString("\n") { "• $it" }; photoStatusView?.text = "Fotos anexadas: ${photos.size}" }
    private fun refreshStatus() {
        val pendingChecks = prereqs().indices.count { checklistStatus[it].isNullOrBlank() }
        val badChecks = checklistStatus.values.count { it == "nao" }
        val pendingControls = risks.count { risk -> (controlBank[risk] ?: emptyList()).any { controlStatus["$risk|$it"].isNullOrBlank() } }
        val blocked = pendingChecks > 0 || badChecks > 0 || pendingControls > 0 || workers.isEmpty()
        statusView?.setTextColor(if (blocked) red else green); statusView?.text = if (blocked) "PT BLOQUEADA" else "PT LIBERADA"
        summaryView?.text = "Atividades: ${selectedActivities.size} • Riscos: ${risks.size} • Checklist pendente: $pendingChecks • Controles pendentes: $pendingControls • Trabalhadores: ${workers.size} • Fotos: ${photos.size}"
    }
    private fun prereqs() = listOf("APR válida emitida para esta atividade", "EPIs específicos inspecionados e aprovados", "Treinamento NR em dia para todos os envolvidos", "Área isolada e sinalizada adequadamente", "Responsável presente no local antes do início")
    private fun v(key: String) = fields[key]?.text?.toString()?.trim().orEmpty()

    private fun sharePtPdf() { val pdf = createPtPdf(); val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", pdf); startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Compartilhar PT em PDF")) }
    private fun createPtPdf(): File { val file = File(cacheDir, "SafeField_PT_${System.currentTimeMillis()}.pdf"); val doc = PdfDocument(); var pageNo = 1; var page = newPage(doc, pageNo); var canvas = page.canvas; var y = 70f
        fun ensure(h: Float) { if (y + h > 790f) { doc.finishPage(page); pageNo++; page = newPage(doc, pageNo); canvas = page.canvas; y = 70f } }
        fun line(value: String, bold: Boolean = false, size: Float = 12f) { val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = size; typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT }; wrapText(value, 82).forEach { ensure(18f); canvas.drawText(it, 38f, y, paint); y += 18f } }
        fun title(value: String) { ensure(34f); val paint = Paint().apply { color = dark }; canvas.drawRect(30f, y - 18f, 565f, y + 8f, paint); paint.color = amber; paint.textSize = 14f; paint.typeface = Typeface.DEFAULT_BOLD; canvas.drawText(value, 38f, y, paint); y += 28f }
        title("PERMISSÃO DE TRABALHO"); line(statusView?.text?.toString().orEmpty(), true, 13f); title("1. Informações gerais"); listOf("Empresa/Planta" to v("empresa"), "Área/Setor" to v("area"), "Local" to v("local"), "Responsável/Emissor" to v("responsavel"), "Início" to v("inicio"), "Fim/Validade" to v("fim"), "Equipe/Executantes" to v("executantes"), "Descrição" to v("descricao"), "Ferramentas" to v("ferramentas"), "Produtos" to v("produtos")).forEach { line("${it.first}: ${it.second}") }
        title("2. Checklist geral"); prereqs().forEachIndexed { i, item -> line("${statusLabel(checklistStatus[i])} $item") }
        title("3. Riscos e medidas de controle"); if (risks.isEmpty()) line("Nenhum risco selecionado.") else risks.forEach { r -> line("Risco: $r", true); (controlBank[r] ?: emptyList()).forEach { c -> line("${statusLabel(controlStatus["$r|$c"])} $c") } }
        title("4. Emergência"); line("Ponto: ${v("ponto")}"); line("Telefone/Ramal: ${v("telefone")}"); line("Procedimento: ${v("emergencia")}")
        title("5. Trabalhadores e evidências"); if (workers.isEmpty()) line("Nenhum trabalhador informado.") else workers.forEach { line("• $it") }; line("Fotos anexadas: ${photos.size}")
        title("6. Encerramento"); line("Atividade concluída: ___ Sim  ___ Não"); line("Área limpa e segura: ___ Sim  ___ Não"); line("Responsável pelo encerramento: ______________________________")
        doc.finishPage(page); FileOutputStream(file).use { doc.writeTo(it) }; doc.close(); return file }
    private fun statusLabel(v: String?) = when (v) { "sim" -> "[X]"; "nao" -> "[NÃO]"; "na" -> "[N/A]"; else -> "[ ]" }
    private fun newPage(doc: PdfDocument, pageNo: Int): PdfDocument.Page { val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNo).create()); val c = page.canvas; val p = Paint(Paint.ANTI_ALIAS_FLAG); p.color = dark; c.drawRect(0f, 0f, 595f, 48f, p); p.color = amber; c.drawRect(0f, 0f, 10f, 842f, p); p.color = amber; p.textSize = 20f; p.typeface = Typeface.DEFAULT_BOLD; c.drawText("SAFEFIELD", 36f, 31f, p); p.color = textColor; p.textSize = 12f; p.typeface = Typeface.DEFAULT; c.drawText("Segurança do Trabalho em Campo", 170f, 31f, p); p.color = Color.GRAY; p.textSize = 10f; c.drawText("Página $pageNo", 500f, 825f, p); return page }
    private fun wrapText(value: String, max: Int): List<String> { val words = value.replace("\n", " ").split(" "); val out = mutableListOf<String>(); var line = ""; for (word in words) { if ((line + " " + word).trim().length > max) { out.add(line); line = word } else line = (line + " " + word).trim() }; if (line.isNotEmpty()) out.add(line); return if (out.isEmpty()) listOf("-") else out }
    private fun openPhotoChooser() { startActivityForResult(Intent.createChooser(Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }, "Selecionar foto"), 200) }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { super.onActivityResult(requestCode, resultCode, data); if (requestCode == 200 && resultCode == RESULT_OK) { data?.data?.let { photos.add(it); updateLists(); refreshStatus(); toast("Foto adicionada") } } }
    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
}
