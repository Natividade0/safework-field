package com.safefield.app

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

internal class HomeDashboard(
    private val activity: MainActivity,
    private val data: PtData,
    private val openPt: () -> Unit,
    private val openPlaceholder: (String) -> Unit,
    private val openPending: (PtFlowState) -> Unit,
    private val openHistory: () -> Unit,
    private val openModules: () -> Unit
) {
    private data class Indicator(val label: String, val value: String, val color: Int)

    fun renderInto(container: LinearLayout): Unit {
        val flow = PtFlowEngine.flow(data)
        container.addView(header(flow).margin(0, 8.dp()))
        container.addView(mainPtCard(flow).margin(0, 8.dp()))
        container.addView(indicators(flow).margin(0, 6.dp()))
        container.addView(miniDashboard(flow).margin(0, 8.dp()))
        container.addView(shortcuts(flow).margin(0, 8.dp()))
    }

    fun moduleGrid(compact: Boolean): GridLayout {
        val grid = GridLayout(activity)
        grid.columnCount = 2
        modules().forEach { name ->
            val tile = moduleTile(name, compact)
            tile.layoutParams = gridParams()
            grid.addView(tile)
        }
        return grid
    }

    private fun header(flow: PtFlowState): LinearLayout {
        val card = Ui.card(activity)
        val top = Ui.row(activity)
        top.gravity = Gravity.CENTER_VERTICAL
        val menu = Ui.ghostButton(activity, "Menu")
        menu.layoutParams = LinearLayout.LayoutParams(92.dp(), 52.dp())
        menu.setOnClickListener { showMenuDialog() }
        top.addView(menu)
        val info = Ui.vbox(activity)
        info.setPadding(14.dp(), 0, 0, 0)
        info.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        info.addView(Ui.label(activity, "Painel de campo"))
        info.addView(Ui.value(activity, "PT atual, pendencias, evidencias e historico.", Ui.TEXT))
        top.addView(info)
        top.addView(Ui.chip(activity, flow.status.name, statusColor(flow.status)))
        card.addView(top)
        return card
    }

    private fun mainPtCard(flow: PtFlowState): LinearLayout {
        val card = Ui.heroCard(activity)
        val top = Ui.row(activity)
        val titleBox = Ui.vbox(activity)
        titleBox.addView(Ui.label(activity, "Permissao de Trabalho"))
        titleBox.addView(Ui.title(activity, flow.number, 23f))
        titleBox.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        top.addView(titleBox)
        top.addView(Ui.chip(activity, flow.status.name, statusColor(flow.status)))
        card.addView(top)
        card.addView(Ui.value(activity, flow.validityLabel).margin(0, 8.dp()))
        flow.validityAlert?.let { card.addView(Ui.value(activity, it, Ui.AMBER_SOFT).margin(0, 3.dp())) }
        card.addView(metricStrip(listOf(
            Indicator("Criticas", flow.critical.size.toString(), Ui.RED),
            Indicator("Importantes", flow.important.size.toString(), Ui.AMBER),
            Indicator("Fotos", data.photoUris.size.toString(), Ui.AMBER_SOFT)
        )).margin(0, 10.dp()))
        val action = Ui.button(activity, "Continuar PT")
        action.setOnClickListener { openPt() }
        card.addView(action.margin(0, 8.dp()))
        return card
    }

    private fun indicators(flow: PtFlowState): GridLayout {
        val grid = GridLayout(activity)
        grid.columnCount = 2
        listOf(
            Indicator("Pendencias criticas", flow.critical.size.toString(), Ui.RED),
            Indicator("Pendencias importantes", flow.important.size.toString(), Ui.AMBER),
            Indicator("Fotos anexadas", data.photoUris.size.toString(), Ui.AMBER_SOFT),
            Indicator("Trabalhadores", data.workers.size.toString(), Ui.GREEN)
        ).forEach { item ->
            val tile = indicatorTile(item)
            tile.layoutParams = gridParams()
            grid.addView(tile)
        }
        return grid
    }

    private fun miniDashboard(flow: PtFlowState): LinearLayout {
        val card = Ui.card(activity)
        card.addView(sectionTitle("Dashboard"))
        card.addView(summaryRow("Status da PT", flow.status.name, statusColor(flow.status)).margin(0, 8.dp()))
        card.addView(summaryRow("Validade", flow.validityLabel, Ui.TEXT).margin(0, 4.dp()))
        card.addView(summaryRow("Fotos", data.photoUris.size.toString(), Ui.AMBER_SOFT).margin(0, 4.dp()))
        card.addView(summaryRow("Trabalhadores", data.workers.size.toString(), Ui.GREEN).margin(0, 4.dp()))
        card.addView(summaryRow("PTs emitidas", data.history.size.toString(), Ui.AMBER_SOFT).margin(0, 4.dp()))
        card.addView(summaryRow("Vencidas / encerradas", "${expiredCount()} / ${closedCount()}", Ui.RED).margin(0, 4.dp()))
        val last = data.history.firstOrNull()
        if (last != null) {
            card.addView(Ui.divider(activity).margin(0, 10.dp()))
            card.addView(Ui.label(activity, "Ultima emissao"))
            card.addView(Ui.value(activity, "${last.ptNumber.ifBlank { "PT" }} - ${last.emittedAt}", Ui.TEXT).margin(0, 4.dp()))
            card.addView(Ui.label(activity, last.place.ifBlank { "Sem local" }))
        }
        return card
    }

    private fun shortcuts(flow: PtFlowState): LinearLayout {
        val card = Ui.card(activity)
        card.addView(sectionTitle("Atalhos principais"))
        val grid = GridLayout(activity)
        grid.columnCount = 2
        listOf(
            "Continuar PT" to View.OnClickListener { openPt() },
            "Ver pendencias" to View.OnClickListener { openPending(flow) },
            "Historico" to View.OnClickListener { showHistoryDialog("TODAS") },
            "Modulos" to View.OnClickListener { showMenuDialog() }
        ).forEach { shortcut ->
            val button = Ui.ghostButton(activity, shortcut.first)
            button.setOnClickListener(shortcut.second)
            button.layoutParams = gridParams()
            grid.addView(button)
        }
        card.addView(grid.margin(0, 8.dp()))
        return card
    }

    private fun showHistoryDialog(filter: String): Unit {
        val dialog = AlertDialog.Builder(activity).create()
        val panel = Ui.vbox(activity, 16.dp())
        panel.background = Ui.bg(Ui.PANEL, 24.dp(), Ui.BORDER, 1)
        panel.addView(Ui.chip(activity, "GESTAO DE PTS", Ui.AMBER))
        panel.addView(Ui.title(activity, "Historico de PTs", 22f).margin(0, 8.dp()))
        panel.addView(historyFilterRow(dialog, filter).margin(0, 8.dp()))
        val items = historyByFilter(filter)
        if (items.isEmpty()) {
            panel.addView(Ui.label(activity, "Nenhuma PT encontrada para este filtro.").margin(0, 8.dp()))
        } else {
            items.take(10).forEach { panel.addView(historyItem(dialog, item = it).margin(0, 6.dp())) }
        }
        val close = Ui.ghostButton(activity, "Fechar")
        close.setOnClickListener { dialog.dismiss() }
        panel.addView(close.margin(0, 10.dp()))
        dialog.setView(panel)
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun historyFilterRow(dialog: AlertDialog, filter: String): LinearLayout {
        val row = Ui.row(activity)
        listOf("TODAS" to "Todas", "VALIDAS" to "Validas", "VENCIDAS" to "Vencidas", "ENCERRADAS" to "Encerradas").forEach { option ->
            val button = if (filter == option.first) Ui.button(activity, option.second, Ui.AMBER) else Ui.ghostButton(activity, option.second)
            button.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            button.setOnClickListener { dialog.dismiss(); showHistoryDialog(option.first) }
            row.addView(button)
        }
        return row
    }

    private fun historyItem(dialog: AlertDialog, item: PtHistoryItem): LinearLayout {
        val card = Ui.card(activity)
        val state = historyState(item)
        val top = Ui.row(activity)
        val titleBox = Ui.vbox(activity)
        titleBox.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        titleBox.addView(Ui.label(activity, item.emittedAt.ifBlank { "Sem data" }))
        titleBox.addView(Ui.value(activity, item.ptNumber.ifBlank { "PT emitida" }, Ui.TEXT))
        top.addView(titleBox)
        top.addView(Ui.chip(activity, state, historyColor(state)))
        card.addView(top)
        card.addView(Ui.label(activity, "Empresa: ${item.company.ifBlank { "-" }}").margin(0, 6.dp()))
        card.addView(Ui.label(activity, "Local: ${item.place.ifBlank { "-" }}"))
        card.addView(Ui.label(activity, "Responsavel: ${item.responsible.ifBlank { "-" }}"))
        card.addView(Ui.label(activity, "Termino: ${if (item.endMillis > 0L) Ui.fmt(item.endMillis) else "nao registrado"}"))
        val row = Ui.row(activity)
        val details = Ui.ghostButton(activity, "Detalhes")
        details.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        details.setOnClickListener { showHistoryDetails(item) }
        row.addView(details)
        val share = Ui.ghostButton(activity, "Compartilhar")
        share.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        share.setOnClickListener { shareHistoryPdf(item) }
        row.addView(share)
        card.addView(row.margin(0, 8.dp()))
        val row2 = Ui.row(activity)
        val close = Ui.ghostButton(activity, "Encerrar")
        close.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        close.setOnClickListener { closeHistory(dialog, item) }
        row2.addView(close)
        val duplicate = Ui.button(activity, "Duplicar", Ui.AMBER)
        duplicate.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        duplicate.setOnClickListener { dialog.dismiss(); duplicateHistory(item) }
        row2.addView(duplicate)
        card.addView(row2.margin(0, 4.dp()))
        return card
    }

    private fun showHistoryDetails(item: PtHistoryItem): Unit {
        val message = "Numero: ${item.ptNumber.ifBlank { "-" }}\nEmpresa: ${item.company.ifBlank { "-" }}\nLocal: ${item.place.ifBlank { "-" }}\nResponsavel: ${item.responsible.ifBlank { "-" }}\nEmissao: ${item.emittedAt.ifBlank { "-" }}\nArquivo: ${item.fileName.ifBlank { "-" }}\nStatus: ${item.status}"
        AlertDialog.Builder(activity).setTitle("Detalhes da PT").setMessage(message).setPositiveButton("OK", null).show()
    }

    private fun shareHistoryPdf(item: PtHistoryItem): Unit {
        val file = File(activity.cacheDir, item.fileName)
        if (item.fileName.isBlank() || !file.exists()) {
            Toast.makeText(activity, "Arquivo PDF nao encontrado neste aparelho", Toast.LENGTH_LONG).show()
            return
        }
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.provider", file)
        val share = Intent(Intent.ACTION_SEND)
        share.type = "application/pdf"
        share.putExtra(Intent.EXTRA_STREAM, uri)
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        activity.startActivity(Intent.createChooser(share, "Compartilhar PT SafeField"))
    }

    private fun closeHistory(dialog: AlertDialog, item: PtHistoryItem): Unit {
        if (item.status == "ENCERRADA") { Toast.makeText(activity, "Esta PT ja esta encerrada", Toast.LENGTH_SHORT).show(); return }
        val note = Ui.input(activity, "Observacao de encerramento", true)
        AlertDialog.Builder(activity)
            .setTitle("Encerrar PT")
            .setView(note)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Encerrar") { _, _ ->
                val index = data.history.indexOf(item)
                if (index in data.history.indices) {
                    data.history[index] = item.copy(status = "ENCERRADA", closedAt = Ui.fmt(System.currentTimeMillis()), closeNote = note.text.toString())
                    PtRepository(activity).save(data)
                    dialog.dismiss()
                    showHistoryDialog("ENCERRADAS")
                }
            }
            .show()
    }

    private fun duplicateHistory(item: PtHistoryItem): Unit {
        data.company = item.company.ifBlank { data.company }
        data.place = item.place.ifBlank { data.place }
        data.responsible = item.responsible.ifBlank { data.responsible }
        data.startMillis = System.currentTimeMillis()
        data.endMillis = data.startMillis + data.validityHours * 60L * 60L * 1000L
        data.checklist.clear()
        data.controls.clear()
        data.photoUris.clear()
        data.signatureB64 = ""
        data.workers = data.workers.map { Worker(it.name, it.role) }.toMutableList()
        PtRepository(activity).save(data)
        Toast.makeText(activity, "Dados duplicados para uma nova PT", Toast.LENGTH_LONG).show()
        openPt()
    }

    private fun historyByFilter(filter: String): List<PtHistoryItem> {
        return data.history.filter { item ->
            when (filter) {
                "VALIDAS" -> historyState(item) == "VALIDA"
                "VENCIDAS" -> historyState(item) == "VENCIDA"
                "ENCERRADAS" -> historyState(item) == "ENCERRADA"
                else -> true
            }
        }
    }

    private fun historyState(item: PtHistoryItem): String {
        return when {
            item.status == "ENCERRADA" -> "ENCERRADA"
            item.endMillis > 0L && System.currentTimeMillis() > item.endMillis -> "VENCIDA"
            else -> "VALIDA"
        }
    }

    private fun historyColor(state: String): Int {
        return when (state) {
            "ENCERRADA" -> Ui.MUTED
            "VENCIDA" -> Ui.RED
            else -> Ui.GREEN
        }
    }

    private fun summaryRow(label: String, value: String, color: Int): LinearLayout {
        val row = Ui.row(activity)
        val left = Ui.label(activity, label)
        left.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(left)
        row.addView(Ui.value(activity, value, color))
        return row
    }

    private fun indicatorTile(item: Indicator): LinearLayout {
        val tile = Ui.card(activity)
        val value = Ui.title(activity, item.value, 22f)
        value.setTextColor(item.color)
        tile.addView(value)
        tile.addView(Ui.label(activity, item.label))
        return tile
    }

    private fun metricStrip(items: List<Indicator>): LinearLayout {
        val row = Ui.row(activity)
        items.forEach { item ->
            val tile = Ui.vbox(activity, 8.dp())
            tile.background = Ui.bg(Ui.SHELL, 14.dp(), Ui.BORDER, 1)
            val value = Ui.title(activity, item.value, 18f)
            value.setTextColor(item.color)
            tile.addView(value)
            tile.addView(Ui.label(activity, item.label))
            tile.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(4.dp(), 0, 4.dp(), 0) }
            row.addView(tile)
        }
        return row
    }

    private fun showMenuDialog(): Unit {
        val dialog = AlertDialog.Builder(activity).create()
        val panel = Ui.vbox(activity, 16.dp())
        panel.background = Ui.bg(Ui.PANEL, 24.dp(), Ui.BORDER, 1)
        panel.addView(Ui.chip(activity, "MENU", Ui.AMBER))
        panel.addView(Ui.title(activity, "Modulos", 22f).margin(0, 10.dp()))
        panel.addView(menuItem("Permissao de Trabalho", "Abrir fluxo completo", "PT") { dialog.dismiss(); openPt() }.margin(0, 10.dp()))
        modules().filter { it != "Permissao de Trabalho" }.forEach { name ->
            panel.addView(menuItem(name, "Em desenvolvimento", initial(name)) { dialog.dismiss(); openPlaceholder(name) }.margin(0, 5.dp()))
        }
        panel.addView(menuItem("Configuracoes / Sobre", "Informacoes do aplicativo", "SF") { dialog.dismiss(); openPlaceholder("Sobre o SafeField") }.margin(0, 5.dp()))
        dialog.setView(panel)
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun menuItem(title: String, subtitle: String, initials: String, action: () -> Unit): LinearLayout {
        val item = Ui.card(activity)
        item.setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
        val row = Ui.row(activity)
        row.addView(bubble(initials, if (title.contains("Trabalho")) Ui.AMBER else Ui.BORDER))
        val texts = Ui.vbox(activity)
        texts.setPadding(12.dp(), 0, 0, 0)
        texts.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        texts.addView(Ui.value(activity, title, Ui.TEXT))
        texts.addView(Ui.label(activity, subtitle))
        row.addView(texts)
        item.addView(row)
        item.setOnClickListener { action() }
        return item
    }

    private fun moduleTile(name: String, compact: Boolean): LinearLayout {
        val isPt = name == "Permissao de Trabalho"
        val card = if (isPt) Ui.heroCard(activity) else Ui.card(activity)
        val row = Ui.row(activity)
        row.addView(bubble(initial(name), if (isPt) Ui.AMBER else Ui.BORDER))
        val texts = Ui.vbox(activity)
        texts.setPadding(12.dp(), 0, 0, 0)
        texts.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        texts.addView(Ui.title(activity, name, if (compact) 15f else 18f))
        texts.addView(Ui.label(activity, if (isPt) "Fluxo completo" else "Em desenvolvimento"))
        row.addView(texts)
        card.addView(row)
        card.setOnClickListener { if (isPt) openPt() else openPlaceholder(name) }
        return card
    }

    private fun sectionTitle(text: String): TextView = Ui.section(activity, text)

    private fun bubble(text: String, color: Int): TextView {
        val bubble = TextView(activity)
        bubble.text = text
        bubble.textSize = 14f
        bubble.gravity = Gravity.CENTER
        bubble.setTextColor(Ui.TEXT)
        bubble.background = Ui.bg(Ui.PANEL, 16.dp(), color, 1)
        bubble.layoutParams = LinearLayout.LayoutParams(48.dp(), 48.dp())
        return bubble
    }

    private fun gridParams(): GridLayout.LayoutParams {
        val params = GridLayout.LayoutParams()
        params.width = 0
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.setMargins(5.dp(), 5.dp(), 5.dp(), 5.dp())
        return params
    }

    private fun statusColor(status: PtStatus): Int {
        return when (status) {
            PtStatus.LIBERADA -> Ui.GREEN
            PtStatus.BLOQUEADA, PtStatus.EXPIRADA -> Ui.RED
            PtStatus.RASCUNHO -> Ui.AMBER
        }
    }

    private fun expiredCount(): Int {
        val now = System.currentTimeMillis()
        return data.history.count { it.status != "ENCERRADA" && it.endMillis > 0L && now > it.endMillis }
    }

    private fun closedCount(): Int = data.history.count { it.status == "ENCERRADA" }

    private fun initial(name: String): String {
        return when (name) {
            "Permissao de Trabalho" -> "PT"
            "Inspecao" -> "IN"
            "Ocorrencia" -> "OC"
            "Colaboradores" -> "CL"
            "Dashboard" -> "DB"
            else -> name.take(3).uppercase()
        }
    }

    private fun modules(): List<String> {
        return listOf("Permissao de Trabalho", "APR", "DDS", "EPI", "Inspecao", "Ocorrencia", "Colaboradores", "Dashboard")
    }

    private fun Int.dp(): Int = Ui.dp(activity, this)
}
