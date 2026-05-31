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

    fun renderInto(container: LinearLayout): Unit {
        val flow = PtFlowEngine.flow(data)
        val row = Ui.row(activity)
        row.gravity = Gravity.TOP
        row.addView(sideMenu(), LinearLayout.LayoutParams(68.dp(), ViewGroup.LayoutParams.WRAP_CONTENT))

        val content = Ui.vbox(activity)
        content.setPadding(10.dp(), 0, 0, 0)
        content.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        content.addView(startCard().margin(0, 6.dp()))
        content.addView(mainActions().margin(0, 8.dp()))
        content.addView(otherModules().margin(0, 8.dp()))
        content.addView(simpleStatus(flow).margin(0, 8.dp()))
        row.addView(content)
        container.addView(row)
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

    private fun sideMenu(): LinearLayout {
        val rail = Ui.vbox(activity, 8.dp())
        rail.gravity = Gravity.CENTER_HORIZONTAL
        rail.background = Ui.bg(Ui.PANEL, 22.dp(), Ui.BORDER, 1)
        rail.addView(sideItem("IN", Ui.BLUE_DARK, "Inspeção") { openInspection() }, sideLp())
        rail.addView(sideItem("PT", Ui.AMBER, "PT") { openPt() }, sideLp())
        rail.addView(sideItem("DS", Ui.GREEN, "DDS") { openPlaceholder("DDS") }, sideLp())
        rail.addView(sideItem("EP", Ui.AMBER_SOFT, "EPI") { openPlaceholder("EPI") }, sideLp())
        rail.addView(sideItem("+", Ui.BORDER_SOFT, "Mais") { showMenuDialog() }, sideLp())
        return rail
    }

    private fun sideItem(initials: String, color: Int, label: String, action: () -> Unit): LinearLayout {
        val item = Ui.vbox(activity)
        item.gravity = Gravity.CENTER
        val icon = TextView(activity)
        icon.text = initials
        icon.textSize = 12f
        icon.gravity = Gravity.CENTER
        icon.setTextColor(Color.WHITE)
        icon.background = Ui.bg(color, 14.dp())
        item.addView(icon, LinearLayout.LayoutParams(42.dp(), 42.dp()))
        val txt = TextView(activity)
        txt.text = label
        txt.textSize = 9.5f
        txt.gravity = Gravity.CENTER
        txt.setTextColor(Ui.MUTED)
        item.addView(txt, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 4.dp(), 0, 0) })
        item.setOnClickListener { action() }
        return item
    }

    private fun startCard(): LinearLayout {
        val card = Ui.heroCard(activity)
        card.addView(Ui.section(activity, "Início"))
        card.addView(Ui.title(activity, "O que deseja fazer?", 22f), smallTop())
        card.addView(Ui.label(activity, "Use o menu lateral ou escolha uma ação principal abaixo."), smallTop())
        return card
    }

    private fun mainActions(): LinearLayout {
        val card = Ui.card(activity)
        card.addView(bigAction("Inspeção", "Registrar achados e gerar relatório", "IN", Ui.BLUE_DARK) { openInspection() })
        card.addView(bigAction("Permissão de Trabalho", "Emitir, assinar e encerrar PT", "PT", Ui.AMBER) { openPt() }, buttonLp())
        return card
    }

    private fun otherModules(): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Outros módulos"))
        val grid = GridLayout(activity)
        grid.columnCount = 2
        modules().drop(2).forEach { module ->
            val tile = smallModule(module)
            tile.layoutParams = gridParams()
            grid.addView(tile)
        }
        card.addView(grid.margin(0, 8.dp()))
        return card
    }

    private fun simpleStatus(flow: PtFlowState): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Resumo"))
        val pending = flow.critical.size + flow.important.size
        card.addView(statusRow("Pendências da PT", pending.toString(), if (pending > 0) Ui.AMBER else Ui.GREEN), smallTop())
        card.addView(statusRow("PT atual", flow.status.name, statusColor(flow.status)), smallTop())
        card.addView(statusRow("Histórico PT", "${data.history.size}", Ui.BLUE), smallTop())
        return card
    }

    private fun bigAction(title: String, subtitle: String, initials: String, color: Int, action: () -> Unit): LinearLayout {
        val box = Ui.heroCard(activity)
        val row = Ui.row(activity)
        row.addView(Ui.iconBubble(activity, initials, color))
        val texts = Ui.vbox(activity)
        texts.setPadding(12.dp(), 0, 0, 0)
        texts.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        texts.addView(Ui.title(activity, title, 19f))
        texts.addView(Ui.label(activity, subtitle), smallTop())
        row.addView(texts)
        box.addView(row)
        box.setOnClickListener { action() }
        return box
    }

    private fun smallModule(module: ModuleItem): LinearLayout {
        val box = Ui.card(activity)
        box.setPadding(10.dp(), 10.dp(), 10.dp(), 10.dp())
        box.gravity = Gravity.CENTER
        box.addView(Ui.iconBubble(activity, module.initials, module.color))
        val title = Ui.value(activity, module.title, Ui.TEXT)
        title.gravity = Gravity.CENTER
        box.addView(title, smallTop())
        box.setOnClickListener { module.action.invoke() }
        return box
    }

    private fun moduleTile(module: ModuleItem, compact: Boolean): LinearLayout {
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

    private fun statusRow(label: String, value: String, color: Int): LinearLayout {
        val row = Ui.row(activity)
        val left = Ui.label(activity, label)
        left.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(left)
        row.addView(Ui.chip(activity, value, color))
        return row
    }

    private fun showMenuDialog(): Unit {
        val dialog = AlertDialog.Builder(activity).create()
        val panel = Ui.vbox(activity, 16.dp())
        panel.background = Ui.bg(Ui.PANEL, 24.dp(), Ui.BORDER, 1)
        panel.addView(Ui.title(activity, "Todos os módulos", 22f))
        modules().forEach { module ->
            panel.addView(menuItem(module) {
                dialog.dismiss()
                module.action.invoke()
            }.margin(0, 6.dp()))
        }
        panel.addView(menuItem(ModuleItem("Sobre", "Informações do aplicativo", "SF", Ui.BLUE) { openPlaceholder("Sobre o SafeField") }) {
            dialog.dismiss()
            openPlaceholder("Sobre o SafeField")
        }.margin(0, 6.dp()))
        dialog.setView(panel)
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun menuItem(module: ModuleItem, action: () -> Unit): LinearLayout {
        val item = Ui.card(activity)
        val row = Ui.row(activity)
        row.addView(Ui.iconBubble(activity, module.initials, module.color))
        val texts = Ui.vbox(activity)
        texts.setPadding(12.dp(), 0, 0, 0)
        texts.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        texts.addView(Ui.value(activity, module.title, Ui.TEXT))
        texts.addView(Ui.label(activity, module.subtitle), smallTop())
        row.addView(texts)
        item.addView(row)
        item.setOnClickListener { action() }
        return item
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
        ModuleItem("Inspeção", "Achados e relatório", "IN", Ui.BLUE_DARK) { openInspection() },
        ModuleItem("PT", "Permissão de Trabalho", "PT", Ui.AMBER) { openPt() },
        ModuleItem("DDS", "Diálogo de segurança", "DS", Ui.GREEN) { openPlaceholder("DDS") },
        ModuleItem("EPI", "Entrega e controle", "EP", Ui.AMBER_SOFT) { openPlaceholder("EPI") },
        ModuleItem("Ocorrência", "Incidentes e desvios", "OC", Ui.RED) { openPlaceholder("Ocorrência") },
        ModuleItem("Colaboradores", "Equipe e documentos", "CL", Ui.BLUE) { openPlaceholder("Colaboradores") },
        ModuleItem("Dashboard", "Indicadores gerais", "DB", Ui.GREEN) { openPlaceholder("Dashboard") }
    )

    private fun statusColor(status: PtStatus): Int {
        return when (status) {
            PtStatus.LIBERADA -> Ui.GREEN
            PtStatus.BLOQUEADA, PtStatus.EXPIRADA -> Ui.RED
            PtStatus.RASCUNHO -> Ui.AMBER
        }
    }

    private fun openInspection(): Unit {
        InspectionModule(activity) { openModules() }.show()
    }

    private fun sideLp(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 4.dp(), 0, 12.dp()) }
    private fun buttonLp(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 12.dp(), 0, 0) }
    private fun smallTop(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 6.dp(), 0, 0) }
    private fun Int.dp(): Int = Ui.dp(activity, this)
}
