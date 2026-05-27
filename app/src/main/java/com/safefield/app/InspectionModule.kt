package com.safefield.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InspectionModule(
    private val activity: Activity,
    private val onBackHome: () -> Unit
) {
    private val repo = InspectionRepository(activity)
    private var data: InspectionData = repo.load()
    private val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

    fun show(): Unit = showCentral()

    private fun screen(title: String, subtitle: String, back: () -> Unit = ::showCentral, build: (LinearLayout) -> Unit): Unit {
        val shell = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Ui.SHELL) }
        val header = Ui.row(activity).apply {
            setPadding(dp(16), dp(12), dp(16), dp(10))
            background = Ui.bg(Ui.DARK, 0, Ui.BORDER, 1)
        }
        header.addView(Ui.ghostButton(activity, "Voltar").apply { setOnClickListener { repo.save(data); back() } }, LinearLayout.LayoutParams(dp(98), ViewGroup.LayoutParams.WRAP_CONTENT))
        val texts = Ui.vbox(activity)
        texts.addView(Ui.title(activity, title, 20f))
        texts.addView(Ui.label(activity, subtitle))
        header.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(12), 0, 0, 0) })
        shell.addView(header)

        val scroll = ScrollView(activity)
        val root = Ui.vbox(activity, dp(14))
        build(root)
        scroll.addView(root)
        shell.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        activity.setContentView(shell)
        Ui.animateIn(root)
    }

    private fun showCentral(): Unit {
        val summary = InspectionEngine.summary(data)
        screen("Inspeção de Campo", "Use em 3 passos simples", onBackHome) { root ->
            val hero = Ui.heroCard(activity)
            hero.addView(Ui.section(activity, data.environmentType.ifBlank { "Escolha o ambiente" }))
            hero.addView(Ui.title(activity, "Ronda de segurança", 25f))
            hero.addView(Ui.label(activity, data.place.ifBlank { "Registre o local, os achados e gere um relatório." }))
            val chips = Ui.row(activity)
            chips.addView(Ui.chip(activity, "${summary.total} achado(s)", Ui.AMBER))
            chips.addView(Ui.chip(activity, "${summary.open} em aberto", if (summary.open > 0) Ui.RED else Ui.GREEN), lpWrap(8, 0, 0, 0))
            hero.addView(chips, spaced())
            root.addView(hero, spaced())

            root.addView(stepCard("1", "Informar local", "Empresa, setor, ambiente e inspetor", isDone = data.company.isNotBlank() && data.place.isNotBlank() && data.inspector.isNotBlank()) { showData() }, spaced())
            root.addView(stepCard("2", "Registrar achados", "Anote o que foi visto em campo", isDone = data.records.isNotEmpty()) { showRecords() }, spaced())
            root.addView(stepCard("3", "Gerar relatório", "Revise e compartilhe o PDF", isDone = false) { showReview() }, spaced())

            val support = Ui.card(activity)
            support.addView(Ui.section(activity, "Resumo"))
            support.addView(Ui.value(activity, "Abertos: ${summary.open}  •  Alta/Crítica: ${summary.highCritical}", Ui.TEXT))
            support.addView(Ui.label(activity, "O plano de ação é montado automaticamente com base nos achados."), smallTop())
            root.addView(support, spaced())

            val actions = Ui.row(activity)
            actions.addView(Ui.ghostButton(activity, "Histórico").apply { setOnClickListener { showHistoryDialog() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            actions.addView(Ui.dangerButton(activity, "Limpar").apply { setOnClickListener { clearDraft() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(8), 0, 0, 0) })
            root.addView(actions, spaced())
        }
    }

    private fun showData(): Unit = screen("Passo 1: Local", "Preencha só o necessário para identificar a inspeção") { root ->
        val environmentButton = Ui.button(activity, "Ambiente: ${data.environmentType.ifBlank { "Outro" }}")
        environmentButton.setOnClickListener {
            choose("Tipo de ambiente", InspectionEngine.environments, data.environmentType.ifBlank { "Outro" }) {
                data.environmentType = it
                repo.save(data)
                showData()
            }
        }
        root.addView(environmentButton, spaced())

        val company = field(root, "Empresa / cliente", data.company)
        val place = field(root, "Local da inspeção", data.place)
        val area = field(root, "Área / setor", data.area)
        val inspector = field(root, "Seu nome / inspetor", data.inspector)
        val objective = field(root, "Observação inicial", data.objective, true)

        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Data e hora"))
        card.addView(Ui.value(activity, format.format(Date(data.dateMillis)), Ui.TEXT))
        card.addView(Ui.button(activity, "Usar data/hora atual").apply { setOnClickListener { data.dateMillis = System.currentTimeMillis(); repo.save(data); showData() } }, buttonLp())
        root.addView(card, spaced())

        root.addView(Ui.button(activity, "Salvar e continuar").apply {
            setOnClickListener {
                data.company = company.text.toString()
                data.area = area.text.toString()
                data.place = place.text.toString()
                data.inspector = inspector.text.toString()
                data.objective = objective.text.toString()
                if (data.number.isBlank()) data.number = InspectionEngine.number(data.dateMillis)
                repo.save(data)
                showRecords()
            }
        }, spaced())
    }

    private fun showRecords(): Unit = screen("Passo 2: Achados", "Registre cada situação encontrada") { root ->
        val add = Ui.button(activity, "+ Registrar achado")
        add.setOnClickListener { showRecordDialog(null) }
        root.addView(add, spaced())
        root.addView(Ui.label(activity, "Exemplo: extintor obstruído, cabo exposto, colaborador sem EPI, área desorganizada."), spaced())
        if (data.records.isEmpty()) {
            root.addView(messageCard("Nenhum achado registrado ainda."), spaced())
        } else {
            data.records.forEachIndexed { index, record -> root.addView(recordCard(index, record), spaced()) }
        }
        root.addView(Ui.button(activity, "Continuar para relatório").apply { setOnClickListener { showReview() } }, spaced())
    }

    private fun showActionPlan(): Unit = screen("Plano de ação", "Pendências criadas automaticamente") { root ->
        val open = data.records.filter { it.status != "Resolvido" && it.status != "Arquivado" }
        if (open.isEmpty()) {
            root.addView(messageCard("Nenhuma pendência aberta no momento."), spaced())
        } else {
            open.forEachIndexed { index, record ->
                val card = Ui.card(activity)
                card.addView(Ui.chip(activity, record.priority, InspectionEngine.priorityColor(record.priority)))
                card.addView(Ui.value(activity, record.description.ifBlank { "Achado sem descrição" }, Ui.TEXT), smallTop())
                card.addView(Ui.label(activity, "Ação: ${record.recommendation.ifBlank { "Definir ação recomendada" }}"), smallTop())
                card.addView(Ui.label(activity, "Responsável: ${record.responsible.ifBlank { "-" }} | Prazo: ${record.deadline.ifBlank { "-" }}"), smallTop())
                card.addView(Ui.ghostButton(activity, "Editar").apply { setOnClickListener { showRecordDialog(data.records.indexOf(record).coerceAtLeast(index)) } }, buttonLp())
                root.addView(card, spaced())
            }
        }
    }

    private fun showReview(): Unit = screen("Passo 3: Relatório", "Confira antes de gerar o PDF") { root ->
        val summary = InspectionEngine.summary(data)
        val pending = InspectionEngine.pending(data)
        val card = Ui.heroCard(activity)
        card.addView(Ui.section(activity, "Resumo da inspeção"))
        card.addView(Ui.value(activity, "Local: ${data.place.ifBlank { "Não informado" }}"))
        card.addView(Ui.value(activity, "Ambiente: ${data.environmentType.ifBlank { "Outro" }}"))
        card.addView(Ui.value(activity, "Achados: ${summary.total} | Abertos: ${summary.open} | Alta/Crítica: ${summary.highCritical}"))
        if (pending.isEmpty()) {
            card.addView(Ui.chip(activity, "PRONTO PARA GERAR", Ui.GREEN), buttonLp())
        } else {
            card.addView(Ui.chip(activity, "FALTA PREENCHER", Ui.RED), buttonLp())
            pending.forEach { card.addView(Ui.label(activity, "• $it"), smallTop()) }
        }
        root.addView(card, spaced())
        root.addView(actionCard("Revisar local", "Voltar ao passo 1") { showData() }, spaced())
        root.addView(actionCard("Revisar achados", "Voltar ao passo 2") { showRecords() }, spaced())
        root.addView(actionCard("Ver plano de ação", "Responsáveis, prazos e pendências") { showActionPlan() }, spaced())
        root.addView(Ui.button(activity, "Gerar e compartilhar PDF").apply { setOnClickListener { generatePdf() } }, spaced())
    }

    private fun showRecordDialog(editIndex: Int?): Unit {
        val editing = editIndex != null
        val original = editIndex?.let { data.records[it] }
        val availableCategories = InspectionEngine.categoriesFor(data.environmentType)
        var category = original?.category ?: availableCategories.first()
        if (!availableCategories.contains(category)) category = "Outro"
        var priority = original?.priority ?: "Média"
        var status = original?.status ?: "Aberto"

        val panel = Ui.vbox(activity, dp(14))
        val description = Ui.input(activity, "O que foi encontrado?", true)
        val location = Ui.input(activity, "Onde foi encontrado?")
        val recommendation = Ui.input(activity, "O que precisa ser feito?", true)
        val responsible = Ui.input(activity, "Quem deve resolver? Opcional")
        val deadline = Ui.input(activity, "Prazo. Opcional")
        val risk = Ui.input(activity, "Risco observado. Opcional", true)
        val notes = Ui.input(activity, "Observação pessoal. Opcional", true)

        description.setText(original?.description.orEmpty())
        location.setText(original?.location.orEmpty())
        recommendation.setText(original?.recommendation.orEmpty())
        responsible.setText(original?.responsible.orEmpty())
        deadline.setText(original?.deadline.orEmpty())
        risk.setText(original?.risk.orEmpty())
        notes.setText(original?.notes.orEmpty())

        val categoryBtn = Ui.ghostButton(activity, "Categoria: $category")
        categoryBtn.setOnClickListener { choose("Categoria", availableCategories, category) { category = it; categoryBtn.text = "Categoria: $it" } }
        val priorityBtn = Ui.ghostButton(activity, "Prioridade: $priority")
        priorityBtn.setOnClickListener { choose("Prioridade", InspectionEngine.priorities, priority) { priority = it; priorityBtn.text = "Prioridade: $it" } }
        val statusBtn = Ui.ghostButton(activity, "Status: $status")
        statusBtn.setOnClickListener { choose("Status", InspectionEngine.statuses, status) { status = it; statusBtn.text = "Status: $it" } }

        panel.addView(Ui.label(activity, "Ambiente: ${data.environmentType.ifBlank { "Outro" }}"), buttonLp())
        listOf(categoryBtn, description, location, priorityBtn, recommendation, responsible, deadline, statusBtn, risk, notes).forEach { panel.addView(it, buttonLp()) }

        AlertDialog.Builder(activity)
            .setTitle(if (editing) "Editar achado" else "Registrar achado")
            .setView(panel)
            .setPositiveButton(if (editing) "Salvar" else "Adicionar") { _, _ ->
                if (description.text.toString().isBlank()) {
                    Toast.makeText(activity, "Descreva o que foi encontrado", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val record = original ?: InspectionRecord()
                record.category = category
                record.priority = priority
                record.status = status
                record.location = location.text.toString()
                record.description = description.text.toString()
                record.risk = risk.text.toString()
                record.recommendation = recommendation.text.toString()
                record.responsible = responsible.text.toString()
                record.deadline = deadline.text.toString()
                record.notes = notes.text.toString()
                if (editIndex == null) data.records.add(0, record) else data.records[editIndex] = record
                repo.save(data)
                showRecords()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun recordCard(index: Int, record: InspectionRecord): LinearLayout {
        val card = Ui.card(activity)
        val top = Ui.row(activity)
        top.addView(Ui.chip(activity, record.priority, InspectionEngine.priorityColor(record.priority)))
        top.addView(Ui.chip(activity, record.status, InspectionEngine.statusColor(record.status)), lpWrap(8, 0, 0, 0))
        card.addView(top)
        card.addView(Ui.value(activity, record.description.ifBlank { "Sem descrição" }, Ui.TEXT), smallTop())
        card.addView(Ui.label(activity, "Categoria: ${record.category}"), smallTop())
        card.addView(Ui.label(activity, "Local: ${record.location.ifBlank { "-" }}"), smallTop())
        if (record.recommendation.isNotBlank()) card.addView(Ui.label(activity, "Ação: ${record.recommendation}"), smallTop())
        if (record.responsible.isNotBlank() || record.deadline.isNotBlank()) card.addView(Ui.label(activity, "Responsável: ${record.responsible.ifBlank { "-" }} | Prazo: ${record.deadline.ifBlank { "-" }}"), smallTop())
        val actions = Ui.row(activity)
        actions.addView(Ui.ghostButton(activity, "Editar").apply { setOnClickListener { showRecordDialog(index) } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        actions.addView(Ui.dangerButton(activity, "Remover").apply { setOnClickListener { data.records.removeAt(index); repo.save(data); showRecords() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(8), 0, 0, 0) })
        card.addView(actions, buttonLp())
        return card
    }

    private fun generatePdf(): Unit {
        val pending = InspectionEngine.pending(data)
        if (pending.isNotEmpty()) {
            Toast.makeText(activity, pending.first(), Toast.LENGTH_LONG).show()
            return
        }
        if (data.number.isBlank()) data.number = InspectionEngine.number(data.dateMillis)
        val file = InspectionPdfGenerator(activity, repo).generate(data)
        data.history.add(0, InspectionHistoryItem(data.number, format.format(Date()), data.company, data.place, data.inspector, file.name))
        data.history = data.history.take(20).toMutableList()
        repo.save(data)
        shareFile(file)
    }

    private fun shareFile(file: File): Unit {
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.provider", file)
        val share = Intent(Intent.ACTION_SEND)
        share.type = "application/pdf"
        share.putExtra(Intent.EXTRA_STREAM, uri)
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        activity.startActivity(Intent.createChooser(share, "Compartilhar inspeção SafeField"))
    }

    private fun showHistoryDialog(): Unit {
        val panel = Ui.vbox(activity, dp(14))
        panel.setBackgroundColor(Ui.SHELL)
        panel.addView(Ui.title(activity, "Histórico de inspeções", 22f))
        if (data.history.isEmpty()) panel.addView(Ui.value(activity, "Nenhum relatório gerado ainda.", Ui.MUTED), spaced()) else data.history.forEach { item ->
            val card = Ui.card(activity)
            card.addView(Ui.value(activity, item.number, Ui.AMBER))
            card.addView(Ui.label(activity, "${item.company} - ${item.place}"))
            card.addView(Ui.label(activity, "Gerado em ${item.generatedAt} por ${item.inspector}"))
            card.addView(Ui.ghostButton(activity, "Compartilhar").apply {
                setOnClickListener {
                    val file = File(activity.cacheDir, item.fileName)
                    if (file.exists()) shareFile(file) else Toast.makeText(activity, "Arquivo não encontrado neste aparelho", Toast.LENGTH_SHORT).show()
                }
            }, buttonLp())
            panel.addView(card, spaced())
        }
        AlertDialog.Builder(activity).setView(panel).setPositiveButton("Fechar", null).show()
    }

    private fun clearDraft(): Unit {
        AlertDialog.Builder(activity)
            .setTitle("Limpar inspeção?")
            .setMessage("O histórico de relatórios será mantido.")
            .setPositiveButton("Limpar") { _, _ -> data = repo.clearDraftKeepHistory(data); showCentral() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun choose(title: String, options: List<String>, current: String, onSelected: (String) -> Unit): Unit {
        val index = options.indexOf(current).coerceAtLeast(0)
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setSingleChoiceItems(options.toTypedArray(), index) { dialog, which -> onSelected(options[which]); dialog.dismiss() }
            .show()
    }

    private fun field(root: LinearLayout, label: String, value: String, multi: Boolean = false): EditText {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, label))
        val edit = Ui.input(activity, label, multi)
        edit.setText(value)
        card.addView(edit, buttonLp())
        root.addView(card, spaced())
        return edit
    }

    private fun metric(label: String, value: String, color: Int): LinearLayout {
        val card = Ui.card(activity)
        card.gravity = Gravity.CENTER
        card.addView(Ui.value(activity, value, color).apply { textSize = 24f; gravity = Gravity.CENTER })
        card.addView(Ui.label(activity, label).apply { gravity = Gravity.CENTER })
        return card
    }

    private fun stepCard(number: String, title: String, subtitle: String, isDone: Boolean, action: () -> Unit): LinearLayout {
        val card = Ui.card(activity)
        val row = Ui.row(activity)
        row.addView(Ui.iconBubble(activity, number, if (isDone) Ui.GREEN else Ui.AMBER))
        val texts = Ui.vbox(activity)
        texts.setPadding(dp(12), 0, 0, 0)
        texts.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        texts.addView(Ui.value(activity, title, Ui.TEXT))
        texts.addView(Ui.label(activity, subtitle))
        row.addView(texts)
        row.addView(Ui.chip(activity, if (isDone) "OK" else "Abrir", if (isDone) Ui.GREEN else Ui.AMBER))
        card.addView(row)
        card.setOnClickListener { action() }
        return card
    }

    private fun actionCard(title: String, subtitle: String, action: () -> Unit): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.value(activity, title, Ui.TEXT))
        card.addView(Ui.label(activity, subtitle))
        card.setOnClickListener { action() }
        return card
    }

    private fun messageCard(message: String): LinearLayout = Ui.card(activity).apply { addView(Ui.value(activity, message, Ui.MUTED)) }
    private fun spaced(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(12)) }
    private fun buttonLp(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(10), 0, 0) }
    private fun smallTop(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(6), 0, 0) }
    private fun lpWrap(l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(l), dp(t), dp(r), dp(b)) }
    private fun dp(value: Int): Int = Ui.dp(activity, value)
}
