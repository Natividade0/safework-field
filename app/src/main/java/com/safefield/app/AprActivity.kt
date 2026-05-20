package com.safefield.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AprActivity : Activity() {
    private lateinit var repo: AprRepository
    private lateinit var data: AprData
    private var centralVisible = false
    private var signatureTarget: ((String) -> Unit)? = null
    private val signatureRequest = 8301
    private val photoRequest = 8302
    private val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

    override fun onCreate(savedInstanceState: Bundle?): Unit {
        super.onCreate(savedInstanceState)
        repo = AprRepository(this)
        data = repo.load()
        if (data.aprNumber.isBlank()) data.aprNumber = AprEngine.aprNumber(data.dateMillis)
        repo.save(data)
        showCentral()
    }

    override fun onPause(): Unit {
        super.onPause()
        if (::repo.isInitialized && ::data.isInitialized) repo.save(data)
    }

    override fun onBackPressed(): Unit {
        repo.save(data)
        if (centralVisible) finish() else showCentral()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?): Unit {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == signatureRequest && resultCode == RESULT_OK) {
            val signature = resultData?.getStringExtra(SignatureActivity.EXTRA_SIGNATURE_B64).orEmpty()
            if (signature.isNotBlank()) {
                signatureTarget?.invoke(signature)
                signatureTarget = null
                repo.save(data)
                Toast.makeText(this, "Assinatura salva", Toast.LENGTH_SHORT).show()
                showTeam()
            }
        }
        if (requestCode == photoRequest && resultCode == RESULT_OK) {
            val uri = resultData?.data
            if (uri != null) {
                try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
                data.photos.add(uri.toString())
                repo.save(data)
                showTeam()
            }
        }
    }

    private fun showCentral(): Unit {
        centralVisible = true
        val pending = AprValidator.pending(data)
        val ready = pending.isEmpty()
        val status = if (ready) "PRONTA PARA EMISSAO" else if (data.items.isEmpty()) "RASCUNHO" else "PENDENTE"
        val shell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Ui.SHELL) }
        val scroll = ScrollView(this)
        val root = Ui.vbox(this, Ui.dp(this, 16))

        val hero = Ui.heroCard(this)
        hero.addView(Ui.section(this, "SafeField"))
        hero.addView(Ui.title(this, "APR", 30f))
        hero.addView(Ui.label(this, "Analise Preliminar de Risco"))
        val top = Ui.row(this)
        top.addView(Ui.chip(this, status, if (ready) Ui.GREEN else Ui.AMBER))
        top.addView(Ui.chip(this, data.aprNumber, Ui.AMBER), lpWrapMargin(8, 0, 0, 0))
        hero.addView(top)
        root.addView(hero, spaced())

        val metrics = Ui.row(this)
        metrics.addView(metric("Pendencias", pending.size.toString(), if (ready) Ui.GREEN else Ui.RED), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(metric("Itens APR", data.items.size.toString(), Ui.AMBER), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(Ui.dp(this@AprActivity, 8), 0, 0, 0) })
        root.addView(metrics, spaced())

        val metrics2 = Ui.row(this)
        metrics2.addView(metric("Fotos", data.photos.size.toString(), Ui.AMBER), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        metrics2.addView(metric("Envolvidos", data.workers.size.toString(), Ui.GREEN), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(Ui.dp(this@AprActivity, 8), 0, 0, 0) })
        root.addView(metrics2, spaced())

        root.addView(actionCard("Dados da APR", "Identificacao, local e descricao") { showData() }, spaced())
        root.addView(actionCard("Atividades e riscos", "Geracao automatica por atividade e texto") { showRisks() }, spaced())
        root.addView(actionCard("Equipe e assinaturas", "Responsavel, envolvidos e evidencias") { showTeam() }, spaced())
        root.addView(actionCard("Revisar e gerar PDF", "Validacao final e compartilhamento") { showReview() }, spaced())

        val actions = Ui.row(this)
        actions.addView(Ui.ghostButton(this, "Historico de APRs").apply { setOnClickListener { showHistoryDialog() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        actions.addView(Ui.dangerButton(this, "Limpar rascunho").apply { setOnClickListener { clearDraft() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(Ui.dp(this@AprActivity, 8), 0, 0, 0) })
        root.addView(actions, spaced())

        scroll.addView(root)
        shell.addView(scroll)
        setContentView(shell)
        Ui.animateIn(root)
    }

    private fun screen(title: String, subtitle: String, build: (LinearLayout) -> Unit): Unit {
        centralVisible = false
        val shell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Ui.SHELL) }
        val header = Ui.row(this).apply { setPadding(Ui.dp(this@AprActivity, 16), Ui.dp(this@AprActivity, 12), Ui.dp(this@AprActivity, 16), Ui.dp(this@AprActivity, 10)); background = Ui.bg(Ui.DARK, 0, Ui.BORDER, 1) }
        header.addView(Ui.ghostButton(this, "Voltar").apply { setOnClickListener { repo.save(data); showCentral() } }, LinearLayout.LayoutParams(Ui.dp(this, 98), ViewGroup.LayoutParams.WRAP_CONTENT))
        val texts = Ui.vbox(this)
        texts.addView(Ui.title(this, title, 20f))
        texts.addView(Ui.label(this, subtitle))
        header.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(Ui.dp(this@AprActivity, 12), 0, 0, 0) })
        shell.addView(header)
        val scroll = ScrollView(this)
        val root = Ui.vbox(this, Ui.dp(this, 14))
        build(root)
        scroll.addView(root)
        shell.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(shell)
    }

    private fun showData(): Unit = screen("Dados da APR", "Identificacao e escopo da analise") { root ->
        val company = field(root, "Empresa / Planta", data.company)
        val area = field(root, "Area / Setor", data.area)
        val place = field(root, "Local", data.place)
        val responsible = field(root, "Responsavel", data.responsible)
        val description = field(root, "Descricao da atividade", data.activityDescription, true)
        val tools = field(root, "Ferramentas / equipamentos", data.tools, true)
        val observations = field(root, "Observacoes", data.observations, true)
        val dateCard = Ui.card(this)
        dateCard.addView(Ui.section(this, "Data e hora"))
        val dateValue = Ui.value(this, format.format(Date(data.dateMillis)), Ui.TEXT)
        dateCard.addView(dateValue)
        dateCard.addView(Ui.button(this, "Usar data/hora atual").apply { setOnClickListener { data.dateMillis = System.currentTimeMillis(); dateValue.text = format.format(Date(data.dateMillis)); repo.save(data) } }, buttonLp())
        root.addView(dateCard, spaced())
        root.addView(Ui.button(this, "Continuar para atividades e riscos").apply {
            setOnClickListener {
                data.company = company.text.toString(); data.area = area.text.toString(); data.place = place.text.toString(); data.responsible = responsible.text.toString()
                data.activityDescription = description.text.toString(); data.tools = tools.text.toString(); data.observations = observations.text.toString()
                repo.save(data); showRisks()
            }
        }, spaced())
    }

    private fun showRisks(): Unit = screen("Atividades e riscos", "APR automatica por atividade critica") { root ->
        val checks = linkedMapOf<String, CheckBox>()
        val card = Ui.card(this)
        card.addView(Ui.section(this, "Atividades criticas"))
        AprEngine.activities.forEach { name ->
            val check = CheckBox(this).apply { text = name; setTextColor(Ui.TEXT); textSize = 15f; isChecked = data.selectedActivities.contains(name) }
            checks[name] = check
            card.addView(check)
        }
        root.addView(card, spaced())
        val manual = field(root, "Atividade manual / complemento", data.manualActivity, true)
        root.addView(Ui.button(this, "Gerar APR automatica").apply {
            setOnClickListener {
                data.selectedActivities = checks.filter { it.value.isChecked }.keys.toMutableSet()
                data.manualActivity = manual.text.toString()
                data.items = AprEngine.generate(data).toMutableList()
                repo.save(data)
                Toast.makeText(this@AprActivity, "APR gerada com ${data.items.size} item(ns)", Toast.LENGTH_SHORT).show()
                showRisks()
            }
        }, spaced())
        root.addView(Ui.ghostButton(this, "Adicionar item manual").apply { setOnClickListener { manualItemDialog() } }, spaced())
        if (data.items.isEmpty()) root.addView(messageCard("Nenhum item APR gerado ainda."), spaced()) else data.items.forEachIndexed { index, item -> root.addView(aprItemCard(index, item), spaced()) }
    }

    private fun showTeam(): Unit = screen("Equipe e evidencias", "Assinaturas e fotos da APR") { root ->
        val responsible = Ui.card(this)
        responsible.addView(Ui.section(this, "Assinatura responsavel"))
        responsible.addView(Ui.value(this, if (data.responsibleSignatureB64.isBlank()) "Assinatura do responsavel pendente" else "Assinatura do responsavel salva", if (data.responsibleSignatureB64.isBlank()) Ui.RED else Ui.GREEN))
        responsible.addView(Ui.button(this, if (data.responsibleSignatureB64.isBlank()) "Assinar responsavel" else "Reassinar responsavel").apply { setOnClickListener { openSignature("Assinatura do responsavel") { data.responsibleSignatureB64 = it } } }, buttonLp())
        root.addView(responsible, spaced())

        val add = Ui.card(this)
        add.addView(Ui.section(this, "Adicionar envolvido"))
        val name = Ui.input(this, "Nome")
        val role = Ui.input(this, "Funcao / empresa")
        add.addView(name, buttonLp()); add.addView(role, buttonLp())
        add.addView(Ui.button(this, "Adicionar envolvido").apply { setOnClickListener { if (name.text.toString().isBlank()) Toast.makeText(this@AprActivity, "Informe o nome do envolvido", Toast.LENGTH_SHORT).show() else { data.workers.add(AprWorker(name.text.toString(), role.text.toString())); repo.save(data); showTeam() } } }, buttonLp())
        root.addView(add, spaced())

        data.workers.forEachIndexed { index, worker -> root.addView(workerCard(index, worker), spaced()) }
        val photos = Ui.card(this)
        photos.addView(Ui.section(this, "Evidencias fotograficas"))
        photos.addView(Ui.value(this, "${data.photos.size} foto(s) anexada(s)", Ui.TEXT))
        photos.addView(Ui.button(this, "Anexar foto").apply { setOnClickListener { pickPhoto() } }, buttonLp())
        root.addView(photos, spaced())
    }

    private fun showReview(): Unit = screen("Revisao da APR", "Validacao final e emissao") { root ->
        val pending = AprValidator.pending(data)
        val summary = Ui.card(this)
        summary.addView(Ui.section(this, "Resumo"))
        summary.addView(line("Numero", data.aprNumber))
        summary.addView(line("Empresa", data.company.ifBlank { "Pendente" }))
        summary.addView(line("Local", data.place.ifBlank { "Pendente" }))
        summary.addView(line("Responsavel", data.responsible.ifBlank { "Pendente" }))
        summary.addView(line("Itens APR", data.items.size.toString()))
        summary.addView(line("Envolvidos", data.workers.size.toString()))
        root.addView(summary, spaced())
        val pend = Ui.card(this)
        pend.addView(Ui.section(this, "Pendencias"))
        if (pending.isEmpty()) pend.addView(Ui.value(this, "APR pronta para emissao.", Ui.GREEN)) else pending.forEach { pend.addView(Ui.value(this, it, Ui.RED)) }
        root.addView(pend, spaced())
        root.addView(Ui.button(this, "Gerar PDF da APR").apply { setOnClickListener { generatePdf() } }, spaced())
    }

    private fun aprItemCard(index: Int, item: AprItem): LinearLayout {
        val card = Ui.card(this)
        val top = Ui.row(this)
        top.addView(Ui.chip(this, item.classification, AprEngine.statusColor(item.classification)))
        top.addView(Ui.value(this, item.activity, Ui.TEXT), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(Ui.dp(this@AprActivity, 8), 0, 0, 0) })
        card.addView(top)
        listOf("Perigo" to item.danger, "Risco" to item.risk, "Consequencia" to item.consequence, "Controle" to item.control, "EPI" to item.epi, "Severidade" to AprEngine.severityLabel(item.severity), "Probabilidade" to AprEngine.probabilityLabel(item.probability)).forEach { card.addView(line(it.first, it.second)) }
        card.addView(Ui.ghostButton(this, "Remover item").apply { setOnClickListener { data.items.removeAt(index); repo.save(data); showRisks() } }, buttonLp())
        return card
    }

    private fun workerCard(index: Int, worker: AprWorker): LinearLayout {
        val card = Ui.card(this)
        card.addView(Ui.section(this, "Envolvido ${index + 1}"))
        card.addView(Ui.value(this, worker.name, Ui.TEXT))
        card.addView(Ui.label(this, worker.role.ifBlank { "Funcao nao informada" }))
        card.addView(Ui.value(this, if (worker.signatureB64.isBlank()) "Assinatura pendente" else "Assinatura salva", if (worker.signatureB64.isBlank()) Ui.RED else Ui.GREEN))
        val actions = Ui.row(this)
        actions.addView(Ui.button(this, if (worker.signatureB64.isBlank()) "Assinar" else "Reassinar").apply { setOnClickListener { openSignature("Assinatura de ${worker.name}") { signature -> data.workers[index] = worker.copy(signatureB64 = signature, signedAt = format.format(Date())) } } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        actions.addView(Ui.dangerButton(this, "Remover").apply { setOnClickListener { data.workers.removeAt(index); repo.save(data); showTeam() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(Ui.dp(this@AprActivity, 8), 0, 0, 0) })
        card.addView(actions, buttonLp())
        return card
    }

    private fun generatePdf(): Unit {
        val pending = AprValidator.pending(data)
        if (pending.isNotEmpty()) { Toast.makeText(this, pending.first(), Toast.LENGTH_LONG).show(); return }
        val file = AprPdfGenerator(this, repo).generate(data)
        data.history.add(0, AprHistoryItem(data.aprNumber, format.format(Date()), data.company, data.place, data.responsible, file.name, "EMITIDA"))
        data.history = data.history.take(20).toMutableList()
        repo.save(data)
        shareFile(file)
    }

    private fun showHistoryDialog(): Unit {
        val panel = Ui.vbox(this, Ui.dp(this, 14))
        panel.setBackgroundColor(Ui.SHELL)
        panel.addView(Ui.title(this, "Historico de APRs", 22f))
        if (data.history.isEmpty()) panel.addView(Ui.value(this, "Nenhuma APR emitida ainda.", Ui.MUTED), spaced()) else data.history.forEach { item ->
            val card = Ui.card(this)
            card.addView(Ui.section(this, item.status))
            card.addView(Ui.value(this, item.aprNumber, Ui.AMBER))
            card.addView(Ui.label(this, "${item.company} - ${item.place}"))
            card.addView(Ui.label(this, "Emitida em ${item.emittedAt} por ${item.responsible}"))
            val actions = Ui.row(this)
            actions.addView(Ui.ghostButton(this, "Compartilhar").apply { setOnClickListener { val file = File(cacheDir, item.fileName); if (file.exists()) shareFile(file) else Toast.makeText(this@AprActivity, "Arquivo nao encontrado neste aparelho", Toast.LENGTH_SHORT).show() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            actions.addView(Ui.button(this, "Duplicar").apply { setOnClickListener { duplicateApr() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(Ui.dp(this@AprActivity, 8), 0, 0, 0) })
            card.addView(actions, buttonLp())
            panel.addView(card, spaced())
        }
        AlertDialog.Builder(this).setView(panel).setPositiveButton("Fechar", null).show()
    }

    private fun duplicateApr(): Unit {
        val history = data.history
        data = data.copy(aprNumber = "APR-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale("pt", "BR")).format(Date())}", dateMillis = System.currentTimeMillis(), workers = data.workers.map { AprWorker(it.name, it.role) }.toMutableList(), photos = mutableListOf(), responsibleSignatureB64 = "", history = history)
        repo.save(data)
        Toast.makeText(this, "APR duplicada como novo rascunho", Toast.LENGTH_SHORT).show()
        showCentral()
    }

    private fun clearDraft(): Unit {
        AlertDialog.Builder(this).setTitle("Limpar rascunho da APR?").setMessage("Historico emitido sera mantido.").setPositiveButton("Limpar") { _, _ -> data = repo.clearDraftKeepHistory(data); showCentral() }.setNegativeButton("Cancelar", null).show()
    }

    private fun manualItemDialog(): Unit {
        val panel = Ui.vbox(this, Ui.dp(this, 14))
        val activity = Ui.input(this, "Atividade")
        val risk = Ui.input(this, "Risco")
        val control = Ui.input(this, "Controle", true)
        panel.addView(activity, buttonLp()); panel.addView(risk, buttonLp()); panel.addView(control, buttonLp())
        AlertDialog.Builder(this).setTitle("Item manual da APR").setView(panel).setPositiveButton("Adicionar") { _, _ ->
            if (risk.text.toString().isBlank()) Toast.makeText(this, "Informe o risco", Toast.LENGTH_SHORT).show() else {
                data.items.add(AprItem(activity.text.toString().ifBlank { "Atividade manual" }, "Perigo identificado em campo", risk.text.toString(), "Lesao ou perda operacional", control.text.toString().ifBlank { "Controle definido pelo responsavel" }, "EPI definido conforme avaliacao", 3, 2, "ALTO"))
                repo.save(data); showRisks()
            }
        }.setNegativeButton("Cancelar", null).show()
    }

    private fun openSignature(title: String, onSaved: (String) -> Unit): Unit {
        signatureTarget = onSaved
        val intent = Intent(this, SignatureActivity::class.java)
        intent.putExtra(SignatureActivity.EXTRA_TITLE, title)
        startActivityForResult(intent, signatureRequest)
    }

    private fun pickPhoto(): Unit {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        startActivityForResult(intent, photoRequest)
    }

    private fun shareFile(file: File): Unit {
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val share = Intent(Intent.ACTION_SEND)
        share.type = "application/pdf"
        share.putExtra(Intent.EXTRA_STREAM, uri)
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(share, "Compartilhar APR SafeField"))
    }

    private fun field(root: LinearLayout, label: String, value: String, multi: Boolean = false): EditText {
        val card = Ui.card(this)
        card.addView(Ui.section(this, label))
        val edit = Ui.input(this, label, multi)
        edit.setText(value)
        card.addView(edit, buttonLp())
        root.addView(card, spaced())
        return edit
    }

    private fun metric(label: String, value: String, color: Int): LinearLayout {
        val card = Ui.card(this)
        card.gravity = Gravity.CENTER
        card.addView(Ui.value(this, value, color).apply { textSize = 24f; gravity = Gravity.CENTER })
        card.addView(Ui.label(this, label).apply { gravity = Gravity.CENTER })
        return card
    }

    private fun actionCard(title: String, subtitle: String, action: () -> Unit): LinearLayout {
        val card = Ui.card(this)
        card.addView(Ui.value(this, title, Ui.TEXT))
        card.addView(Ui.label(this, subtitle))
        card.setOnClickListener { action() }
        return card
    }

    private fun messageCard(message: String): LinearLayout = Ui.card(this).apply { addView(Ui.value(this@AprActivity, message, Ui.MUTED)) }
    private fun line(label: String, value: String): TextView = Ui.value(this, "$label: $value", Ui.TEXT)
    private fun spaced(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, Ui.dp(this@AprActivity, 12)) }
    private fun buttonLp(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, Ui.dp(this@AprActivity, 10), 0, 0) }
    private fun lpWrapMargin(l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(Ui.dp(this@AprActivity, l), Ui.dp(this@AprActivity, t), Ui.dp(this@AprActivity, r), Ui.dp(this@AprActivity, b)) }
}
