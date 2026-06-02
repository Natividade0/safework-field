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
    private data class StatItem(val title: String, val value: String, val icon: String, val color: Int)

    fun renderInto(container: LinearLayout): Unit {
        container.addView(welcomeCard().margin(0, 8.dp()))
        container.addView(compactSummary().margin(0, 8.dp()))
        container.addView(mainModules().margin(0, 8.dp()))
        container.addView(recentCard().margin(0, 8.dp()))
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

    private fun hasStartedPt(): Boolean {
        return data.company.isNotBlank() || data.place.isNotBlank() || data.description.isNotBlank() || data.history.isNotEmpty()
    }

    private fun activePtPendingCount(): Int {
        if (!hasStartedPt()) return 0
        val flow = PtFlowEngine.flow(data)
        return flow.critical.size + flow.important.size
    }

    private fun welcomeCard(): LinearLayout {
        val pending = activePtPendingCount()
        val card = Ui.heroCard(activity)
        card.setPadding(0, 0, 0, 0)

        val main = Ui.row(activity)
        main.gravity = Gravity.CENTER_VERTICAL

        val menuRail = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = Ui.bg(Ui.BLUE, 24.dp(), Ui.BLUE, 0)
            setOnClickListener { showMenuDialog() }
        }
        val menuText = TextView(activity).apply {
            text = "Menu"
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        menuRail.addView(menuText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        main.addView(menuRail, LinearLayout.LayoutParams(76.dp(), ViewGroup.LayoutParams.MATCH_PARENT))

        val content = Ui.vbox(activity, 18.dp())
        content.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        content.addView(Ui.label(activity, "Boa rotina de campo"))
        content.addView(Ui.title(activity, "Técnico de Segurança", 23f))
        content.addView(Ui.label(activity, "Registre inspeções, PTs e pendências em poucos toques."), smallTop())
        val chips = Ui.row(activity)
        chips.addView(Ui.chip(activity, if (pending > 0) "$pending pendência(s)" else "Sem pendências", if (pending > 0) Ui.RED else Ui.GREEN))
        chips.addView(Ui.chip(activity, "${data.history.size} PT(s)", Ui.BLUE), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(8.dp(), 0, 0, 0) })
        content.addView(chips, buttonLp())
        main.addView(content)
        card.addView(main, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 168.dp()))
        return card
    }

    private fun compactSummary(): LinearLayout {
        val card = Ui.card(activity)
        val top = Ui.row(activity)
        top.addView(Ui.section(activity, "Resumo rápido"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(Ui.label(activity, "Hoje"))
        card.addView(top)
        val pending = activePtPendingCount()
        val grid = GridLayout(activity)
        grid.columnCount = 2
        listOf(
            StatItem("Pendências", pending.toString(), "!", if (pending > 0) Ui.RED else Ui.GREEN),
            StatItem("Inspeções", "0", "✓", Ui.BLUE),
            StatItem("PTs", data.history.size.toString(), "P", Ui.GREEN),
            StatItem("Ocorrências", "0", "!", Ui.AMBER)
        ).forEach { item ->
            grid.addView(statRow(item), gridParams())
        }
        card.addView(grid, buttonLp())
        return card
    }

    private fun mainModules(): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Módulos principais"))
        val grid = GridLayout(activity)
        grid.columnCount = 2
        modules().forEach { module -> grid.addView(moduleBigCard(module), gridParams()) }
        card.addView(grid, buttonLp())
        return card
    }

    private fun recentCard(): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Últimas atividades"))
        if (data.history.isEmpty()) {
            card.addView(activityRow("Nenhum registro recente", "Crie uma inspeção ou permissão de trabalho para começar.", "—", Ui.MUTED), buttonLp())
        } else {
            data.history.take(4).forEach { item ->
                val state = historyState(item)
                card.addView(activityRow(item.ptNumber.ifBlank { "Permissão de Trabalho" }, item.place.ifBlank { "Sem local" }, state, historyColor(state)), buttonLp())
            }
        }
        return card
    }

    private fun statRow(item: StatItem): LinearLayout {
        val row = Ui.row(activity)
        row.background = Ui.bg(0xFFF8FAFC.toInt(), 16.dp(), Ui.BORDER, 1)
        row.setPadding(10.dp(), 10.dp(), 10.dp(), 10.dp())
        row.addView(Ui.iconBubble(activity, item.icon, item.color), LinearLayout.LayoutParams(36.dp(), 36.dp()))
        val texts = Ui.vbox(activity)
        texts.setPadding(8.dp(), 0, 0, 0)
        texts.addView(Ui.title(activity, item.value, 19f))
        texts.addView(Ui.label(activity, item.title))
        row.addView(texts)
        return row
    }

    private fun moduleBigCard(module: ModuleItem): LinearLayout {
        val box = Ui.card(activity)
        box.setPadding(14.dp(), 14.dp(), 14.dp(), 14.dp())
        box.addView(Ui.iconBubble(activity, module.initials, module.color), LinearLayout.LayoutParams(48.dp(), 48.dp()))
        box.addView(Ui.value(activity, module.title, Ui.TEXT), buttonLp())
        box.addView(Ui.label(activity, module.subtitle), smallTop())
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

    private fun activityRow(title: String, subtitle: String, status: String, color: Int): LinearLayout {
        val row = Ui.row(activity)
        row.background = Ui.bg(0xFFF8FAFC.toInt(), 16.dp(), Ui.BORDER, 1)
        row.setPadding(10.dp(), 10.dp(), 10.dp(), 10.dp())
        row.addView(Ui.iconBubble(activity, "•", color), LinearLayout.LayoutParams(38.dp(), 38.dp()))
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
        panel.addView(Ui.title(activity, "Menu SafeField", 22f))
        modules().forEach { module -> panel.addView(moduleListTile(module).margin(0, 6.dp())) }
        dialog.setView(panel)
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun modules(): List<ModuleItem> = listOf(
        ModuleItem("Inspeções", "Achados e relatório", "IN", Ui.BLUE) { openInspection() },
        ModuleItem("Permissão de Trabalho", "Emitir, assinar e encerrar", "PT", Ui.BLUE) { openPt() },
        ModuleItem("EPI", "Entrega e controle", "EP", Ui.AMBER) { openPlaceholder("EPI") },
        ModuleItem("Ocorrências", "Incidentes e desvios", "OC", Ui.RED) { openPlaceholder("Ocorrência") },
        ModuleItem("Colaboradores", "Equipe e documentos", "CL", Ui.PURPLE) { openPlaceholder("Colaboradores") },
        ModuleItem("Relatórios", "Exportação de dados", "RE", Ui.GREEN) { openPlaceholder("Relatórios") },
        ModuleItem("DDS", "Diálogo de segurança", "DS", Ui.GREEN) { openPlaceholder("DDS") },
        ModuleItem("Dashboard", "Indicadores gerais", "DB", Ui.BLUE) { openPlaceholder("Dashboard") }
    )

    private fun historyState(item: PtHistoryItem): String = when {
        item.status == "ENCERRADA" -> "ENCERRADA"
        item.endMillis > 0L && System.currentTimeMillis() > item.endMillis -> "VENCIDA"
        else -> "VÁLIDA"
    }

    private fun historyColor(state: String): Int = when (state) {
        "ENCERRADA" -> Ui.MUTED
        "VENCIDA" -> Ui.RED
        else -> Ui.GREEN
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

    private fun buttonLp(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 10.dp(), 0, 0) }
    private fun smallTop(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 4.dp(), 0, 0) }
    private fun Int.dp(): Int = Ui.dp(activity, this)
}
