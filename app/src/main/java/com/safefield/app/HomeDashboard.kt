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
    private data class ModuleItem(val title: String, val subtitle: String, val initials: String, val color: Int, val action: () -> Unit)
    private data class StatItem(val title: String, val value: String, val delta: String, val color: Int)

    fun renderInto(container: LinearLayout): Unit {
        val flow = PtFlowEngine.flow(data)
        container.addView(appTopBar().margin(0, 2.dp()))
        container.addView(tabBar().margin(0, 8.dp()))
        container.addView(greeting().margin(0, 8.dp()))
        container.addView(summaryCards(flow).margin(0, 8.dp()))
        container.addView(quickActions().margin(0, 8.dp()))
        container.addView(moduleStrip().margin(0, 8.dp()))
        container.addView(recentCard(flow).margin(0, 8.dp()))
    }

    fun moduleGrid(compact: Boolean): GridLayout {
        val grid = GridLayout(activity)
        grid.columnCount = 2
        modules().forEach { module ->
            val tile = moduleListTile(module)
            tile.layoutParams = gridParams()
            grid.addView(tile)
        }
        return grid
    }

    private fun appTopBar(): LinearLayout {
        val row = Ui.row(activity)
        row.setPadding(2.dp(), 4.dp(), 2.dp(), 4.dp())
        row.addView(Ui.iconBubble(activity, "✓", Ui.BLUE))
        val text = Ui.vbox(activity)
        text.setPadding(12.dp(), 0, 0, 0)
        text.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        text.addView(Ui.title(activity, "SafeField", 26f))
        text.addView(Ui.label(activity, "Segurança do Trabalho em Campo"))
        row.addView(text)
        row.addView(Ui.chip(activity, "2", Ui.RED))
        row.addView(Ui.iconBubble(activity, "U", Ui.PURPLE), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(8.dp(), 0, 0, 0) })
        return row
    }

    private fun tabBar(): LinearLayout {
        val card = Ui.card(activity)
        card.setPadding(10.dp(), 10.dp(), 10.dp(), 10.dp())
        val row = Ui.row(activity)
        listOf(
            "Dashboard" to { openModules() },
            "Riscos" to { openPt() },
            "Inspeções" to { openInspection() },
            "PT" to { openPt() },
            "Mais" to { showMenuDialog() }
        ).forEachIndexed { index, item ->
            val btn = if (index == 0) Ui.button(activity, item.first, Ui.BLUE) else Ui.ghostButton(activity, item.first)
            btn.textSize = 12f
            btn.setOnClickListener { item.second.invoke() }
            row.addView(btn, LinearLayout.LayoutParams(0, 44.dp(), 1f).apply { if (index > 0) setMargins(6.dp(), 0, 0, 0) })
        }
        card.addView(row)
        return card
    }

    private fun greeting(): LinearLayout {
        val box = Ui.vbox(activity)
        box.addView(Ui.value(activity, "Olá, Técnico de Segurança", Ui.TEXT))
        box.addView(Ui.label(activity, "Bem-vindo ao SafeField"), smallTop())
        return box
    }

    private fun summaryCards(flow: PtFlowState): LinearLayout {
        val card = Ui.card(activity)
        val top = Ui.row(activity)
        top.addView(Ui.section(activity, "Resumo geral"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(Ui.label(activity, "Últimos 30 dias"))
        card.addView(top)
        val grid = GridLayout(activity)
        grid.columnCount = 2
        val pending = flow.critical.size + flow.important.size
        listOf(
            StatItem("Ocorrências", "0", "+0%", Ui.AMBER),
            StatItem("Inspeções", "0", "+0%", Ui.BLUE),
            StatItem("PT Emitidas", data.history.size.toString(), "+0%", Ui.GREEN),
            StatItem("Pendências", pending.toString(), if (pending > 0) "+${pending}" else "0", if (pending > 0) Ui.RED else Ui.GREEN)
        ).forEach { item ->
            val stat = statTile(item)
            stat.layoutParams = gridParams()
            grid.addView(stat)
        }
        card.addView(grid.margin(0, 8.dp()))
        return card
    }

    private fun quickActions(): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Ações rápidas"))
        val row = Ui.row(activity)
        row.addView(actionButton("Inspeção", "Registrar achado", Ui.BLUE) { openInspection() }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(actionButton("Permissão de Trabalho", "Solicitar / Gerenciar", Ui.BLUE) { openPt() }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(8.dp(), 0, 0, 0) })
        card.addView(row, buttonLp())
        return card
    }

    private fun moduleStrip(): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Módulos"))
        val grid = GridLayout(activity)
        grid.columnCount = 5
        modules().drop(2).take(5).forEach { module ->
            val tile = miniModule(module)
            tile.layoutParams = miniGridParams()
            grid.addView(tile)
        }
        card.addView(grid.margin(0, 8.dp()))
        return card
    }

    private fun recentCard(flow: PtFlowState): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Recentes"))
        if (data.history.isEmpty()) {
            card.addView(Ui.label(activity, "Nenhuma PT emitida ainda. Use Inspeção ou Permissão de Trabalho para começar."), smallTop())
        } else {
            data.history.take(4).forEach { item ->
                card.addView(recentRow(item.ptNumber.ifBlank { "Permissão de Trabalho" }, item.place.ifBlank { "Sem local" }, historyState(item), historyColor(historyState(item))), buttonLp())
            }
        }
        return card
    }

    private fun actionButton(title: String, subtitle: String, color: Int, action: () -> Unit): LinearLayout {
        val box = Ui.vbox(activity, 12.dp())
        box.background = Ui.ripple(color, 16.dp())
        box.addView(Ui.value(activity, title, Color.WHITE))
        box.addView(TextView(activity).apply { text = subtitle; textSize = 11f; setTextColor(Color.WHITE); gravity = Gravity.START }, smallTop())
        box.setOnClickListener { action() }
        return box
    }

    private fun statTile(item: StatItem): LinearLayout {
        val box = Ui.card(activity)
        box.setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
        box.addView(Ui.iconBubble(activity, item.title.take(1), item.color))
        box.addView(Ui.title(activity, item.value, 22f), smallTop())
        box.addView(Ui.label(activity, item.title), smallTop())
        box.addView(Ui.label(activity, item.delta), smallTop())
        return box
    }

    private fun miniModule(module: ModuleItem): LinearLayout {
        val box = Ui.vbox(activity, 8.dp())
        box.gravity = Gravity.CENTER
        box.background = Ui.ripple(Color.WHITE, 16.dp(), Ui.BORDER, 1)
        box.addView(Ui.iconBubble(activity, module.initials, module.color))
        val text = TextView(activity)
        text.text = module.title
        text.textSize = 10.5f
        text.gravity = Gravity.CENTER
        text.setTextColor(Ui.TEXT)
        box.addView(text, smallTop())
        box.setOnClickListener { module.action.invoke() }
        return box
    }

    private fun moduleListTile(module: ModuleItem): LinearLayout {
        val box = Ui.card(activity)
        val row = Ui.row(activity)
        row.addView(Ui.iconBubble(activity, module.initials, module.color))
        val texts = Ui.vbox(activity)
        texts.setPadding(10.dp(), 0, 0, 0)
        texts.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        texts.addView(Ui.value(activity, module.title, Ui.TEXT))
        texts.addView(Ui.label(activity, module.subtitle), smallTop())
        row.addView(texts)
        box.addView(row)
        box.setOnClickListener { module.action.invoke() }
        return box
    }

    private fun recentRow(title: String, subtitle: String, status: String, color: Int): LinearLayout {
        val row = Ui.row(activity)
        row.background = Ui.bg(0xFFF8FAFC.toInt(), 16.dp(), Ui.BORDER, 1)
        row.setPadding(10.dp(), 10.dp(), 10.dp(), 10.dp())
        row.addView(Ui.iconBubble(activity, "PT", Ui.BLUE))
        val texts = Ui.vbox(activity)
        texts.setPadding(10.dp(), 0, 0, 0)
        texts.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        texts.addView(Ui.value(activity, title, Ui.TEXT))
        texts.addView(Ui.label(activity, subtitle), smallTop())
        row.addView(texts)
        row.addView(Ui.chip(activity, status, color))
        return row
    }

    private fun showMenuDialog(): Unit {
        val dialog = AlertDialog.Builder(activity).create()
        val panel = Ui.vbox(activity, 16.dp())
        panel.background = Ui.bg(Color.WHITE, 24.dp(), Ui.BORDER, 1)
        panel.addView(Ui.title(activity, "Todos os módulos", 22f))
        modules().forEach { module ->
            panel.addView(moduleListTile(module).margin(0, 6.dp()))
        }
        dialog.setView(panel)
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun modules(): List<ModuleItem> = listOf(
        ModuleItem("Inspeção", "Achados e relatório", "IN", Ui.BLUE) { openInspection() },
        ModuleItem("PT", "Permissão de Trabalho", "PT", Ui.BLUE) { openPt() },
        ModuleItem("DDS", "Diálogo de segurança", "DDS", Ui.GREEN) { openPlaceholder("DDS") },
        ModuleItem("EPI", "Entrega e controle", "EPI", Ui.AMBER) { openPlaceholder("EPI") },
        ModuleItem("Ocorrência", "Incidentes e desvios", "OC", Ui.RED) { openPlaceholder("Ocorrência") },
        ModuleItem("Colaboradores", "Equipe e documentos", "CL", Ui.PURPLE) { openPlaceholder("Colaboradores") },
        ModuleItem("Relatórios", "Exportação de dados", "REL", Ui.BLUE) { openPlaceholder("Relatórios") },
        ModuleItem("Dashboard", "Indicadores gerais", "DB", Ui.GREEN) { openPlaceholder("Dashboard") }
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

    private fun openInspection(): Unit {
        InspectionModule(activity) { openModules() }.show()
    }

    private fun gridParams(): GridLayout.LayoutParams = GridLayout.LayoutParams().apply {
        width = 0
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        setMargins(5.dp(), 5.dp(), 5.dp(), 5.dp())
    }

    private fun miniGridParams(): GridLayout.LayoutParams = GridLayout.LayoutParams().apply {
        width = 0
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        setMargins(4.dp(), 4.dp(), 4.dp(), 4.dp())
    }

    private fun buttonLp(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 10.dp(), 0, 0) }
    private fun smallTop(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 4.dp(), 0, 0) }
    private fun Int.dp(): Int = Ui.dp(activity, this)
}
