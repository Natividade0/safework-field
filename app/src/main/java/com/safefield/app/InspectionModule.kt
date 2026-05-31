package com.safefield.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.view.Gravity
import android.view.ViewGroup
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

class InspectionModule(
    private val activity: Activity,
    private val onBackHome: () -> Unit
) {
    private val repo = InspectionRepository(activity)
    private var data: InspectionData = repo.load()
    private val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

    fun show(): Unit = showStart()

    private fun screen(title: String, subtitle: String, back: () -> Unit = ::showStart, build: (LinearLayout) -> Unit): Unit {
        val shell = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Ui.SHELL) }
        val header = Ui.row(activity).apply {
            setPadding(dp(16), dp(12), dp(16), dp(10))
            background = Ui.bg(Ui.DARK, 0, Ui.BORDER, 1)
        }
        header.addView(Ui.ghostButton(activity, "Voltar").apply { setOnClickListener { repo.save(data); back() } }, LinearLayout.LayoutParams(dp(92), ViewGroup.LayoutParams.WRAP_CONTENT))
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

    private fun showStart(): Unit {
        val hasInspection = data.company.isNotBlank() || data.place.isNotBlank() || data.records.isNotEmpty()
        screen("Inspeções", "Registre achados de campo sem burocracia", onBackHome) { root ->
            val hero = Ui.heroCard(activity)
            hero.addView(Ui.section(activity, "INSPEÇÃO V2"))
            hero.addView(Ui.title(activity, "Ronda rápida", 26f), smallTop())
            hero.addView(Ui.label(activity, "Crie uma inspeção, adicione achados e gere o PDF no final."), smallTop())
            val mainButton = Ui.button(activity, if (hasInspection) "Continuar inspeção" else "+ Nova inspeção")
            mainButton.setOnClickListener { if (hasInspection) showInspection() else showCreateInspection() }
            hero.addView(mainButton, buttonLp())
            root.addView(hero, spaced())

            if (hasInspection) {
                root.addView(currentInspectionCard(), spaced())
            }

            root.addView(actionCard("Histórico", "Relatórios de inspeção gerados neste aparelho") { showHistoryDialog() }, spaced())
            root.addView(actionCard("Como usar", "1. Crie a inspeção  2. Adicione achados  3. Gere o PDF") { showHelpDialog() }, spaced())
        }
    }

    private fun showCreateInspection(): Unit = screen("Nova inspeção", "Informe apenas o básico para começar") { root ->
        val title = field(root, "Título da inspeção", data.objective.ifBlank { "Inspeção de campo" })
        val company = field(root, "Empresa / cliente", data.company)
        val place = field(root, "Local", data.place)
        val area = field(root, "Área / setor", data.area)
        val inspector = field(root, "Inspetor", data.inspector)

        val envBtn = Ui.button(activity, "Ambiente: ${data.environmentType.ifBlank { "Outro" }}")
        envBtn.setOnClickListener {
            choose("Tipo de ambiente", InspectionEngine.environments, data.environmentType.ifBlank { "Outro" }) {
                data.environmentType = it
                repo.save(data)
                showCreateInspection()
            }
        }
        root.addView(envBtn, spaced())

        val dateCard = Ui.card(activity)
        dateCard.addView(Ui.section(activity, "Data"))
        dateCard.addView(Ui.value(activity, format.format(Date(data.dateMillis)), Ui.TEXT), smallTop())
        dateCard.addView(Ui.ghostButton(activity, "Usar data/hora atual").apply { setOnClickListener { data.dateMillis = System.currentTimeMillis(); repo.save(data); showCreateInspection() } }, buttonLp())
        root.addView(dateCard, spaced())

        root.addView(Ui.button(activity, "Criar e adicionar achados").apply {
            setOnClickListener {
                data.objective = title.text.toString().ifBlank { "Inspeção de campo" }
                data.company = company.text.toString()
                data.place = place.text.toString()
                data.area = area.text.toString()
                data.inspector = inspector.text.toString()
                if (data.number.isBlank()) data.number = InspectionEngine.number(data.dateMillis)
                repo.save(data)
                showInspection()
            }
        }, spaced())
    }

    private fun showInspection(): Unit {
        val summary = InspectionEngine.summary(data)
        screen(data.objective.ifBlank { "Inspeção de campo" }, "${summary.total} achado(s) • ${summary.open} aberto(s)") { root ->
            val top = Ui.heroCard(activity)
            top.addView(Ui.section(activity, data.environmentType.ifBlank { "Outro" }))
            top.addView(Ui.title(activity, data.place.ifBlank { "Local não informado" }, 23f), smallTop())
            top.addView(Ui.label(activity, "${data.company.ifBlank { "Empresa não informada" }} • ${data.area.ifBlank { "Sem área" }}"), smallTop())
            val chips = Ui.row(activity)
            chips.addView(Ui.chip(activity, "${summary.total} achados", Ui.BLUE))
            chips.addView(Ui.chip(activity, "${summary.highCritical} alta/crítica", if (summary.highCritical > 0) Ui.RED else Ui.GREEN), lpWrap(8, 0, 0, 0))
            top.addView(chips, buttonLp())
            root.addView(top, spaced())

            root.addView(Ui.button(activity, "+ Adicionar achado").apply { setOnClickListener { showFindingDialog(null) } }, spaced())

            if (data.records.isEmpty()) {
                root.addView(emptyState(), spaced())
            } else {
                root.addView(filterLine(), spaced())
                data.records.forEachIndexed { index, record -> root.addView(findingCard(index, record), spaced()) }
            }

            val actions = Ui.row(activity)
            actions.addView(Ui.ghostButton(activity, "Editar dados").apply { setOnClickListener { showCreateInspection() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            actions.addView(Ui.button(activity, "Gerar PDF").apply { setOnClickListener { showReport() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(8), 0, 0, 0) })
            root.addView(actions, spaced())
        }
    }

    private fun showReport(): Unit = screen("Relatório", "Revise antes de compartilhar") { root ->
        val summary = InspectionEngine.summary(data)
        val pending = InspectionEngine.pending(data)
        val card = Ui.heroCard(activity)
        card.addView(Ui.section(activity, "RESUMO"))
        card.addView(Ui.value(activity, data.objective.ifBlank { "Inspeção de campo" }, Ui.TEXT), smallTop())
        card.addView(Ui.label(activity, "Local: ${data.place.ifBlank { "-" }}"), smallTop())
        card.addView(Ui.label(activity, "Achados: ${summary.total} • Abertos: ${summary.open} • Resolvidos: ${summary.resolved}"), smallTop())
        card.addView(Ui.label(activity, "Alta/Crítica: ${summary.highCritical}"), smallTop())
        if (pending.isEmpty()) card.addView(Ui.chip(activity, "Pronto para gerar", Ui.GREEN), buttonLp()) else {
            card.addView(Ui.chip(activity, "Falta preencher", Ui.RED), buttonLp())
            pending.forEach { card.addView(Ui.label(activity, "• $it"), smallTop()) }
        }
        root.addView(card, spaced())
        root.addView(actionCard("Ver achados", "Revisar a lista antes do PDF") { showInspection() }, spaced())
        root.addView(Ui.button(activity, "Gerar e compartilhar PDF").apply { setOnClickListener { generatePdf() } }, spaced())
    }

    private fun showFindingDialog(editIndex: Int?): Unit {
        val original = editIndex?.let { data.records[it] }
        val categories = InspectionEngine.categoriesFor(data.environmentType)
        var category = original?.category ?: "Outro"
        if (!categories.contains(category)) category = "Outro"
        var priority = original?.priority ?: "Média"
        var status = original?.status ?: "Aberto"

        val panel = Ui.vbox(activity, dp(14))
        val description = Ui.input(activity, "O que foi encontrado?", true)
        val location = Ui.input(activity, "Onde? Opcional")
        val recommendation = Ui.input(activity, "O que precisa ser feito? Opcional", true)
        val responsible = Ui.input(activity, "Quem deve resolver? Opcional")

        description.setText(original?.description.orEmpty())
        location.setText(original?.location.orEmpty())
        recommendation.setText(original?.recommendation.orEmpty())
        responsible.setText(original?.responsible.orEmpty())

        val categoryBtn = Ui.ghostButton(activity, "Categoria: $category")
        categoryBtn.setOnClickListener { choose("Categoria", categories, category) { category = it; categoryBtn.text = "Categoria: $it" } }
        val priorityBtn = Ui.ghostButton(activity, "Gravidade: $priority")
        priorityBtn.setOnClickListener { choose("Gravidade", InspectionEngine.priorities, priority) { priority = it; priorityBtn.text = "Gravidade: $it" } }
        val statusBtn = Ui.ghostButton(activity, "Status: $status")
        statusBtn.setOnClickListener { choose("Status", InspectionEngine.statuses, status) { status = it; statusBtn.text = "Status: $it" } }

        listOf(description, location, categoryBtn, priorityBtn, recommendation, responsible, statusBtn).forEach { panel.addView(it, buttonLp()) }

        AlertDialog.Builder(activity)
            .setTitle(if (editIndex == null) "Adicionar achado" else "Editar achado")
            .setView(panel)
            .setPositiveButton("Salvar") { _, _ ->
                if (description.text.toString().isBlank()) {
                    Toast.makeText(activity, "Descreva o que foi encontrado", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val record = original ?: InspectionRecord()
                record.description = description.text.toString()
                record.location = location.text.toString()
                record.category = category
                record.priority = priority
                record.recommendation = recommendation.text.toString()
                record.responsible = responsible.text.toString()
                record.status = status
                if (editIndex == null) data.records.add(0, record) else data.records[editIndex] = record
                repo.save(data)
                showInspection()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun findingCard(index: Int, record: InspectionRecord): LinearLayout {
        val card = Ui.card(activity)
        val row = Ui.row(activity)
        row.addView(severityDot(record.priority))
        val content = Ui.vbox(activity)
        content.setPadding(dp(10), 0, 0, 0)
        content.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        content.addView(Ui.value(activity, record.description.ifBlank { "Achado sem descrição" }, Ui.TEXT))
        val sub = listOf(record.category, record.location.ifBlank { null }, record.responsible.ifBlank { null }).filterNotNull().joinToString(" • ")
        content.addView(Ui.label(activity, sub.ifBlank { "Sem detalhes adicionais" }), smallTop())
        row.addView(content)
        row.addView(Ui.chip(activity, record.status, InspectionEngine.statusColor(record.status)))
        card.addView(row)
        if (record.recommendation.isNotBlank()) card.addView(Ui.label(activity, "Ação: ${record.recommendation}"), buttonLp())
        val actions = Ui.row(activity)
        actions.addView(Ui.ghostButton(activity, "Editar").apply { setOnClickListener { showFindingDialog(index) } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        actions.addView(Ui.dangerButton(activity, "Remover").apply { setOnClickListener { data.records.removeAt(index); repo.save(data); showInspection() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(8), 0, 0, 0) })
        card.addView(actions, buttonLp())
        return card
    }

    private fun severityDot(priority: String): TextView {
        val color = InspectionEngine.priorityColor(priority)
        return TextView(activity).apply {
            text = when (priority) {
                "Crítica" -> "!"
                "Alta" -> "A"
                "Média" -> "M"
                else -> "B"
            }
            gravity = Gravity.CENTER
            textSize = 13f
            setTextColor(color)
            background = Ui.bg(Ui.PANEL, dp(999), color, 1)
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
        }
    }

    private fun filterLine(): LinearLayout {
        val row = Ui.row(activity)
        val summary = InspectionEngine.summary(data)
        row.addView(Ui.chip(activity, "Todos ${summary.total}", Ui.BLUE))
        row.addView(Ui.chip(activity, "Abertos ${summary.open}", if (summary.open > 0) Ui.AMBER else Ui.GREEN), lpWrap(8, 0, 0, 0))
        row.addView(Ui.chip(activity, "Resolvidos ${summary.resolved}", Ui.GREEN), lpWrap(8, 0, 0, 0))
        return row
    }

    private fun currentInspectionCard(): LinearLayout {
        val summary = InspectionEngine.summary(data)
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "EM ANDAMENTO"))
        card.addView(Ui.value(activity, data.objective.ifBlank { "Inspeção de campo" }, Ui.TEXT), smallTop())
        card.addView(Ui.label(activity, "${data.place.ifBlank { "Sem local" }} • ${summary.total} achado(s)"), smallTop())
        card.setOnClickListener { showInspection() }
        return card
    }

    private fun emptyState(): LinearLayout {
        val card = Ui.card(activity)
        card.gravity = Gravity.CENTER_HORIZONTAL
        card.addView(Ui.title(activity, "Nenhum achado ainda", 20f))
        card.addView(Ui.label(activity, "Toque em + Adicionar achado para registrar o que encontrou em campo."), smallTop())
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
            card.addView(Ui.value(activity, item.number, Ui.BLUE))
            card.addView(Ui.label(activity, "${item.company} - ${item.place}"), smallTop())
            card.addView(Ui.label(activity, "Gerado em ${item.generatedAt} por ${item.inspector}"), smallTop())
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

    private fun showHelpDialog(): Unit {
        AlertDialog.Builder(activity)
            .setTitle("Como usar")
            .setMessage("1. Toque em Nova inspeção.\n2. Informe local e área.\n3. Adicione os achados encontrados.\n4. Gere o PDF ao final da ronda.")
            .setPositiveButton("Entendi", null)
            .show()
    }

    private fun clearInspection(): Unit {
        AlertDialog.Builder(activity)
            .setTitle("Limpar inspeção atual?")
            .setMessage("O histórico será mantido.")
            .setPositiveButton("Limpar") { _, _ -> data = repo.clearDraftKeepHistory(data); showStart() }
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

    private fun actionCard(title: String, subtitle: String, action: () -> Unit): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.value(activity, title, Ui.TEXT))
        card.addView(Ui.label(activity, subtitle), smallTop())
        card.setOnClickListener { action() }
        return card
    }

    private fun spaced(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(12)) }
    private fun buttonLp(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(10), 0, 0) }
    private fun smallTop(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(6), 0, 0) }
    private fun lpWrap(l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(l), dp(t), dp(r), dp(b)) }
    private fun dp(value: Int): Int = Ui.dp(activity, value)
}
