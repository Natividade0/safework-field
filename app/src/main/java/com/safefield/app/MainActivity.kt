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
        scroll.isFillViewport = true

        val content = Ui.vbox(this, 20.dp())
        scroll.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val header = Ui.row(this)
        header.gravity = Gravity.CENTER_VERTICAL
        if (back != null) {
            val backButton = Ui.ghostButton(this, "‹")
            backButton.textSize = 24f
            backButton.layoutParams = LinearLayout.LayoutParams(48.dp(), 48.dp())
            backButton.setOnClickListener { saveAnd(back) }
            header.addView(backButton)
        }

        val titleBox = Ui.vbox(this)
        val titleView = Ui.title(this, title, if (title == "SafeField") 30f else 25f)
        titleBox.addView(titleView)
        if (title == "SafeField") titleBox.addView(Ui.label(this, "Segurança do Trabalho em Campo"))
        titleBox.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        header.addView(titleBox)

        content.addView(header.margin(0, 4.dp()))
        content.body()
        Ui.animateIn(content)
        setContentView(scroll)
    }

    private fun showHome(): Unit {
        screen("SafeField") {
            val pending = RiskEngine.pending(data)
            addView(homeHero(pending).margin(0, 10.dp()))
            addView(Ui.section(this@MainActivity, "Módulos de campo").margin(0, 10.dp()))
            val modules = listOf("Permissão de Trabalho", "APR", "DDS", "EPI", "Inspeção", "Ocorrência", "Colaboradores", "Dashboard")
            modules.forEachIndexed { index, name ->
                addView(moduleCard(name, index, pending).margin(0, 7.dp()))
            }
        }
    }

    private fun homeHero(pending: List<String>): LinearLayout {
        val hero = Ui.heroCard(this)
        hero.addView(Ui.chip(this, "SAFEFIELD • CAMPO", Ui.AMBER_SOFT))
        hero.addView(Ui.title(this, "Controle inteligente de PT", 24f).margin(0, 8.dp()))
        hero.addView(Ui.label(this, "Rascunho automático, riscos por atividade, checklist técnico, assinatura e PDF nativo."))

        val row = Ui.row(this)
        row.addView(miniStat("Status", if (pending.isEmpty()) "Liberada" else "Bloqueada", if (pending.isEmpty()) Ui.GREEN else Ui.RED), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(miniStat("Pendências", pending.size.toString(), if (pending.isEmpty()) Ui.GREEN else Ui.AMBER), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(miniStat("Fotos", data.photoUris.size.toString(), Ui.AMBER), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        hero.addView(row.margin(0, 12.dp()))

        val open = Ui.button(this, "Abrir Permissão de Trabalho")
        open.setOnClickListener { showPtCentral() }
        hero.addView(open.margin(0, 8.dp()))
        return hero
    }

    private fun miniStat(label: String, value: String, color: Int): LinearLayout {
        val box = Ui.vbox(this, 10.dp())
        box.background = Ui.bg(Ui.PANEL, 14.dp(), Ui.BORDER, 1)
        val valueView = Ui.title(this, value, 18f)
        valueView.setTextColor(color)
        box.addView(valueView)
        box.addView(Ui.label(this, label))
        return box
    }

    private fun moduleCard(name: String, index: Int, pending: List<String>): LinearLayout {
        val primary = name == "Permissão de Trabalho"
        val card = if (primary) Ui.heroCard(this) else Ui.card(this)
        val row = Ui.row(this)

        val icon = TextView(this)
        icon.text = moduleIcon(name)
        icon.textSize = 28f
        icon.gravity = Gravity.CENTER
        icon.background = Ui.bg(if (primary) 0x33F59E0B else Ui.PANEL, 16.dp(), if (primary) Ui.AMBER else Ui.BORDER, 1)
        row.addView(icon, LinearLayout.LayoutParams(56.dp(), 56.dp()))

        val texts = Ui.vbox(this)
        texts.setPadding(14.dp(), 0, 0, 0)
        texts.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        texts.addView(Ui.title(this, name, if (primary) 20f else 18f))
        texts.addView(Ui.label(this, moduleSubtitle(name, pending)))
        row.addView(texts)

        val chipText = if (primary) {
            if (pending.isEmpty()) "OK" else "${pending.size}"
        } else {
            "EM BREVE"
        }
        val chipColor = if (primary && pending.isEmpty()) Ui.GREEN else if (primary) Ui.RED else Ui.MUTED
        row.addView(Ui.chip(this, chipText, chipColor))

        card.addView(row)
        if (primary) {
            card.addView(Ui.progress(this, ptProgress(), 5).margin(0, 12.dp()))
        }
        card.setOnClickListener {
            if (primary) showPtCentral() else placeholder(name)
        }
        card.translationX = if (index % 2 == 0) -10f else 10f
        card.animate().translationX(0f).setDuration(180L + index * 25L).start()
        return card
    }

    private fun moduleIcon(name: String): String {
        return when (name) {
            "Permissão de Trabalho" -> "PT"
            "APR" -> "⚠"
            "DDS" -> "☰"
            "EPI" -> "◈"
            "Inspeção" -> "✓"
            "Ocorrência" -> "!"
            "Colaboradores" -> "👥"
            else -> "▣"
        }
    }

    private fun moduleSubtitle(name: String, pending: List<String>): String {
        return when (name) {
            "Permissão de Trabalho" -> if (pending.isEmpty()) "PT pronta para revisão e emissão" else "${pending.size} pendência(s) antes da emissão"
            "APR" -> "Análise preliminar de risco"
            "DDS" -> "Diálogo diário de segurança"
            "EPI" -> "Controle de entrega e inspeção"
            "Inspeção" -> "Checklists e evidências de campo"
            "Ocorrência" -> "Registro rápido de desvios"
            "Colaboradores" -> "Equipe, funções e histórico"
            else -> "Indicadores de segurança"
        }
    }

    private fun placeholder(name: String): Unit {
        screen(name, ::showHome) {
            val card = Ui.heroCard(this@MainActivity)
            card.addView(Ui.chip(this@MainActivity, "MÓDULO EM EXPANSÃO", Ui.AMBER))
            card.addView(Ui.title(this@MainActivity, name, 24f).margin(0, 8.dp()))
            card.addView(Ui.label(this@MainActivity, "A base visual e técnica já está pronta para evoluir este módulo sem alterar o núcleo da PT."))
            addView(card.margin(0, 10.dp()))
        }
    }

    private fun showPtCentral(): Unit {
        screen("Central da PT", ::showHome) {
            val pending = RiskEngine.pending(data)
            addView(statusPanel(pending).margin(0, 8.dp()))
            addView(Ui.section(this@MainActivity, "Progresso da permissão").margin(0, 10.dp()))
            addView(Ui.progress(this@MainActivity, ptProgress(), 5).margin(0, 6.dp()))
            addView(stepCard("01", "Dados do serviço", "Identificação, local, responsável e vigência", serviceDone(), ::showServiceData).margin(0, 6.dp()))
            addView(stepCard("02", "Atividades e riscos", "Riscos automáticos e controles obrigatórios", risksDone(), ::showRisks).margin(0, 6.dp()))
            addView(stepCard("03", "Checklist de liberação", "Itens críticos com Sim, Não ou N/A", checklistDone(), ::showChecklist).margin(0, 6.dp()))
            addView(stepCard("04", "Equipe e evidências", "Trabalhadores, fotos e assinatura", teamDone(), ::showTeamEvidence).margin(0, 6.dp()))
            addView(stepCard("05", "Revisão e emissão", "Resumo final, validação e PDF", pending.isEmpty(), ::showReview).margin(0, 6.dp()))

            val reviewButton = Ui.button(this@MainActivity, "Revisar e gerar PDF", if (pending.isEmpty()) Ui.AMBER else Ui.RED)
            reviewButton.setOnClickListener { showReview() }
            addView(reviewButton.margin(0, 12.dp()))

            val clearButton = Ui.ghostButton(this@MainActivity, "Limpar rascunho")
            clearButton.setOnClickListener { confirmClearDraft() }
            addView(clearButton.margin(0, 4.dp()))

            addView(Ui.section(this@MainActivity, "Últimas emissões").margin(0, 14.dp()))
            if (data.history.isEmpty()) {
                val empty = Ui.card(this@MainActivity)
                empty.addView(Ui.value(this@MainActivity, "Nenhuma PT emitida neste aparelho."))
                empty.addView(Ui.label(this@MainActivity, "Quando gerar um PDF, o histórico aparecerá aqui."))
                addView(empty.margin(0, 5.dp()))
            }
            data.history.take(5).forEach { item ->
                val historyCard = Ui.card(this@MainActivity)
                historyCard.addView(Ui.chip(this@MainActivity, item.emittedAt, Ui.AMBER))
                historyCard.addView(Ui.value(this@MainActivity, item.place.ifBlank { "Sem local informado" }))
                historyCard.addView(Ui.label(this@MainActivity, item.fileName))
                addView(historyCard.margin(0, 5.dp()))
            }
        }
    }

    private fun ptProgress(): Int {
        var score = 0
        if (serviceDone()) score++
        if (risksDone()) score++
        if (checklistDone()) score++
        if (teamDone()) score++
        if (RiskEngine.pending(data).isEmpty()) score++
        return score
    }

    private fun statusPanel(pending: List<String>): LinearLayout {
        val card = Ui.heroCard(this)
        val released = pending.isEmpty()
        card.addView(Ui.chip(this, if (released) "PRONTO PARA EMISSÃO" else "AÇÃO NECESSÁRIA", if (released) Ui.GREEN else Ui.RED))
        val title = Ui.title(this, if (released) "PT LIBERADA" else "PT BLOQUEADA", 26f)
        title.setTextColor(if (released) Ui.GREEN else Ui.RED)
        card.addView(title.margin(0, 8.dp()))
        val subtitle = if (released) "Todas as etapas estão conformes. Gere e compartilhe o PDF." else "${pending.size} pendência(s) impedem a emissão da PT."
        card.addView(Ui.label(this, subtitle))
        if (pending.isNotEmpty()) {
            card.addView(Ui.divider(this).margin(0, 12.dp()))
            pending.take(4).forEach {
                card.addView(Ui.value(this, "• $it", Ui.RED).margin(0, 3.dp()))
            }
        }
        return card
    }

    private fun stepCard(number: String, titleText: String, subtitle: String, done: Boolean, action: () -> Unit): LinearLayout {
        val card = Ui.card(this)
        val row = Ui.row(this)

        val badge = TextView(this)
        badge.text = number
        badge.gravity = Gravity.CENTER
        badge.textSize = 15f
        badge.setTextColor(if (done) Ui.GREEN else Ui.AMBER)
        badge.background = Ui.bg(if (done) 0x2222C55E else 0x22F59E0B, 16.dp(), if (done) Ui.GREEN else Ui.AMBER, 1)
        row.addView(badge, LinearLayout.LayoutParams(48.dp(), 48.dp()))

        val texts = Ui.vbox(this)
        texts.setPadding(14.dp(), 0, 0, 0)
        texts.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        texts.addView(Ui.title(this, titleText, 17f))
        texts.addView(Ui.label(this, subtitle))
        row.addView(texts)

        row.addView(Ui.chip(this, if (done) "CONFORME" else "PENDENTE", if (done) Ui.GREEN else Ui.RED))
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
            addView(Ui.section(this@MainActivity, "Identificação da atividade").margin(0, 8.dp()))
            addInput("Empresa / Planta", data.company) { data.company = it }
            addInput("Área / Setor", data.area) { data.area = it }
            addInput("Local da atividade", data.place) { data.place = it }
            addInput("Responsável / Emissor", data.responsible) { data.responsible = it }
            addInput("Equipe executante", data.teamName) { data.teamName = it }
            addInput("Descrição detalhada da atividade", data.description, true) { data.description = it }
            addInput("Ferramentas / equipamentos", data.tools, true) { data.tools = it }
            addInput("Substâncias / produtos", data.products, true) { data.products = it }
            addView(timeCard().margin(0, 10.dp()))
        }
    }

    private fun LinearLayout.addInput(label: String, value: String, multi: Boolean = false, onChange: (String) -> Unit): EditText {
        addView(Ui.label(this@MainActivity, label).margin(0, 5.dp()))
        val edit = Ui.input(this@MainActivity, label, multi)
        edit.setText(value)
        edit.addTextChangedListener(SimpleWatcher {
            onChange(edit.text.toString())
            repo.save(data)
        })
        addView(edit.margin(0, 4.dp()))
        return edit
    }

    private fun timeCard(): LinearLayout {
        val card = Ui.heroCard(this)
        card.addView(Ui.chip(this, "DATA E VALIDADE", Ui.AMBER))
        card.addView(Ui.title(this, "Vigência inteligente", 18f).margin(0, 8.dp()))
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
        card.addView(validities.margin(0, 8.dp()))

        val now = Ui.button(this, "Usar horário atual")
        now.setOnClickListener {
            setNow()
            repo.save(data)
            showServiceData()
        }
        card.addView(now.margin(0, 6.dp()))

        val start = Ui.ghostButton(this, "Escolher início")
        start.setOnClickListener {
            pickDateTime(data.startMillis) {
                data.startMillis = it
                data.endMillis = it + data.validityHours * 60L * 60L * 1000L
                repo.save(data)
                showServiceData()
            }
        }
        card.addView(start.margin(0, 4.dp()))

        val end = Ui.ghostButton(this, "Editar término")
        end.setOnClickListener {
            pickDateTime(data.endMillis) {
                data.endMillis = it
                repo.save(data)
                showServiceData()
            }
        }
        card.addView(end.margin(0, 4.dp()))
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
            addView(Ui.section(this@MainActivity, "Atividades críticas").margin(0, 8.dp()))
            RiskEngine.activities.forEach { activity ->
                addView(activityRow(activity).margin(0, 4.dp()))
            }
            addInput("Atividade manual / complemento", data.manualActivity, true) { data.manualActivity = it }
            addView(Ui.section(this@MainActivity, "Riscos e controles gerados").margin(0, 12.dp()))

            val risks = RiskEngine.risksFor(data)
            if (risks.isEmpty()) {
                val empty = Ui.card(this@MainActivity)
                empty.addView(Ui.value(this@MainActivity, "Nenhum risco gerado."))
                empty.addView(Ui.label(this@MainActivity, "Selecione uma atividade ou descreva a atividade manual para gerar controles."))
                addView(empty.margin(0, 5.dp()))
            }
            risks.forEach { risk ->
                addView(riskCard(risk).margin(0, 6.dp()))
            }
        }
    }

    private fun activityRow(activity: String): CheckBox {
        val check = CheckBox(this)
        check.text = activity
        check.setTextColor(Ui.TEXT)
        check.textSize = 15f
        check.background = Ui.bg(Ui.CARD, 16.dp(), Ui.BORDER, 1)
        check.setPadding(14.dp(), 10.dp(), 14.dp(), 10.dp())
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
        card.addView(Ui.chip(this, "RISCO IDENTIFICADO", Ui.AMBER))
        card.addView(Ui.title(this, risk, 18f).margin(0, 8.dp()))
        RiskEngine.controls[risk].orEmpty().forEach { control ->
            card.addView(Ui.label(this, control).margin(0, 5.dp()))
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
            addView(Ui.section(this@MainActivity, "Pré-requisitos obrigatórios").margin(0, 8.dp()))
            RiskEngine.checklistItems.forEach { item ->
                addView(checklistCard(item).margin(0, 6.dp()))
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
            addView(Ui.section(this@MainActivity, "Equipe executante").margin(0, 8.dp()))
            val workerName = Ui.input(this@MainActivity, "Nome do trabalhador")
            val workerRole = Ui.input(this@MainActivity, "Função")
            addView(workerName.margin(0, 4.dp()))
            addView(workerRole.margin(0, 4.dp()))

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
            addView(addWorker.margin(0, 8.dp()))

            data.workers.forEachIndexed { index, worker ->
                addView(workerCard(index, worker).margin(0, 5.dp()))
            }
            addView(evidenceCard().margin(0, 8.dp()))
            addView(signatureCard().margin(0, 8.dp()))
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
        card.addView(Ui.chip(this, "EVIDÊNCIAS", Ui.AMBER))
        card.addView(Ui.title(this, "Fotos do local", 18f).margin(0, 8.dp()))
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
        card.addView(button.margin(0, 8.dp()))
        return card
    }

    private fun signatureCard(): LinearLayout {
        val card = Ui.heroCard(this)
        card.addView(Ui.chip(this, "ASSINATURA", if (data.signatureB64.isNotBlank()) Ui.GREEN else Ui.RED))
        card.addView(Ui.title(this, "Assinatura do responsável", 18f).margin(0, 8.dp()))
        val saved = data.signatureB64.isNotBlank()
        card.addView(Ui.value(this, "Status: ${if (saved) "salva" else "pendente"}", if (saved) Ui.GREEN else Ui.RED))
        repo.base64ToBitmap(data.signatureB64)?.let {
            val preview = ImageView(this)
            preview.setImageBitmap(it)
            preview.setBackgroundColor(android.graphics.Color.WHITE)
            preview.adjustViewBounds = true
            preview.maxHeight = 140.dp()
            card.addView(preview.margin(0, 8.dp()))
        }
        val button = Ui.button(this, if (saved) "Reassinar responsável" else "Assinar responsável")
        button.setOnClickListener { openSignatureDialog() }
        card.addView(button.margin(0, 8.dp()))
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
            addView(reviewPanel(pending).margin(0, 6.dp()))
            if (pending.isNotEmpty()) {
                addView(Ui.section(this@MainActivity, "Pendências").margin(0, 10.dp()))
                pending.forEach { addView(Ui.value(this@MainActivity, "• $it", Ui.RED)) }
            }
            val button = Ui.button(this@MainActivity, "Gerar e compartilhar PDF", if (pending.isEmpty()) Ui.AMBER else Ui.RED)
            button.setOnClickListener { generatePdf() }
            addView(button.margin(0, 12.dp()))
        }
    }

    private fun reviewPanel(pending: List<String>): LinearLayout {
        val card = Ui.heroCard(this)
        val released = pending.isEmpty()
        card.addView(Ui.chip(this, if (released) "EMISSÃO LIBERADA" else "EMISSÃO BLOQUEADA", if (released) Ui.GREEN else Ui.RED))
        val status = Ui.title(this, if (released) "PT LIBERADA" else "PT BLOQUEADA", 22f)
        status.setTextColor(if (released) Ui.GREEN else Ui.RED)
        card.addView(status.margin(0, 8.dp()))
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
