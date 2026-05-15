package com.safefield.app

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
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

        val menu = Ui.ghostButton(activity, "☰")
        menu.textSize = 24f
        menu.layoutParams = LinearLayout.LayoutParams(52.dp(), 52.dp())
        menu.setOnClickListener { showMenuDialog() }
        top.addView(menu)

        val info = Ui.vbox(activity)
        info.setPadding(14.dp(), 0, 0, 0)
        info.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        info.addView(Ui.label(activity, "Painel de campo"))
        info.addView(Ui.value(activity, "Acompanhe a PT atual e acesse os módulos pelo menu.", Ui.TEXT))
        top.addView(info)

        top.addView(Ui.chip(activity, flow.status.name, statusColor(flow.status)))
        card.addView(top)
        return card
    }

    private fun mainPtCard(flow: PtFlowState): LinearLayout {
        val card = Ui.heroCard(activity)
        val statusColor = statusColor(flow.status)
        val top = Ui.row(activity)
        val titleBox = Ui.vbox(activity)
        titleBox.addView(Ui.label(activity, "Permissão de Trabalho"))
        titleBox.addView(Ui.title(activity, flow.number, 23f))
        titleBox.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        top.addView(titleBox)
        top.addView(Ui.chip(activity, flow.status.name, statusColor))
        card.addView(top)
        card.addView(Ui.value(activity, "Validade: ${flow.validityLabel}").margin(0, 8.dp()))
        flow.validityAlert?.let { alert ->
            card.addView(Ui.value(activity, alert, Ui.AMBER_SOFT).margin(0, 3.dp()))
        }
        card.addView(metricStrip(listOf(
            Indicator("Críticas", flow.critical.size.toString(), Ui.RED),
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
            Indicator("Pendências críticas", flow.critical.size.toString(), Ui.RED),
            Indicator("Pendências importantes", flow.important.size.toString(), Ui.AMBER),
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
        card.addView(sectionTitle("Dashboard da PT"))
        card.addView(summaryRow("Status da PT", flow.status.name, statusColor(flow.status)).margin(0, 8.dp()))
        card.addView(summaryRow("Validade", flow.validityLabel, Ui.TEXT).margin(0, 4.dp()))
        card.addView(summaryRow("Fotos", data.photoUris.size.toString(), Ui.AMBER_SOFT).margin(0, 4.dp()))
        card.addView(summaryRow("Trabalhadores", data.workers.size.toString(), Ui.GREEN).margin(0, 4.dp()))
        val last = data.history.firstOrNull()
        if (last != null) {
            card.addView(Ui.divider(activity).margin(0, 10.dp()))
            card.addView(Ui.label(activity, "Última emissão"))
            card.addView(Ui.value(activity, "${last.emittedAt} - ${last.place.ifBlank { "Sem local" }}", Ui.TEXT).margin(0, 4.dp()))
        }
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
            tile.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(4.dp(), 0, 4.dp(), 0)
            }
            row.addView(tile)
        }
        return row
    }

    private fun shortcuts(flow: PtFlowState): LinearLayout {
        val card = Ui.card(activity)
        card.addView(sectionTitle("Atalhos principais"))
        val grid = GridLayout(activity)
        grid.columnCount = 2
        listOf(
            "Continuar PT" to View.OnClickListener { openPt() },
            "Ver pendências" to View.OnClickListener { openPending(flow) },
            "Histórico" to View.OnClickListener { openHistory() },
            "Módulos" to View.OnClickListener { showMenuDialog() }
        ).forEach { shortcut ->
            val button = Ui.ghostButton(activity, shortcut.first)
            button.setOnClickListener(shortcut.second)
            button.layoutParams = gridParams()
            grid.addView(button)
        }
        card.addView(grid.margin(0, 8.dp()))
        return card
    }

    private fun showMenuDialog(): Unit {
        val dialog = AlertDialog.Builder(activity).create()
        val panel = Ui.vbox(activity, 16.dp())
        panel.background = Ui.bg(Ui.PANEL, 24.dp(), Ui.BORDER, 1)

        val top = Ui.row(activity)
        top.gravity = Gravity.CENTER_VERTICAL
        val menuTitle = Ui.vbox(activity)
        menuTitle.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        menuTitle.addView(Ui.chip(activity, "☰ MENU", Ui.AMBER))
        menuTitle.addView(Ui.title(activity, "Módulos", 22f).margin(0, 10.dp()))
        top.addView(menuTitle)
        panel.addView(top)

        panel.addView(Ui.label(activity, "Acesse os módulos de campo e áreas preparadas para expansão."))
        panel.addView(menuItem("Permissão de Trabalho", "Abrir fluxo completo", "PT") {
            dialog.dismiss()
            openPt()
        }.margin(0, 10.dp()))
        modules().filter { it != "Permissão de Trabalho" }.forEach { name ->
            panel.addView(menuItem(name, if (name == "Dashboard") "Resumo e indicadores" else "Em desenvolvimento", initial(name)) {
                dialog.dismiss()
                if (name == "Dashboard") openPlaceholder("Dashboard") else openPlaceholder(name)
            }.margin(0, 5.dp()))
        }
        panel.addView(menuItem("Configurações / Sobre", "Informações do aplicativo", "SF") {
            dialog.dismiss()
            openPlaceholder("Sobre o SafeField")
        }.margin(0, 5.dp()))

        dialog.setView(panel)
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun menuItem(title: String, subtitle: String, initials: String, action: () -> Unit): LinearLayout {
        val item = Ui.card(activity)
        item.setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
        val row = Ui.row(activity)
        row.addView(bubble(initials, if (title == "Permissão de Trabalho") Ui.AMBER else Ui.BORDER))
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
        val isPt = name == "Permissão de Trabalho"
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
        if (isPt && !compact) {
            val flow = PtFlowEngine.flow(data)
            card.addView(Ui.chip(activity, flow.status.name, statusColor(flow.status)).margin(0, 10.dp()))
        }
        card.setOnClickListener { if (isPt) openPt() else openPlaceholder(name) }
        return card
    }

    private fun sectionTitle(text: String): TextView {
        val title = Ui.section(activity, text)
        title.setTextColor(Ui.AMBER_SOFT)
        return title
    }

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

    private fun initial(name: String): String {
        return when (name) {
            "Permissão de Trabalho" -> "PT"
            "Inspeção" -> "IN"
            "Ocorrência" -> "OC"
            "Colaboradores" -> "CL"
            "Dashboard" -> "DB"
            else -> name.take(3).uppercase()
        }
    }

    private fun modules(): List<String> {
        return listOf("Permissão de Trabalho", "APR", "DDS", "EPI", "Inspeção", "Ocorrência", "Colaboradores", "Dashboard")
    }

    private fun Int.dp(): Int = Ui.dp(activity, this)
}
