package com.safefield.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
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
    private var activeFilter: String = "Todos"
    private val photoRequest: Int = 9201
    private var pendingPhotoRecordIndex: Int? = null

    fun show(): Unit = showStart()

    fun handleActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?): Boolean {
        if (requestCode != photoRequest) return false
        if (resultCode == Activity.RESULT_OK) {
            val index = pendingPhotoRecordIndex
            if (index != null && index in data.records.indices) {
                val clip = resultData?.clipData
                if (clip != null) {
                    for (i in 0 until clip.itemCount) addPhotoToRecord(index, clip.getItemAt(i).uri)
                } else {
                    resultData?.data?.let { addPhotoToRecord(index, it) }
                }
                repo.save(data)
                Toast.makeText(activity, "Foto(s) anexada(s)", Toast.LENGTH_SHORT).show()
                showFindingDetail(index)
            }
        }
        pendingPhotoRecordIndex = null
        return true
    }

    private fun addPhotoToRecord(index: Int, uri: Uri): Unit {
        runCatching { activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        data.records[index].photos.add(uri.toString())
    }

    private fun pickPhotosForRecord(index: Int): Unit {
        pendingPhotoRecordIndex = index
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        activity.startActivityForResult(intent, photoRequest)
    }

    private fun screen(title: String, subtitle: String, back: () -> Unit = ::showStart, build: (LinearLayout) -> Unit): Unit {
        registerAndroidBack(back)
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

    private fun registerAndroidBack(back: () -> Unit): Unit {
        if (activity !is MainActivity) return
        runCatching {
            val field = MainActivity::class.java.getDeclaredField("currentBack")
            field.isAccessible = true
            val handler: () -> Unit = { repo.save(data); back() }
            field.set(activity, handler)
        }
    }

    private fun showStart(): Unit {
        val hasInspection = data.company.isNotBlank() || data.place.isNotBlank() || data.records.isNotEmpty()
        screen("Inspeções", "Registre achados de campo sem burocracia", onBackHome) { root ->
            val hero = Ui.heroCard(activity)
            hero.addView(Ui.section(activity, "INSPEÇÃO DE CAMPO"))
            hero.addView(Ui.title(activity, "Ronda rápida", 26f), smallTop())
            hero.addView(Ui.label(activity, "Crie uma inspeção, adicione achados e gere o PDF no final."), smallTop())
            val mainButton = Ui.button(activity, if (hasInspection) "Continuar inspeção" else "+ Nova inspeção")
            mainButton.setOnClickListener { if (hasInspection) showInspection() else showCreateInspection() }
            hero.addView(mainButton, buttonLp())
            root.addView(hero, spaced())

            if (hasInspection) root.addView(currentInspectionCard(), spaced())

            val row = Ui.row(activity)
            row.addView(Ui.ghostButton(activity, "Histórico").apply { setOnClickListener { showHistoryDialog() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(Ui.ghostButton(activity, "Como usar").apply { setOnClickListener { showHelpDialog() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(8), 0, 0, 0) })
            root.addView(row, spaced())
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
            chips.addView(Ui.chip(activity, "${summary.open} abertos", if (summary.open > 0) Ui.AMBER else Ui.GREEN), lpWrap(8, 0, 0, 0))
            chips.addView(Ui.chip(activity, "${summary.highCritical} alta/crítica", if (summary.highCritical > 0) Ui.RED else Ui.GREEN), lpWrap(8, 0, 0, 0))
            top.addView(chips, buttonLp())
            root.addView(top, spaced())

            val actionGrid = GridLayout(activity).apply { columnCount = 2 }
            actionGrid.addView(primaryAction("+ Achado", "Registrar manualmente", Ui.BLUE) { showFindingDialog(null) }, gridParams())
            actionGrid.addView(primaryAction("Modelos", "Usar achado pronto", Ui.GREEN) { showQuickTemplates() }, gridParams())
            root.addView(actionGrid, spaced())

            if (data.records.isEmpty()) {
                root.addView(emptyState(), spaced())
            } else {
                root.addView(filterLine(), spaced())
                val filtered = filteredRecords()
                if (filtered.isEmpty()) root.addView(emptyFilteredState(), spaced())
                filtered.forEach { pair -> root.addView(findingCard(pair.first, pair.second), spaced()) }
            }

            val actions = Ui.row(activity)
            actions.addView(Ui.ghostButton(activity, "Editar dados").apply { setOnClickListener { showCreateInspection() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            actions.addView(Ui.button(activity, "Gerar PDF").apply { setOnClickListener { showReport() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(8), 0, 0, 0) })
            root.addView(actions, spaced())
        }
    }

    private fun filteredRecords(): List<Pair<Int, InspectionRecord>> {
        return data.records.mapIndexed { index, record -> index to record }.filter { item ->
            val record = item.second
            when (activeFilter) {
                "Abertos" -> record.status == "Aberto" || record.status == "Em andamento"
                "Resolvidos" -> record.status == "Resolvido"
                "Alta/Crítica" -> record.priority == "Alta" || record.priority == "Crítica"
                "Sem responsável" -> record.status != "Resolvido" && record.status != "Arquivado" && record.responsible.isBlank()
                else -> true
            }
        }
    }

    private fun showQuickTemplates(): Unit = screen("Modelos rápidos", "Escolha um achado comum para preencher automaticamente", ::showInspection) { root ->
        val grid = GridLayout(activity).apply { columnCount = 2 }
        InspectionEngine.quickFindings.forEach { preset ->
            val card = Ui.card(activity)
            card.addView(Ui.iconBubble(activity, presetIcon(preset), presetColor(preset)), LinearLayout.LayoutParams(dp(44), dp(44)))
            card.addView(Ui.value(activity, preset, Ui.TEXT), buttonLp())
            card.addView(Ui.label(activity, InspectionEngine.quickRecommendationFor(preset).ifBlank { "Descrever manualmente" }), smallTop())
            card.setOnClickListener { showFindingDialog(null, preset) }
            grid.addView(card, gridParams())
        }
        root.addView(grid, spaced())
    }

    private fun showReport(): Unit = screen("Relatório", "Revise antes de compartilhar", ::showInspection) { root ->
        val summary = InspectionEngine.summary(data)
        val pending = InspectionEngine.pending(data)
        val noResponsible = data.records.count { it.status != "Resolvido" && it.status != "Arquivado" && it.responsible.isBlank() }
        val card = Ui.heroCard(activity)
        card.addView(Ui.section(activity, "RESUMO EXECUTIVO"))
        card.addView(Ui.value(activity, data.objective.ifBlank { "Inspeção de campo" }, Ui.TEXT), smallTop())
        card.addView(Ui.label(activity, "Local: ${data.place.ifBlank { "-" }}"), smallTop())
        if (pending.isEmpty()) card.addView(Ui.chip(activity, "Pronto para gerar", Ui.GREEN), buttonLp()) else {
            card.addView(Ui.chip(activity, "Falta preencher", Ui.RED), buttonLp())
            pending.forEach { card.addView(Ui.label(activity, "• $it"), smallTop()) }
        }
        root.addView(card, spaced())

        val grid = GridLayout(activity).apply { columnCount = 2 }
        listOf(
            Triple("Achados", summary.total.toString(), Ui.BLUE),
            Triple("Abertos", summary.open.toString(), Ui.AMBER),
            Triple("Alta/Crítica", summary.highCritical.toString(), Ui.RED),
            Triple("Sem responsável", noResponsible.toString(), if (noResponsible > 0) Ui.RED else Ui.GREEN)
        ).forEach { item ->
            val stat = Ui.card(activity)
            stat.addView(Ui.chip(activity, item.first, item.third))
            stat.addView(Ui.title(activity, item.second, 26f), buttonLp())
            grid.addView(stat, gridParams())
        }
        root.addView(grid, spaced())

        root.addView(actionCard("Ver achados", "Revisar a lista antes do PDF") { showInspection() }, spaced())
        root.addView(Ui.button(activity, "Gerar e compartilhar PDF").apply { setOnClickListener { generatePdf() } }, spaced())
    }

    private fun showFindingDialog(editIndex: Int?, preset: String? = null): Unit {
        val original = editIndex?.let { data.records[it] }
        val categories = InspectionEngine.categoriesFor(data.environmentType)
        var category = original?.category ?: preset?.let { InspectionEngine.quickCategoryFor(it) } ?: "Outro"
        if (!categories.contains(category)) category = "Outro"
        var priority = original?.priority ?: "Média"
        var status = original?.status ?: "Aberto"

        val panel = Ui.vbox(activity, dp(14))
        val description = Ui.input(activity, "O que foi encontrado?", true)
        val location = Ui.input(activity, "Onde? Opcional")
        val recommendation = Ui.input(activity, "O que precisa ser feito? Opcional", true)
        val responsible = Ui.input(activity, "Quem deve resolver? Opcional")
        val deadline = Ui.input(activity, "Prazo. Opcional")

        description.setText(original?.description ?: if (preset == "Outro") "" else preset.orEmpty())
        location.setText(original?.location.orEmpty())
        recommendation.setText(original?.recommendation ?: preset?.let { InspectionEngine.quickRecommendationFor(it) }.orEmpty())
        responsible.setText(original?.responsible.orEmpty())
        deadline.setText(original?.deadline.orEmpty())

        val categoryBtn = Ui.ghostButton(activity, "Categoria: $category")
        categoryBtn.setOnClickListener { choose("Categoria", categories, category) { category = it; categoryBtn.text = "Categoria: $it" } }
        val priorityBtn = Ui.ghostButton(activity, "Gravidade: $priority")
        priorityBtn.setOnClickListener { choose("Gravidade", InspectionEngine.priorities, priority) { priority = it; priorityBtn.text = "Gravidade: $it" } }
        val statusBtn = Ui.ghostButton(activity, "Status: $status")
        statusBtn.setOnClickListener { choose("Status", InspectionEngine.statuses, status) { status = it; statusBtn.text = "Status: $it" } }

        listOf(description, location, categoryBtn, priorityBtn, recommendation, responsible, deadline, statusBtn).forEach { panel.addView(it, buttonLp()) }

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
                record.deadline = deadline.text.toString()
                record.status = status
                if (editIndex == null) data.records.add(0, record) else data.records[editIndex] = record
                repo.save(data)
                showInspection()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showFindingDetail(index: Int): Unit {
        val record = data.records.getOrNull(index) ?: return
        screen("Detalhe do achado", record.status, ::showInspection) { root ->
            val card = Ui.heroCard(activity)
            card.addView(Ui.chip(activity, record.priority, InspectionEngine.priorityColor(record.priority)))
            card.addView(Ui.title(activity, record.description.ifBlank { "Achado sem descrição" }, 22f), smallTop())
            card.addView(Ui.label(activity, "Categoria: ${record.category}"), smallTop())
            card.addView(Ui.label(activity, "Local: ${record.location.ifBlank { "-" }}"), smallTop())
            card.addView(Ui.label(activity, "Status: ${record.status}"), smallTop())
            card.addView(Ui.label(activity, "Fotos anexadas: ${record.photos.size}"), smallTop())
            root.addView(card, spaced())

            root.addView(detailCard("Ação recomendada", record.recommendation.ifBlank { "Não informada" }), spaced())
            root.addView(detailCard("Responsável", record.responsible.ifBlank { "Não informado" }), spaced())
            root.addView(detailCard("Prazo", record.deadline.ifBlank { "Não informado" }), spaced())
            if (record.risk.isNotBlank()) root.addView(detailCard("Risco observado", record.risk), spaced())
            if (record.notes.isNotBlank()) root.addView(detailCard("Observação", record.notes), spaced())

            root.addView(photoManagerCard(index, record), spaced())

            val actions = Ui.row(activity)
            actions.addView(Ui.ghostButton(activity, "Editar").apply { setOnClickListener { showFindingDialog(index) } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            if (record.status != "Resolvido") actions.addView(Ui.button(activity, "Resolver").apply { setOnClickListener { markResolved(index) } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(8), 0, 0, 0) })
            root.addView(actions, spaced())
            root.addView(Ui.button(activity, "Duplicar achado", Ui.BLUE).apply { setOnClickListener { duplicateRecord(index) } }, spaced())
            if (record.status == "Resolvido") root.addView(Ui.ghostButton(activity, "Reabrir achado").apply { setOnClickListener { reopenRecord(index) } }, spaced())
            root.addView(Ui.dangerButton(activity, "Remover achado").apply { setOnClickListener { data.records.removeAt(index); repo.save(data); showInspection() } }, spaced())
        }
    }

    private fun photoManagerCard(index: Int, record: InspectionRecord): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Evidências fotográficas"))
        card.addView(Ui.value(activity, "${record.photos.size} foto(s) anexada(s)", Ui.TEXT), smallTop())
        card.addView(Ui.button(activity, "Adicionar fotos").apply { setOnClickListener { pickPhotosForRecord(index) } }, buttonLp())
        if (record.photos.isNotEmpty()) {
            record.photos.forEachIndexed { photoIndex, _ ->
                val row = Ui.row(activity)
                row.addView(Ui.label(activity, "Foto ${photoIndex + 1}"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                row.addView(Ui.dangerButton(activity, "Remover").apply { setOnClickListener { removePhoto(index, photoIndex) } }, LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT))
                card.addView(row, smallTop())
            }
        }
        return card
    }

    private fun removePhoto(index: Int, photoIndex: Int): Unit {
        if (index !in data.records.indices) return
        val photos = data.records[index].photos
        if (photoIndex !in photos.indices) return
        photos.removeAt(photoIndex)
        repo.save(data)
        showFindingDetail(index)
    }

    private fun duplicateRecord(index: Int): Unit {
        val source = data.records.getOrNull(index) ?: return
        val copy = InspectionRecord(
            category = source.category,
            location = source.location,
            description = "${source.description} (cópia)",
            risk = source.risk,
            recommendation = source.recommendation,
            responsible = source.responsible,
            deadline = source.deadline,
            priority = source.priority,
            status = "Aberto",
            notes = source.notes,
            photos = source.photos.toMutableList()
        )
        data.records.add(0, copy)
        repo.save(data)
        showInspection()
    }

    private fun reopenRecord(index: Int): Unit {
        val record = data.records.getOrNull(index) ?: return
        record.status = "Aberto"
        record.notes = listOf(record.notes, "Reaberto em ${format.format(Date())}.").filter { it.isNotBlank() }.joinToString("\n")
        repo.save(data)
        showFindingDetail(index)
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
        if (record.deadline.isNotBlank()) card.addView(Ui.label(activity, "Prazo: ${record.deadline}"), smallTop())
        card.addView(Ui.label(activity, "Fotos: ${record.photos.size}"), smallTop())
        val actions = Ui.row(activity)
        actions.addView(Ui.ghostButton(activity, "Ver detalhes").apply { setOnClickListener { showFindingDetail(index) } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (record.status != "Resolvido") {
            actions.addView(Ui.button(activity, "Resolver").apply { setOnClickListener { markResolved(index) } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(8), 0, 0, 0) })
        }
        card.addView(actions, buttonLp())
        card.setOnClickListener { showFindingDetail(index) }
        return card
    }

    private fun markResolved(index: Int): Unit {
        val record = data.records.getOrNull(index) ?: return
        record.status = "Resolvido"
        if (record.notes.isBlank()) record.notes = "Marcado como resolvido em ${format.format(Date())}."
        repo.save(data)
        Toast.makeText(activity, "Achado marcado como resolvido", Toast.LENGTH_SHORT).show()
        showInspection()
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
            background = Ui.bg(Ui.CARD_SOFT, dp(999), color, 1)
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
        }
    }

    private fun filterLine(): LinearLayout {
        val box = Ui.vbox(activity)
        box.addView(Ui.section(activity, "Filtros"))
        val grid = GridLayout(activity).apply { columnCount = 2 }
        listOf("Todos", "Abertos", "Resolvidos", "Alta/Crítica", "Sem responsável").forEach { filter ->
            val selected = activeFilter == filter
            val button = Ui.ghostButton(activity, filter).apply {
                setTextColor(if (selected) Ui.BLUE else Ui.TEXT)
                background = Ui.bg(if (selected) Ui.CARD_SOFT else 0xFFFFFFFF.toInt(), dp(16), if (selected) Ui.BLUE else Ui.BORDER, 1)
                setOnClickListener { activeFilter = filter; showInspection() }
            }
            grid.addView(button, gridParams())
        }
        box.addView(grid, smallTop())
        return box
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
        card.addView(Ui.label(activity, "Toque em + Achado ou use modelos rápidos para começar."), smallTop())
        return card
    }

    private fun emptyFilteredState(): LinearLayout {
        val card = Ui.card(activity)
        card.gravity = Gravity.CENTER_HORIZONTAL
        card.addView(Ui.title(activity, "Nenhum achado neste filtro", 19f))
        card.addView(Ui.label(activity, "Altere o filtro ou registre um novo achado."), smallTop())
        return card
    }

    private fun detailCard(title: String, value: String): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, title))
        card.addView(Ui.value(activity, value, Ui.TEXT), smallTop())
        return card
    }

    private fun primaryAction(title: String, subtitle: String, color: Int, action: () -> Unit): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.iconBubble(activity, title.take(1), color), LinearLayout.LayoutParams(dp(44), dp(44)))
        card.addView(Ui.value(activity, title, Ui.TEXT), buttonLp())
        card.addView(Ui.label(activity, subtitle), smallTop())
        card.setOnClickListener { action() }
        return card
    }

    private fun presetIcon(preset: String): String = when (preset) {
        "Cabo exposto" -> "E"
        "Extintor obstruído" -> "F"
        "Falta de sinalização" -> "S"
        "Colaborador sem EPI" -> "EPI"
        "Máquina sem proteção" -> "M"
        "Risco de queda" -> "Q"
        "Área desorganizada" -> "O"
        "Produto químico sem identificação" -> "Q"
        else -> "+"
    }

    private fun presetColor(preset: String): Int = when (preset) {
        "Cabo exposto", "Máquina sem proteção", "Risco de queda" -> Ui.RED
        "Extintor obstruído", "Falta de sinalização", "Produto químico sem identificação" -> Ui.AMBER
        "Colaborador sem EPI", "Área desorganizada" -> Ui.BLUE
        else -> Ui.GREEN
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
            .setMessage("1. Toque em Nova inspeção.\n2. Informe local e área.\n3. Use modelos rápidos ou adicione achados manualmente.\n4. Use os filtros para priorizar pendências.\n5. Anexe fotos no detalhe do achado.\n6. Gere o PDF ao final da ronda.")
            .setPositiveButton("Entendi", null)
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

    private fun gridParams(): GridLayout.LayoutParams = GridLayout.LayoutParams().apply {
        width = 0
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        setMargins(dp(5), dp(5), dp(5), dp(5))
    }

    private fun spaced(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(12)) }
    private fun buttonLp(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(10), 0, 0) }
    private fun smallTop(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(6), 0, 0) }
    private fun lpWrap(l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(l), dp(t), dp(r), dp(b)) }
    private fun dp(value: Int): Int = Ui.dp(activity, value)
}
