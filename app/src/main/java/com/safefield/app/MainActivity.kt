package com.safefield.app

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var repo: PtRepository
    private lateinit var data: PtData
    private val photoRequest = 7001

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); repo = PtRepository(this); data = repo.load(); if (data.startMillis <= 0L) setNow(); showHome() }
    override fun onPause() { super.onPause(); repo.save(data) }
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == photoRequest && resultCode == RESULT_OK) {
            val clip = resultData?.clipData
            if (clip != null) for (i in 0 until clip.itemCount) addPhoto(clip.getItemAt(i).uri) else resultData?.data?.let { addPhoto(it) }
            repo.save(data); showTeamEvidence()
        }
    }

    private fun addPhoto(uri: Uri) { runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }; data.photoUris.add(uri.toString()) }
    private fun setNow() { data.startMillis = System.currentTimeMillis(); data.endMillis = data.startMillis + data.validityHours * 60L * 60L * 1000L }
    private fun saveAnd(block: () -> Unit) { repo.save(data); block() }

    private fun screen(title: String, back: (() -> Unit)? = null, body: LinearLayout.() -> Unit) {
        val scroll = ScrollView(this); scroll.setBackgroundColor(Ui.SHELL)
        val content = Ui.vbox(this, Ui.dp(this, 16)); scroll.addView(content)
        val header = Ui.row(this)
        if (back != null) header.addView(Ui.ghostButton(this, "Voltar").apply { setOnClickListener { saveAnd(back) }; layoutParams = LinearLayout.LayoutParams(Ui.dp(this@MainActivity, 96), ViewGroup.LayoutParams.WRAP_CONTENT) })
        header.addView(Ui.title(this, title).apply { gravity = Gravity.CENTER_VERTICAL; layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) })
        content.addView(header); content.body(); setContentView(scroll)
    }

    private fun showHome() = screen("SafeField") {
        addView(Ui.label(this@MainActivity, "Segurança do Trabalho em Campo"))
        listOf("Permissão de Trabalho", "APR", "DDS", "EPI", "Inspeção", "Ocorrência", "Colaboradores", "Dashboard").forEach { name ->
            addView(Ui.card(this@MainActivity).apply { addView(Ui.title(this@MainActivity, name, 18f)); addView(Ui.label(this@MainActivity, if (name == "Permissão de Trabalho") "Central completa para emissão de PT" else "Módulo preparado para expansão")); setOnClickListener { if (name == "Permissão de Trabalho") showPtCentral() else placeholder(name) } }.margin(6.dp()))
        }
    }

    private fun placeholder(name: String) = screen(name, ::showHome) { addView(Ui.card(this@MainActivity).apply { addView(Ui.title(this@MainActivity, "Módulo preparado para expansão", 18f)); addView(Ui.label(this@MainActivity, "Base pronta para evoluir este módulo.")) }.margin(6.dp())) }

    private fun showPtCentral() = screen("Central da PT", ::showHome) {
        val pending = RiskEngine.pending(data)
        addView(Ui.card(this@MainActivity).apply { addView(Ui.title(this@MainActivity, if (pending.isEmpty()) "PT LIBERADA" else "PT BLOQUEADA", 20f).apply { setTextColor(if (pending.isEmpty()) Ui.GREEN else Ui.RED) }); addView(Ui.label(this@MainActivity, if (pending.isEmpty()) "Todas as etapas estão conformes." else "${pending.size} pendência(s) impedem a emissão.")); pending.take(6).forEach { addView(Ui.value(this@MainActivity, "- $it", Ui.RED)) } }.margin(6.dp()))
        step("1. Dados do serviço", serviceDone(), ::showServiceData); step("2. Atividades e riscos", risksDone(), ::showRisks); step("3. Checklist de liberação", checklistDone(), ::showChecklist); step("4. Equipe e evidências", teamDone(), ::showTeamEvidence); step("5. Revisão e emissão", pending.isEmpty(), ::showReview)
        addView(Ui.button(this@MainActivity, "Revisar e gerar PDF").apply { setOnClickListener { showReview() } }.margin(6.dp()))
        addView(Ui.ghostButton(this@MainActivity, "Limpar rascunho").apply { setOnClickListener { AlertDialog.Builder(this@MainActivity).setTitle("Limpar rascunho?").setMessage("Todos os dados, fotos e assinatura salvos serão removidos.").setNegativeButton("Cancelar", null).setPositiveButton("Limpar") { _, _ -> data = PtData(); setNow(); repo.clear(); showPtCentral() }.show() } }.margin(6.dp()))
        addView(Ui.title(this@MainActivity, "Últimas PTs emitidas", 18f).margin(8.dp()))
        if (data.history.isEmpty()) addView(Ui.label(this@MainActivity, "Nenhuma PT emitida neste aparelho."))
        data.history.take(5).forEach { h -> addView(Ui.card(this@MainActivity).apply { addView(Ui.value(this@MainActivity, h.emittedAt)); addView(Ui.label(this@MainActivity, "${h.place} - ${h.fileName}")) }.margin(4.dp())) }
    }

    private fun LinearLayout.step(text: String, done: Boolean, action: () -> Unit) { addView(Ui.card(this@MainActivity).apply { val r = Ui.row(this@MainActivity); r.addView(Ui.title(this@MainActivity, text, 17f).apply { layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }); r.addView(Ui.value(this@MainActivity, if (done) "Conforme" else "Pendente", if (done) Ui.GREEN else Ui.RED)); addView(r); setOnClickListener { saveAnd(action) } }.margin(5.dp())) }
    private fun serviceDone() = data.company.isNotBlank() && data.place.isNotBlank() && data.responsible.isNotBlank() && data.description.isNotBlank()
    private fun risksDone() = (data.activities.isNotEmpty() || data.manualActivity.isNotBlank()) && RiskEngine.risksFor(data).all { r -> RiskEngine.controls[r].orEmpty().all { !data.controls[RiskEngine.controlKey(r, it)].isNullOrBlank() } }
    private fun checklistDone() = RiskEngine.checklistItems.all { data.checklist[it] == RiskEngine.YES || data.checklist[it] == RiskEngine.NA }
    private fun teamDone() = data.workers.isNotEmpty() && data.signatureB64.isNotBlank()

    private fun showServiceData() = screen("Dados do serviço", ::showPtCentral) {
        addInput("Empresa / Planta", data.company) { data.company = it }; addInput("Área / Setor", data.area) { data.area = it }; addInput("Local da atividade", data.place) { data.place = it }; addInput("Responsável / Emissor", data.responsible) { data.responsible = it }; addInput("Equipe executante", data.teamName) { data.teamName = it }; addInput("Descrição detalhada da atividade", data.description, true) { data.description = it }; addInput("Ferramentas / equipamentos", data.tools, true) { data.tools = it }; addInput("Substâncias / produtos", data.products, true) { data.products = it }; addView(timeCard())
    }

    private fun LinearLayout.addInput(label: String, value: String, multi: Boolean = false, onChange: (String) -> Unit): EditText {
        addView(Ui.label(this@MainActivity, label).margin(4.dp())); val edit = Ui.input(this@MainActivity, label, multi).apply { setText(value); addTextChangedListener(SimpleWatcher { onChange(text.toString()); repo.save(data) }) }; addView(edit.margin(4.dp())); return edit
    }

    private fun timeCard(): View = Ui.card(this).apply {
        addView(Ui.title(this@MainActivity, "Vigência da PT", 18f)); addView(Ui.value(this@MainActivity, "Início: ${Ui.fmt(data.startMillis)}\nValidade: ${data.validityHours}h\nTérmino: ${Ui.fmt(data.endMillis)}"))
        val validities = Ui.row(this@MainActivity); listOf(4, 8, 12, 24).forEach { h -> validities.addView(Ui.ghostButton(this@MainActivity, "${h}h").apply { setOnClickListener { data.validityHours = h; data.endMillis = data.startMillis + h * 60L * 60L * 1000L; repo.save(data); showServiceData() }; layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }) }; addView(validities)
        addView(Ui.button(this@MainActivity, "Agora").apply { setOnClickListener { setNow(); repo.save(data); showServiceData() } }); addView(Ui.ghostButton(this@MainActivity, "Escolher início").apply { setOnClickListener { pickDateTime(data.startMillis) { data.startMillis = it; data.endMillis = it + data.validityHours * 60L * 60L * 1000L; repo.save(data); showServiceData() } } }); addView(Ui.ghostButton(this@MainActivity, "Escolher término").apply { setOnClickListener { pickDateTime(data.endMillis) { data.endMillis = it; repo.save(data); showServiceData() } } })
    }.margin(6.dp())

    private fun pickDateTime(initial: Long, onPicked: (Long) -> Unit) { val cal = Calendar.getInstance().apply { timeInMillis = initial }; DatePickerDialog(this, { _, y, m, d -> TimePickerDialog(this, { _, h, min -> onPicked(Calendar.getInstance().apply { set(y, m, d, h, min, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis) }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show() }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show() }

    private fun showRisks() = screen("Atividades e riscos", ::showPtCentral) {
        addView(Ui.title(this@MainActivity, "Atividades críticas", 18f).margin(4.dp()))
        RiskEngine.activities.forEach { activity -> addView(CheckBox(this@MainActivity).apply { text = activity; setTextColor(Ui.TEXT); isChecked = data.activities.contains(activity); setOnCheckedChangeListener { _, checked -> if (checked) data.activities.add(activity) else data.activities.remove(activity); repo.save(data); showRisks() } }) }
        addInput("Atividade manual / complemento", data.manualActivity, true) { data.manualActivity = it }
        addView(Ui.title(this@MainActivity, "Riscos e controles", 18f).margin(8.dp()))
        val risks = RiskEngine.risksFor(data); if (risks.isEmpty()) addView(Ui.label(this@MainActivity, "Selecione atividades ou descreva a análise manual para gerar riscos."))
        risks.forEach { risk -> addView(Ui.card(this@MainActivity).apply { addView(Ui.title(this@MainActivity, risk, 17f)); RiskEngine.controls[risk].orEmpty().forEach { control -> addView(Ui.label(this@MainActivity, control)); addView(answerGroup(data.controls[RiskEngine.controlKey(risk, control)].orEmpty()) { data.controls[RiskEngine.controlKey(risk, control)] = it; repo.save(data) }) } }.margin(5.dp())) }
    }

    private fun showChecklist() = screen("Checklist de liberação", ::showPtCentral) { RiskEngine.checklistItems.forEach { item -> addView(Ui.card(this@MainActivity).apply { addView(Ui.value(this@MainActivity, item)); addView(answerGroup(data.checklist[item].orEmpty()) { data.checklist[item] = it; repo.save(data) }) }.margin(5.dp())) } }
    private fun answerGroup(current: String, onChange: (String) -> Unit): RadioGroup = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL; listOf(RiskEngine.YES, RiskEngine.NO, RiskEngine.NA).forEach { label -> addView(RadioButton(this@MainActivity).apply { text = label; setTextColor(Ui.TEXT); id = View.generateViewId(); tag = label; isChecked = current == label }) }; setOnCheckedChangeListener { group, checkedId -> (group.findViewById<RadioButton>(checkedId)?.tag as? String)?.let(onChange) } }

    private fun showTeamEvidence() = screen("Equipe e evidências", ::showPtCentral) {
        val workerName = Ui.input(this@MainActivity, "Nome do trabalhador"); val workerRole = Ui.input(this@MainActivity, "Função"); addView(workerName.margin(4.dp())); addView(workerRole.margin(4.dp()))
        addView(Ui.button(this@MainActivity, "Adicionar trabalhador").apply { setOnClickListener { if (workerName.text.isBlank()) Toast.makeText(this@MainActivity, "Informe o nome do trabalhador", Toast.LENGTH_SHORT).show() else { data.workers.add(Worker(workerName.text.toString(), workerRole.text.toString())); repo.save(data); showTeamEvidence() } } }.margin(4.dp()))
        data.workers.forEachIndexed { index, worker -> addView(Ui.card(this@MainActivity).apply { val r = Ui.row(this@MainActivity); r.addView(Ui.value(this@MainActivity, "${worker.name} - ${worker.role}").apply { layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }); r.addView(Ui.ghostButton(this@MainActivity, "Remover").apply { setOnClickListener { data.workers.removeAt(index); repo.save(data); showTeamEvidence() } }); addView(r) }.margin(4.dp())) }
        addView(Ui.card(this@MainActivity).apply { addView(Ui.title(this@MainActivity, "Evidências", 18f)); addView(Ui.value(this@MainActivity, "Fotos anexadas: ${data.photoUris.size}")); addView(Ui.button(this@MainActivity, "Anexar fotos").apply { setOnClickListener { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "image/*"; putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) }, photoRequest) } }) }.margin(5.dp()))
        signatureBlock()
    }

    private fun LinearLayout.signatureBlock() { addView(Ui.card(this@MainActivity).apply { addView(Ui.title(this@MainActivity, "Assinatura do responsável", 18f)); addView(Ui.value(this@MainActivity, "Assinatura do responsável: ${if (data.signatureB64.isBlank()) "pendente" else "salva"}", if (data.signatureB64.isBlank()) Ui.RED else Ui.GREEN)); repo.base64ToBitmap(data.signatureB64)?.let { addView(ImageView(this@MainActivity).apply { setImageBitmap(it); setBackgroundColor(android.graphics.Color.WHITE); adjustViewBounds = true; maxHeight = 140.dp() }) }; addView(Ui.button(this@MainActivity, if (data.signatureB64.isBlank()) "Assinar responsável" else "Reassinar responsável").apply { setOnClickListener { openSignatureDialog() } }) }.margin(5.dp())) }

    private fun openSignatureDialog() { val pad = SignaturePadView(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 260.dp()) }; val box = Ui.vbox(this, 12.dp()).apply { addView(pad) }; AlertDialog.Builder(this).setTitle("Assinatura do responsável").setView(box).setNegativeButton("Cancelar", null).setNeutralButton("Limpar", null).setPositiveButton("Salvar", null).create().apply { setOnShowListener { getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { pad.clearPad() }; getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { if (pad.isEmpty()) Toast.makeText(this@MainActivity, "Assine antes de salvar", Toast.LENGTH_SHORT).show() else { data.signatureB64 = repo.bitmapToBase64(pad.exportBitmap()); repo.save(data); dismiss(); showTeamEvidence() } } } }.show() }

    private fun showReview() = screen("Revisão e emissão", ::showPtCentral) {
        val pending = RiskEngine.pending(data)
        addView(Ui.card(this@MainActivity).apply { addView(Ui.title(this@MainActivity, if (pending.isEmpty()) "PT LIBERADA" else "PT BLOQUEADA", 20f).apply { setTextColor(if (pending.isEmpty()) Ui.GREEN else Ui.RED) }); addView(Ui.value(this@MainActivity, "Empresa: ${data.company.ifBlank { "-" }}")); addView(Ui.value(this@MainActivity, "Local: ${data.place.ifBlank { "-" }}")); addView(Ui.value(this@MainActivity, "Responsável: ${data.responsible.ifBlank { "-" }}")); addView(Ui.value(this@MainActivity, "Início: ${Ui.fmt(data.startMillis)}")); addView(Ui.value(this@MainActivity, "Validade: ${data.validityHours}h")); addView(Ui.value(this@MainActivity, "Término: ${Ui.fmt(data.endMillis)}")); addView(Ui.value(this@MainActivity, "Atividades: ${(data.activities + data.manualActivity).filter { it.isNotBlank() }.joinToString()}")); addView(Ui.value(this@MainActivity, "Quantidade de riscos: ${RiskEngine.risksFor(data).size}")); addView(Ui.value(this@MainActivity, "Checklist: ${RiskEngine.checklistItems.count { data.checklist[it]?.isNotBlank() == true }}/${RiskEngine.checklistItems.size}")); addView(Ui.value(this@MainActivity, "Trabalhadores: ${data.workers.size}")); addView(Ui.value(this@MainActivity, "Fotos: ${data.photoUris.size}")); addView(Ui.value(this@MainActivity, "Assinatura: ${if (data.signatureB64.isBlank()) "pendente" else "salva"}", if (data.signatureB64.isBlank()) Ui.RED else Ui.GREEN)) }.margin(5.dp()))
        if (pending.isNotEmpty()) { addView(Ui.title(this@MainActivity, "Pendências", 18f).margin(6.dp())); pending.forEach { addView(Ui.value(this@MainActivity, "- $it", Ui.RED)) } }
        addView(Ui.button(this@MainActivity, "Gerar e compartilhar PDF", if (pending.isEmpty()) Ui.AMBER else Ui.RED).apply { setOnClickListener { generatePdf() } }.margin(8.dp()))
    }

    private fun generatePdf() {
        val pending = RiskEngine.pending(data)
        if (pending.isNotEmpty()) { Toast.makeText(this, pending.firstOrNull { it == "Assinatura do responsável pendente" } ?: "Existem pendências para emitir a PT", Toast.LENGTH_LONG).show(); showReview(); return }
        val file = PtPdfGenerator(this, repo).generate(data); val stamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date()); data.history.add(0, PtHistoryItem(stamp, data.place, file.name)); while (data.history.size > 10) data.history.removeAt(data.history.lastIndex); repo.save(data)
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file); startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Compartilhar PT SafeField"))
    }

    private fun Int.dp(): Int = Ui.dp(this@MainActivity, this)
}

class SimpleWatcher(private val after: () -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
    override fun afterTextChanged(s: android.text.Editable?) = after()
}
