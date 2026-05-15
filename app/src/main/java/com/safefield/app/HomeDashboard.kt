package com.safefield.app

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
        container.addView(header().margin(0, 10.dp()))
        container.addView(mainPtCard(flow).margin(0, 8.dp()))
        container.addView(indicators(flow).margin(0, 6.dp()))
        container.addView(shortcuts(flow).margin(0, 8.dp()))
        container.addView(sectionTitle("Módulos").margin(0, 10.dp()))
        container.addView(moduleGrid(compact = true).margin(0, 2.dp()))
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

    private fun header(): LinearLayout {
        val hero = Ui.heroCard(activity)
        val accent = View(activity)
        accent.background = Ui.bg(Ui.AMBER, 999.dp())
        hero.addView(accent, LinearLayout.LayoutParams(72.dp(), 4.dp()).apply {
            setMargins(0, 0, 0, 14.dp())
        })
        val brand = Ui.title(activity, "SAFEFIELD", 30f)
        brand.setTextColor(Ui.AMBER_SOFT)
        hero.addView(brand)
        hero.addView(Ui.value(activity, "Segurança do Trabalho em Campo", Ui.TEXT))
        hero.addView(Ui.label(activity, "Dashboard operacional para emissão, controle e acompanhamento de PTs em campo."))
        return hero
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
        card.addView(sectionTitle("Atalhos"))
        val grid = GridLayout(activity)
        grid.columnCount = 2
        listOf(
            "Continuar PT" to View.OnClickListener { openPt() },
            "Ver pendências" to View.OnClickListener { openPending(flow) },
            "Histórico" to View.OnClickListener { openHistory() },
            "Módulos" to View.OnClickListener { openModules() }
        ).forEach { shortcut ->
            val button = Ui.ghostButton(activity, shortcut.first)
            button.setOnClickListener(shortcut.second)
            button.layoutParams = gridParams()
            grid.addView(button)
        }
        card.addView(grid.margin(0, 8.dp()))
        return card
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
