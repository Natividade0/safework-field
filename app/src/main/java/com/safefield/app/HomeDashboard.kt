package com.safefield.app

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView

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
    private data class ModuleItem(val title: String, val subtitle: String, val initials: String, val color: Int, val action: () -> Unit)

    fun renderInto(container: LinearLayout): Unit {
        val flow = PtFlowEngine.flow(data)
        container.addView(topHeader().margin(0, 8.dp()))
        container.addView(todayOverview(flow).margin(0, 8.dp()))
        container.addView(primaryModules().margin(0, 8.dp()))
        container.addView(workPanel(flow).margin(0, 8.dp()))
    }

    fun moduleGrid(compact: Boolean): GridLayout {
        val grid = GridLayout(activity)
        grid.columnCount = 2
        modules().forEach { module ->
            val tile = moduleTile(module, compact)
            tile.layoutParams = gridParams()
            grid.addView(tile)
        }
        return grid
    }

    private fun topHeader(): LinearLayout {
        val card = Ui.heroCard(activity)
        val row = Ui.row(activity)
        row.gravity = Gravity.CENTER_VERTICAL
        row.addView(Ui.iconBubble(activity, "SF", Ui.AMBER))

        val info = Ui.vbox(activity)
        info.setPadding(12.dp(), 0, 0, 0)
        info.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        info.addView(Ui.label(activity, "Aplicativo de campo"))
        info.addView(Ui.title(activity, "SafeField", 28f))
        info.addView(Ui.label(activity, "Escolha um módulo e registre tudo de forma simples."))
        row.addView(info)

        val menu = Ui.ghostButton(activity, "Menu")
        menu.layoutParams = LinearLayout.LayoutParams(86.dp(), 48.dp())
        menu.setOnClickListener { showMenuDialog() }
        row.addView(menu)
        card.addView(row)
        return card
    }

    private fun todayOverview(flow: PtFlowState): LinearLayout {
        val card = Ui.card(activity)
        val top = Ui.row(activity)
        top.addView(Ui.section(activity, "Visão geral"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(Ui.chip(activity, "Offline", Ui.GREEN))
        card.addView(top)

        val grid = GridLayout(activity)
        grid.columnCount = 2
        val pending = flow.critical.size + flow.important.size
        listOf(
            Indicator("Pendências", pending.toString(), if (flow.critical.isNotEmpty()) Ui.RED else Ui.AMBER),
            Indicator("Inspeções", "0", Ui.GREEN),
            Indicator("PTs emitidas", data.history.size.toString(), Ui.AMBER_SOFT),
            Indicator("Fotos PT", data.photoUris.size.toString(), Ui.AMBER_SOFT)
        ).forEach { item ->
            val tile = indicatorTile(item)
            tile.layoutParams = gridParams()
            grid.addView(tile)
        }
        card.addView(grid.margin(0, 8.dp()))
        return card
    }

    private fun primaryModules(): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Módulos"))
        card.addView(Ui.label(activity, "Acesse cada ferramenta separadamente. Nenhum módulo depende do outro para começar."), smallTop())
        card.addView(moduleGrid(compact = true).margin(0, 8.dp()))
        return card
    }

    private fun workPanel(flow: PtFlowState): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Acompanhamento"))
        card.addView(summaryRow("Status da PT atual", flow.status.name, statusColor(flow.status)).margin(0, 8.dp()))
        card.addView(summaryRow("Validade", flow.validityLabel, Ui.TEXT).margin(0, 4.dp()))
        card.addView(summaryRow("Vencidas / encerradas", "${expiredCount()} / ${closedCount()}", Ui.RED).margin(0, 4.dp()))
        flow.validityAlert?.let { card.addView(Ui.value(activity, it, Ui.AMBER_SOFT).margin(0, 8.dp())) }
        val last = data.history.firstOrNull()
        if (last != null) {
            card.addView(Ui.divider(activity).margin(0, 10.dp()))
            card.addView(Ui.label(activity, "Último registro de PT"))
            card.addView(Ui.value(activity, "${last.ptNumber.ifBlank { "PT" }} • ${last.emittedAt}", Ui.TEXT).margin(0, 4.dp()))
            card.addView(Ui.label(activity, last.place.ifBlank { "Sem local" }))
        }
        return card
    }

    private fun showHistoryDialog(): Unit {
        val dialog = AlertDialog.Builder(activity).create()
        val panel = Ui.vbox(activity, 16.dp())
        panel.background = Ui.bg(Ui.PANEL, 24.dp(), Ui.BORDER, 1)
        panel.addView(Ui.chip(activity, "HISTÓRICO", Ui.AMBER))
        panel.addView(Ui.title(activity, "Registros recentes", 22f).margin(0, 8.dp()))
        if (data.history.isEmpty()) {
            panel.addView(Ui.label(activity, "Nenhuma PT emitida neste aparelho.").margin(0, 8.dp()))
        } else {
            data.history.take(8).forEach { item ->
                val card = Ui.card(activity)
                val state = historyState(item)
                card.addView(Ui.chip(activity, state, historyColor(state)))
                card.addView(Ui.value(activity, item.ptNumber.ifBlank { "PT emitida" }, Ui.TEXT).margin(0, 4.dp()))
                card.addView(Ui.label(activity, "${item.emittedAt.ifBlank { "sem data" }} • ${item.place.ifBlank { "sem local" }}"))
                if (item.closedAt.isNotBlank()) card.addView(Ui.label(activity, "Encerrada em: ${item.closedAt}"))
                panel.addView(card.margin(0, 6.dp()))
            }
        }
        panel.addView(Ui.ghostButton(activity, "Fechar").apply { setOnClickListener { dialog.dismiss() } }.margin(0, 10.dp()))
        dialog.setView(panel)
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun showMenuDialog(): Unit {
        val dialog = AlertDialog.Builder(activity).create()
        val panel = Ui.vbox(activity, 16.dp())
        panel.background = Ui.bg(Ui.PANEL, 24.dp(), Ui.BORDER, 1)
        panel.addView(Ui.chip(activity, "MENU", Ui.AMBER))
        panel.addView(Ui.title(activity, "Módulos SafeField", 22f).margin(0, 10.dp()))
        modules().forEach { module ->
            panel.addView(menuItem(module) {
                dialog.dismiss()
                module.action.invoke()
            }.margin(0, 5.dp()))
        }
        panel.addView(menuItem(ModuleItem("Configurações / Sobre", "Informações do aplicativo", "SF", Ui.BORDER) { openPlaceholder("Sobre o SafeField") }) {
            dialog.dismiss()
            openPlaceholder("Sobre o SafeField")
        }.margin(0, 5.dp()))
        dialog.setView(panel)
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun menuItem(module: ModuleItem, action: () -> Unit): LinearLayout {
        val item = Ui.card(activity)
        item.setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
        val row = Ui.row(activity)
        row.addView(bubble(module.initials, module.color))
        val texts = Ui.vbox(activity)
        texts.setPadding(12.dp(), 0, 0, 0)
        texts.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        texts.addView(Ui.value(activity, module.title, Ui.TEXT))
        texts.addView(Ui.label(activity, module.subtitle))
        row.addView(texts)
        item.addView(row)
        item.setOnClickListener { action() }
        return item
    }

    private fun moduleTile(module: ModuleItem, compact: Boolean): LinearLayout {
        val card = Ui.card(activity)
        val pad = if (compact) 12.dp() else 16.dp()
        card.setPadding(pad, pad, pad, pad)
        val row = Ui.row(activity)
        row.addView(bubble(module.initials, module.color))
        val texts = Ui.vbox(activity)
        texts.setPadding(10.dp(), 0, 0, 0)
        texts.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        texts.addView(Ui.value(activity, module.title, Ui.TEXT))
        texts.addView(Ui.label(activity, module.subtitle))
        row.addView(texts)
        card.addView(row)
        card.setOnClickListener { module.action.invoke() }
        return card
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
        tile.setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
        val value = Ui.title(activity, item.value, 22f)
        value.setTextColor(item.color)
        tile.addView(value)
        tile.addView(Ui.label(activity, item.label))
        return tile
    }

    private fun bubble(text: String, color: Int): TextView {
        val bubble = TextView(activity)
        bubble.text = text
        bubble.textSize = 13f
        bubble.gravity = Gravity.CENTER
        bubble.setTextColor(if (color == Ui.AMBER_SOFT) Color.BLACK else Ui.TEXT)
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

    private fun modules(): List<ModuleItem> = listOf(
        ModuleItem("Inspeção", "Achados e relatório", "IN", Ui.GREEN) { openInspection() },
        ModuleItem("Permissão de Trabalho", "Emissão e controle", "PT", Ui.AMBER) { openPt() },
        ModuleItem("DDS", "Diálogo de segurança", "DS", Ui.BORDER_SOFT) { openPlaceholder("DDS") },
        ModuleItem("EPI", "Entrega e controle", "EP", Ui.BORDER_SOFT) { openPlaceholder("EPI") },
        ModuleItem("Ocorrência", "Incidentes e desvios", "OC", Ui.RED) { openPlaceholder("Ocorrência") },
        ModuleItem("Colaboradores", "Equipe e documentos", "CL", Ui.BORDER_SOFT) { openPlaceholder("Colaboradores") },
        ModuleItem("Dashboard", "Indicadores gerais", "DB", Ui.AMBER_SOFT) { openPlaceholder("Dashboard") }
    )

    private fun statusColor(status: PtStatus): Int {
        return when (status) {
            PtStatus.LIBERADA -> Ui.GREEN
            PtStatus.BLOQUEADA, PtStatus.EXPIRADA -> Ui.RED
            PtStatus.RASCUNHO -> Ui.AMBER
        }
    }

    private fun historyState(item: PtHistoryItem): String {
        return when {
            item.status == "ENCERRADA" -> "ENCERRADA"
            item.endMillis > 0L && System.currentTimeMillis() > item.endMillis -> "VENCIDA"
            else -> "VÁLIDA"
        }
    }

    private fun historyColor(state: String): Int {
        return when (state) {
            "ENCERRADA" -> Ui.MUTED
            "VENCIDA" -> Ui.RED
            else -> Ui.GREEN
        }
    }

    private fun expiredCount(): Int {
        val now = System.currentTimeMillis()
        return data.history.count { it.status != "ENCERRADA" && it.endMillis > 0L && now > it.endMillis }
    }

    private fun closedCount(): Int = data.history.count { it.status == "ENCERRADA" }

    private fun openInspection(): Unit {
        InspectionModule(activity) { openModules() }.show()
    }

    private fun smallTop(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 6.dp(), 0, 0) }
    private fun Int.dp(): Int = Ui.dp(activity, this)
}
