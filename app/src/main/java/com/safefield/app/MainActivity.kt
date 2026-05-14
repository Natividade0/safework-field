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
import android.widget.Toast
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var repo: PtRepository
    private lateinit var data: PtData
    private val photoRequest: Int = 7001

    override fun onCreate(savedInstanceState: Bundle?): Unit {
        super.onCreate(savedInstanceState)
        repo = PtRepository(this)
        data = repo.load()
        if (data.startMillis <= 0L) setNow()
        showHome()
    }

    override fun onPause(): Unit {
        super.onPause()
        repo.save(data)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?): Unit {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == photoRequest && resultCode == RESULT_OK) {
            val clip = resultData?.clipData
            if (clip != null) {
                for (i in 0 until clip.itemCount) addPhoto(clip.getItemAt(i).uri)
            } else {
                resultData?.data?.let { addPhoto(it) }
            }
            repo.save(data)
            showTeamEvidence()
        }
    }

    private fun addPhoto(uri: Uri): Unit {
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        data.photoUris.add(uri.toString())
    }

    private fun setNow(): Unit {
        data.startMillis = System.currentTimeMillis()
        data.endMillis = data.startMillis + data.validityHours * 60L * 60L * 1000L
    }

    private fun saveAnd(block: () -> Unit): Unit {
        repo.save(data)
        block()
    }

    private fun screen(title: String, back: (() -> Unit)? = null, body: LinearLayout.() -> Unit): Unit {
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Ui.SHELL)

        val content = Ui.vbox(this, 16.dp())
        scroll.addView(content)

        val header = Ui.row(this)
        if (back != null) {
            val backButton = Ui.ghostButton(this, "Voltar")
            backButton.layoutParams = LinearLayout.LayoutParams(96.dp(), ViewGroup.LayoutParams.WRAP_CONTENT)
            backButton.setOnClickListener { saveAnd(back) }
            header.addView(backButton)
        }

        val titleView = Ui.title(this, title)
        titleView.gravity = Gravity.CENTER_VERTICAL
        titleView.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        header.addView(titleView)

        content.addView(header)
        content.body()
        setContentView(scroll)
    }

    private fun showHome(): Unit {
        screen("SafeField") {
            addView(Ui.label(this@MainActivity, "Segurança do Trabalho em Campo"))
            val modules = listOf("Permissão de Trabalho", "APR", "DDS", "EPI", "Inspeção", "Ocorrência", "Colaboradores", "Dashboard")
            modules.forEach { name ->
                addView(moduleCard(name).margin(6.dp()))
            }
        }
    }

    private fun moduleCard(name: String): LinearLayout {
        val card = Ui.card(this)
        card.addView(Ui.title(this, name, 18f))
        val subtitle = if (name == "Permissão de Trabalho") "Central completa para emissão de PT" else "Módulo preparado para expansão"
        card.addView(Ui.label(this, subtitle))
        card.setOnClickListener {
            if (name == "Permissão de Trabalho") showPtCentral() else placeholder(name)
        }
        return card
    }

    private fun placeholder(name: String): Unit {
        screen(name, ::showHome) {
            val card = Ui.card(this@MainActivity)
            card.addView(Ui.title(this@MainActivity, "Módulo preparado para expansão", 18f))
            card.addView(Ui.label(this@MainActivity, "Base pronta para evoluir este módulo."))
            addView(card.margin(6.dp()))
        }
    }

    private fun showPtCentral(): Unit {
        screen("Central da PT", ::showHome) {
            val pending = RiskEngine.pending(data)
            addView(statusPanel(pending).margin(6.dp()))
            addView(stepCard("1. Dados do serviço", serviceDone(), ::showServiceData).margin(5.dp()))
            addView(stepCard("2. Atividades e riscos", risksDone(), ::showRisks).margin(5.dp()))
            addView(stepCard("3. Checklist de liberação", checklistDone(), ::showChecklist).margin(5.dp()))
            addView(stepCard("4. Equipe e evidências", teamDone(), ::showTeamEvidence).margin(5.dp()))
            addView(stepCard("5. Revisão e emissão", pending.isEmpty(), ::showReview).margin(5.dp()))

            val reviewButton = Ui.button(this@MainActivity, "Revisar e gerar PDF")
            reviewButton.setOnClickListener { showReview() }
            addView(reviewButton.margin(6.dp()))

            val clearButton = Ui.ghostButton(this@MainActivity, "Limpar rascunho")
            clearButton.setOnClickListener { confirmClearDraft() }
            addView(clearButton.margin(6.dp()))

            addView(Ui.title(this@MainActivity, "Últimas PTs emitidas", 18f).margin(8.dp()))
            if (data.history.isEmpty()) {
                addView(Ui.label(this@MainActivity, "Nenhuma PT emitida neste aparelho."))
            }
            data.history.take(5).forEach { item ->
                val historyCard = Ui.card(this@MainActivity)
                historyCard.addView(Ui.value(this@MainActivity, item.emittedAt))
                historyCard.addView(Ui.label(this@MainActivity, "${item.place} - ${item.fileName}"))
                addView(historyCard.margin(4.dp()))
            }
        }
    }

    private fun statusPanel(pending: List<String>): LinearLayout {
        val card = Ui.card(this)
        val released = pending.isEmpty()
        val title = Ui.title(this, if (released) "PT LIBERADA" else "PT BLOQUEADA", 20f)
        title.setTextColor(if (released) Ui.GREEN else Ui.RED)
        card.addView(title)
        val subtitle = if (released) "Todas as etapas estão conformes." else "${pending.size} pendência(s) impedem a emissão."
        card.addView(Ui.label(this, subtitle))
        pending.take(6).forEach {
            card.addView(Ui.value(this, "- $it", Ui.RED))
        }
        return card
    }

    private fun stepCard(text: String, done: Boolean, action: () -> Unit): LinearLayout {
        val card = Ui.card(this)
        val row = Ui.row(this)
        val title = Ui.title(this, text, 17f)
        title.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(title)
        row.addView(Ui.value(this, if (done) "Conforme" else "Pendente", if (done) Ui.GREEN else Ui.RED))
        card.addView(row)
        card.setOnClickListener { saveAnd(action) }
        return card
    }

    private fun confirmClearDraft(): Unit {
        AlertDialog.Builder(this)
            .setTitle("Limpar rascunho?")
            .setMessage("Todos os dados, fotos e assinatura salvos serão removidos.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Limpar") { _, _ ->
                data = PtData()
                setNow()
                repo.clear()
                showPtCentral()
            }
            .show()
    }

    private fun serviceDone(): Boolean {
        return data.company.isNotBlank() && data.place.isNotBlank() && data.responsible.isNotBlank() && data.description.isNotBlank()
    }

    private fun risksDone(): Boolean {
        val hasActivity = data.activities.isNotEmpty() || data.manualActivity.isNotBlank()
        val allControlsAnswered = RiskEngine.risksFor(data).all { risk ->
            RiskEngine.controls[risk].orEmpty().all { control ->
                !data.controls[RiskEngine.controlKey(risk, control)].isNullOrBlank()
            }
        }
        return hasActivity && allControlsAnswered
    }

    private fun checklistDone(): Boolean {
        return RiskEngine.checklistItems.all { data.checklist[it] == RiskEngine.YES || data.checklist[it] == RiskEngine.NA }
    }

    private fun teamDone(): Boolean {
        return data.workers.isNotEmpty() && data.signatureB64.isNotBlank()
    }

    private fun showServiceData(): Unit {
        screen("Dados do serviço", ::showPtCentral) {
            addInput("Empresa / Planta", data.company) { data.company = it }
            addInput("Área / Setor", data.area) { data.area = it }
            addInput("Local da atividade", data.place) { data.place = it }
            addInput("Responsável / Emissor", data.responsible) { data.responsible = it }
            addInput("Equipe executante", data.teamName) { data.teamName = it }
            addInput("Descrição detalhada da atividade", data.description, true) { data.description = it }
            addInput("Ferramentas / equipamentos", data.tools, true) { data.tools = it }
            addInput("Substâncias / produtos", data.products, true) { data.products = it }
            addView(timeCard().margin(6.dp()))
        }
    }

    private fun LinearLayout.addInput(label: String, value: String, multi: Boolean = false, onChange: (String) -> Unit): EditText {
        addView(Ui.label(this@MainActivity, label).margin(4.dp()))
        val edit = Ui.input(this@MainActivity, label, multi)
        edit.setText(value)
        edit.addTextChangedListener(SimpleWatcher {
            onChange(edit.text.toString())
            repo.save(data)
        })
        addView(edit.margin(4.dp()))
        return edit
    }

    private fun timeCard(): LinearLayout {
        val card = Ui.card(this)
        card.addView(Ui.title(this, "Vigência da PT", 18f))
        card.addView(Ui.value(this, "Início: ${Ui.fmt(data.startMillis)}\nValidade: ${data.validityHours}h\nTérmino: ${Ui.fmt(data.endMillis)}"))

        val validities = Ui.row(this)
        listOf(4, 8, 12, 24).forEach { hours ->
            val button = Ui.ghostButton(this, "${hours}h")
            button.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            button.setOnClickListener {
                data.validityHours = hours
                data.endMillis = data.startMillis + hours * 60L * 60L * 1000L
                repo.save(data)
                showServiceData()
            }
            validities.addView(button)
        }
        card.addView(validities)

        val now = Ui.button(this, "Agora")
        now.setOnClickListener {
            setNow()
            repo.save(data)
            showServiceData()
        }
        card.addView(now)

        val start = Ui.ghostButton(this, "Escolher início")
        start.setOnClickListener {
            pickDateTime(data.startMillis) {
                data.startMillis = it
                data.endMillis = it + data.validityHours * 60L * 60L * 1000L
                repo.save(data)
                showServiceData()
            }
        }
        card.addView(start)

        val end = Ui.ghostButton(this, "Escolher término")
        end.setOnClickListener {
            pickDateTime(data.endMillis) {
                data.endMillis = it
                repo.save(data)
                showServiceData()
            }
        }
        card.addView(end)
        return card
    }

    private fun pickDateTime(initial: Long, onPicked: (Long) -> Unit): Unit {
        val cal = Calendar.getInstance()
        cal.timeInMillis = initial
        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                val picked = Calendar.getInstance()
                picked.set(year, month, day, hour, minute, 0)
                picked.set(Calendar.MILLISECOND, 0)
                onPicked(picked.timeInMillis)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showRisks(): Unit {
        screen("Atividades e riscos", ::showPtCentral) {
            addView(Ui.title(this@MainActivity, "Atividades críticas", 18f).margin(4.dp()))
            RiskEngine.activities.forEach { activity ->
                addView(activityRow(activity))
            }
            addInput("Atividade manual / complemento", data.manualActivity, true) { data.manualActivity = it }
            addView(Ui.title(this@MainActivity, "Riscos e controles", 18f).margin(8.dp()))

            val risks = RiskEngine.risksFor(data)
            if (risks.isEmpty()) {
                addView(Ui.label(this@MainActivity, "Selecione atividades ou descreva a análise manual para gerar riscos."))
            }
            risks.forEach { risk ->
                addView(riskCard(risk).margin(5.dp()))
            }
        }
    }

    private fun activityRow(activity: String): CheckBox {
        val check = CheckBox(this)
        check.text = activity
        check.setTextColor(Ui.TEXT)
        check.isChecked = data.activities.contains(activity)
        check.setOnCheckedChangeListener { _, checked ->
            if (checked) data.activities.add(activity) else data.activities.remove(activity)
            repo.save(data)
            showRisks()
        }
        return check
    }

    private fun riskCard(risk: String): LinearLayout {
        val card = Ui.card(this)
        card.addView(Ui.title(this, risk, 17f))
        RiskEngine.controls[risk].orEmpty().forEach { control ->
            card.addView(Ui.label(this, control))
            val key = RiskEngine.controlKey(risk, control)
            val group = answerGroup(data.controls[key].orEmpty()) {
                data.controls[key] = it
                repo.save(data)
            }
            card.addView(group)
        }
        return card
    }

    private fun showChecklist(): Unit {
        screen("Checklist de liberação", ::showPtCentral) {
            RiskEngine.checklistItems.forEach { item ->
                addView(checklistCard(item).margin(5.dp()))
            }
        }
    }

    private fun checklistCard(item: String): LinearLayout {
        val card = Ui.card(this)
        card.addView(Ui.value(this, item))
        val group = answerGroup(data.checklist[item].orEmpty()) {
            data.checklist[item] = it
            repo.save(data)
        }
        card.addView(group)
        return card
    }

    private fun answerGroup(current: String, onChange: (String) -> Unit): RadioGroup {
        val group = RadioGroup(this)
        group.orientation = RadioGroup.HORIZONTAL
        listOf(RiskEngine.YES, RiskEngine.NO, RiskEngine.NA).forEach { label ->
            val option = RadioButton(this)
            option.text = label
            option.setTextColor(Ui.TEXT)
            option.id = View.generateViewId()
            option.tag = label
            option.isChecked = current == label
            group.addView(option)
        }
        group.setOnCheckedChangeListener { radioGroup, checkedId ->
            val selected = radioGroup.findViewById<RadioButton>(checkedId)?.tag as? String
            if (selected != null) onChange(selected)
        }
        return group
    }

    private fun showTeamEvidence(): Unit {
        screen("Equipe e evidências", ::showPtCentral) {
            val workerName = Ui.input(this@MainActivity, "Nome do trabalhador")
            val workerRole = Ui.input(this@MainActivity, "Função")
            addView(workerName.margin(4.dp()))
            addView(workerRole.margin(4.dp()))

            val addWorker = Ui.button(this@MainActivity, "Adicionar trabalhador")
            addWorker.setOnClickListener {
                if (workerName.text.isBlank()) {
                    Toast.makeText(this@MainActivity, "Informe o nome do trabalhador", Toast.LENGTH_SHORT).show()
                } else {
                    data.workers.add(Worker(workerName.text.toString(), workerRole.text.toString()))
                    repo.save(data)
                    showTeamEvidence()
                }
            }
            addView(addWorker.margin(4.dp()))

            data.workers.forEachIndexed { index, worker ->
                addView(workerCard(index, worker).margin(4.dp()))
            }
            addView(evidenceCard().margin(5.dp()))
            addView(signatureCard().margin(5.dp()))
        }
    }

    private fun workerCard(index: Int, worker: Worker): LinearLayout {
        val card = Ui.card(this)
        val row = Ui.row(this)
        val info = Ui.value(this, "${worker.name} - ${worker.role}")
        info.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(info)
        val remove = Ui.ghostButton(this, "Remover")
        remove.setOnClickListener {
            data.workers.removeAt(index)
            repo.save(data)
            showTeamEvidence()
        }
        row.addView(remove)
        card.addView(row)
        return card
    }

    private fun evidenceCard(): LinearLayout {
        val card = Ui.card(this)
        card.addView(Ui.title(this, "Evidências", 18f))
        card.addView(Ui.value(this, "Fotos anexadas: ${data.photoUris.size}"))
        val button = Ui.button(this, "Anexar fotos")
        button.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            startActivityForResult(intent, photoRequest)
        }
        card.addView(button)
        return card
    }

    private fun signatureCard(): LinearLayout {
        val card = Ui.card(this)
        card.addView(Ui.title(this, "Assinatura do responsável", 18f))
        val saved = data.signatureB64.isNotBlank()
        card.addView(Ui.value(this, "Assinatura do responsável: ${if (saved) "salva" else "pendente"}", if (saved) Ui.GREEN else Ui.RED))
        repo.base64ToBitmap(data.signatureB64)?.let {
            val preview = ImageView(this)
            preview.setImageBitmap(it)
            preview.setBackgroundColor(android.graphics.Color.WHITE)
            preview.adjustViewBounds = true
            preview.maxHeight = 140.dp()
            card.addView(preview)
        }
        val button = Ui.button(this, if (saved) "Reassinar responsável" else "Assinar responsável")
        button.setOnClickListener { openSignatureDialog() }
        card.addView(button)
        return card
    }

    private fun openSignatureDialog(): Unit {
        val pad = SignaturePadView(this)
        pad.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 260.dp())
        val box = Ui.vbox(this, 12.dp())
        box.addView(pad)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Assinatura do responsável")
            .setView(box)
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Limpar", null)
            .setPositiveButton("Salvar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { pad.clearPad() }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (pad.isEmpty()) {
                    Toast.makeText(this, "Assine antes de salvar", Toast.LENGTH_SHORT).show()
                } else {
                    data.signatureB64 = repo.bitmapToBase64(pad.exportBitmap())
                    repo.save(data)
                    dialog.dismiss()
                    showTeamEvidence()
                }
            }
        }
        dialog.show()
    }

    private fun showReview(): Unit {
        screen("Revisão e emissão", ::showPtCentral) {
            val pending = RiskEngine.pending(data)
            addView(reviewPanel(pending).margin(5.dp()))
            if (pending.isNotEmpty()) {
                addView(Ui.title(this@MainActivity, "Pendências", 18f).margin(6.dp()))
                pending.forEach { addView(Ui.value(this@MainActivity, "- $it", Ui.RED)) }
            }
            val button = Ui.button(this@MainActivity, "Gerar e compartilhar PDF", if (pending.isEmpty()) Ui.AMBER else Ui.RED)
            button.setOnClickListener { generatePdf() }
            addView(button.margin(8.dp()))
        }
    }

    private fun reviewPanel(pending: List<String>): LinearLayout {
        val card = Ui.card(this)
        val released = pending.isEmpty()
        val status = Ui.title(this, if (released) "PT LIBERADA" else "PT BLOQUEADA", 20f)
        status.setTextColor(if (released) Ui.GREEN else Ui.RED)
        card.addView(status)
        card.addView(Ui.value(this, "Empresa: ${data.company.ifBlank { "-" }}"))
        card.addView(Ui.value(this, "Local: ${data.place.ifBlank { "-" }}"))
        card.addView(Ui.value(this, "Responsável: ${data.responsible.ifBlank { "-" }}"))
        card.addView(Ui.value(this, "Início: ${Ui.fmt(data.startMillis)}"))
        card.addView(Ui.value(this, "Validade: ${data.validityHours}h"))
        card.addView(Ui.value(this, "Término: ${Ui.fmt(data.endMillis)}"))
        card.addView(Ui.value(this, "Atividades: ${activitySummary()}"))
        card.addView(Ui.value(this, "Quantidade de riscos: ${RiskEngine.risksFor(data).size}"))
        val answeredChecklist = RiskEngine.checklistItems.count { data.checklist[it]?.isNotBlank() == true }
        card.addView(Ui.value(this, "Checklist: $answeredChecklist/${RiskEngine.checklistItems.size}"))
        card.addView(Ui.value(this, "Trabalhadores: ${data.workers.size}"))
        card.addView(Ui.value(this, "Fotos: ${data.photoUris.size}"))
        val signatureSaved = data.signatureB64.isNotBlank()
        card.addView(Ui.value(this, "Assinatura: ${if (signatureSaved) "salva" else "pendente"}", if (signatureSaved) Ui.GREEN else Ui.RED))
        return card
    }

    private fun activitySummary(): String {
        val values = data.activities.toMutableList()
        if (data.manualActivity.isNotBlank()) values.add(data.manualActivity)
        return values.joinToString().ifBlank { "-" }
    }

    private fun generatePdf(): Unit {
        val pending = RiskEngine.pending(data)
        if (pending.isNotEmpty()) {
            val message = pending.firstOrNull { it == "Assinatura do responsável pendente" } ?: "Existem pendências para emitir a PT"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            showReview()
            return
        }

        val file = PtPdfGenerator(this, repo).generate(data)
        val stamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
        data.history.add(0, PtHistoryItem(stamp, data.place, file.name))
        while (data.history.size > 10) data.history.removeAt(data.history.lastIndex)
        repo.save(data)

        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val share = Intent(Intent.ACTION_SEND)
        share.type = "application/pdf"
        share.putExtra(Intent.EXTRA_STREAM, uri)
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(share, "Compartilhar PT SafeField"))
    }

    private fun Int.dp(): Int = Ui.dp(this@MainActivity, this)
}

class SimpleWatcher(private val after: () -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int): Unit = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int): Unit = Unit
    override fun afterTextChanged(s: android.text.Editable?): Unit = after()
}
