package com.safefield.app

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView

internal class PtCentralDashboard(
    private val activity: MainActivity,
    private val data: PtData,
    private val openData: () -> Unit,
    private val openRisks: () -> Unit,
    private val openChecklist: () -> Unit,
    private val openTeam: () -> Unit,
    private val openReview: () -> Unit,
    private val clearDraft: () -> Unit
) {
    fun renderInto(container: LinearLayout): Unit {
        val flow = PtFlowEngine.flow(data)
        container.addView(operationPanel(flow).margin(0, 8.dp()))
        container.addView(primaryAction(flow).margin(0, 8.dp()))
        container.addView(pendingBlocks(flow).margin(0, 8.dp()))
        container.addView(flowMap(flow).margin(0, 8.dp()))
        container.addView(activitySummary(flow).margin(0, 8.dp()))
        container.addView(historyBlock().margin(0, 8.dp()))
        val clear = Ui.ghostButton(activity, "Limpar rascunho")
        clear.setOnClickListener { clearDraft() }
        container.addView(clear.margin(0, 8.dp()))
    }

    private fun operationPanel(flow: PtFlowState): LinearLayout {
        val card = Ui.heroCard(activity)
        val statusColor = statusColor(flow.status)
        val top = Ui.row(activity)
        val titleBox = Ui.vbox(activity)
        titleBox.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        titleBox.addView(Ui.label(activity, "Permissão de Trabalho"))
        titleBox.addView(Ui.title(activity, flow.number, 24f))
        top.addView(titleBox)
        top.addView(Ui.chip(activity, flow.status.name, statusColor))
        card.addView(top)

        val headline = Ui.title(activity, statusMessage(flow.status), 22f)
        headline.setTextColor(statusColor)
        card.addView(headline.margin(0, 10.dp()))
        card.addView(Ui.value(activity, flow.validityLabel, if (flow.status == PtStatus.EXPIRADA) Ui.RED else Ui.TEXT).margin(0, 3.dp()))
        flow.validityAlert?.let { card.addView(Ui.value(activity, it, Ui.AMBER_SOFT).margin(0, 3.dp())) }
        card.addView(Ui.divider(activity).margin(0, 12.dp()))

        val grid = GridLayout(activity)
        grid.columnCount = 2
        listOf(
            "Empresa" to data.company.ifBlank { "pendente" },
            "Local" to data.place.ifBlank { "pendente" },
            "Responsável" to data.responsible.ifBlank { "pendente" },
            "Atividade" to activityMain(),
            "Início" to Ui.fmt(data.startMillis),
            "Término" to Ui.fmt(data.endMillis)
        ).forEach { item ->
            val tile = infoTile(item.first, item.second)
            tile.layoutParams = gridParams()
            grid.addView(tile)
        }
        card.addView(grid.margin(0, 2.dp()))
        return card
    }

    private fun primaryAction(flow: PtFlowState): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Próxima ação"))
        card.addView(Ui.value(activity, nextActionHint(flow), Ui.TEXT).margin(0, 8.dp()))
        val color = if (flow.canEmit) Ui.GREEN else if (flow.status == PtStatus.EXPIRADA) Ui.RED else Ui.AMBER
        val button = Ui.button(activity, flow.primaryAction, color)
        button.setOnClickListener { openTarget(flow.nextTarget) }
        card.addView(button.margin(0, 8.dp()))
        return card
    }

    private fun pendingBlocks(flow: PtFlowState): LinearLayout {
        val box = Ui.vbox(activity)
        if (flow.critical.isEmpty() && flow.important.isEmpty()) {
            val ok = Ui.card(activity)
            ok.addView(Ui.chip(activity, "LIBERADO", Ui.GREEN))
            ok.addView(Ui.title(activity, "Tudo pronto para emissão", 20f).margin(0, 8.dp()))
            ok.addView(Ui.label(activity, "Nenhuma pendência impede a emissão da PT."))
            box.addView(ok)
            return box
        }
        if (flow.critical.isNotEmpty()) {
            box.addView(pendingGroup("Pendências críticas", "Bloqueiam a emissão da PT", flow.critical, Ui.RED).margin(0, 6.dp()))
        }
        if (flow.important.isNotEmpty()) {
            box.addView(pendingGroup("Pendências importantes", "Orientam o preenchimento e evidências", flow.important, Ui.AMBER).margin(0, 6.dp()))
        }
        return box
    }

    private fun pendingGroup(title: String, subtitle: String, items: List<PendingItem>, color: Int): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.chip(activity, title.uppercase(), color))
        card.addView(Ui.label(activity, subtitle).margin(0, 8.dp()))
        items.forEach { item ->
            val row = Ui.row(activity)
            val dot = TextView(activity)
            dot.text = "!"
            dot.gravity = Gravity.CENTER
            dot.setTextColor(color)
            dot.background = Ui.bg(0x0012161E, 999.dp(), color, 1)
            row.addView(dot, LinearLayout.LayoutParams(28.dp(), 28.dp()))
            val message = Ui.value(activity, item.message, Ui.TEXT)
            message.setPadding(10.dp(), 0, 0, 0)
            message.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            row.addView(message)
            row.setOnClickListener { openTarget(item.target) }
            card.addView(row.margin(0, 5.dp()))
        }
        return card
    }

    private fun flowMap(flow: PtFlowState): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Fluxo da liberação"))
        card.addView(Ui.label(activity, "Siga a etapa indicada até a PT ficar liberada para emissão.").margin(0, 6.dp()))
        card.addView(Ui.progress(activity, doneCount(flow), 5).margin(0, 8.dp()))
        card.addView(stepCard("01", "Dados", stepStatus(PtTarget.DADOS, flow), stepHint(PtTarget.DADOS, flow), openData).margin(0, 5.dp()))
        card.addView(stepCard("02", "Riscos", stepStatus(PtTarget.RISCOS, flow), stepHint(PtTarget.RISCOS, flow), openRisks).margin(0, 5.dp()))
        card.addView(stepCard("03", "Checklist", stepStatus(PtTarget.CHECKLIST, flow), stepHint(PtTarget.CHECKLIST, flow), openChecklist).margin(0, 5.dp()))
        card.addView(stepCard("04", "Equipe", stepStatus(PtTarget.EQUIPE, flow), stepHint(PtTarget.EQUIPE, flow), openTeam).margin(0, 5.dp()))
        card.addView(stepCard("05", "Revisão", stepStatus(PtTarget.REVISAO, flow), stepHint(PtTarget.REVISAO, flow), openReview).margin(0, 5.dp()))
        return card
    }

    private fun stepCard(number: String, title: String, status: StepState, hint: String, action: () -> Unit): LinearLayout {
        val card = Ui.card(activity)
        card.setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
        val row = Ui.row(activity)
        val badge = TextView(activity)
        badge.text = number
        badge.gravity = Gravity.CENTER
        badge.textSize = 14f
        badge.setTextColor(status.color)
        badge.background = Ui.bg(Ui.PANEL, 16.dp(), status.color, 1)
        row.addView(badge, LinearLayout.LayoutParams(46.dp(), 46.dp()))
        val texts = Ui.vbox(activity)
        texts.setPadding(12.dp(), 0, 0, 0)
        texts.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        texts.addView(Ui.value(activity, title, Ui.TEXT))
        texts.addView(Ui.label(activity, hint))
        row.addView(texts)
        row.addView(Ui.chip(activity, status.label, status.color))
        card.addView(row)
        card.setOnClickListener { action() }
        return card
    }

    private fun activitySummary(flow: PtFlowState): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Resumo da atividade"))
        card.addView(summaryLine("O que será feito", data.description.ifBlank { "pendente" }).margin(0, 6.dp()))
        card.addView(summaryLine("Onde será feito", data.place.ifBlank { "pendente" }).margin(0, 4.dp()))
        card.addView(summaryLine("Quem libera", data.responsible.ifBlank { "pendente" }).margin(0, 4.dp()))
        card.addView(summaryLine("Quem executa", if (data.workers.isEmpty()) "pendente" else "${data.workers.size} trabalhador(es)").margin(0, 4.dp()))
        card.addView(summaryLine("Até quando vale", flow.validityLabel).margin(0, 4.dp()))
        return card
    }

    private fun historyBlock(): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Histórico"))
        val last = data.history.firstOrNull()
        if (last == null) {
            card.addView(Ui.label(activity, "Nenhuma PT emitida neste aparelho.").margin(0, 6.dp()))
        } else {
            card.addView(Ui.value(activity, last.emittedAt, Ui.AMBER_SOFT).margin(0, 6.dp()))
            card.addView(Ui.label(activity, last.place.ifBlank { "Sem local informado" }))
            card.addView(Ui.label(activity, last.fileName))
        }
        return card
    }

    private fun summaryLine(label: String, value: String): LinearLayout {
        val row = Ui.row(activity)
        val left = Ui.label(activity, label)
        left.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(left)
        val color = if (value == "pendente") Ui.RED else Ui.TEXT
        row.addView(Ui.value(activity, value, color))
        return row
    }

    private fun infoTile(label: String, value: String): LinearLayout {
        val tile = Ui.vbox(activity, 10.dp())
        tile.background = Ui.bg(Ui.SHELL, 14.dp(), Ui.BORDER, 1)
        tile.addView(Ui.label(activity, label))
        tile.addView(Ui.value(activity, value, if (value == "pendente") Ui.RED else Ui.TEXT))
        return tile
    }

    private fun openTarget(target: PtTarget): Unit {
        when (target) {
            PtTarget.DADOS, PtTarget.VALIDADE -> openData()
            PtTarget.RISCOS -> openRisks()
            PtTarget.CHECKLIST -> openChecklist()
            PtTarget.EQUIPE -> openTeam()
            PtTarget.REVISAO -> openReview()
        }
    }

    private fun statusMessage(status: PtStatus): String {
        return when (status) {
            PtStatus.RASCUNHO -> "PT EM RASCUNHO — CONTINUE O PREENCHIMENTO"
            PtStatus.BLOQUEADA -> "PT BLOQUEADA — NÃO EMITIR"
            PtStatus.LIBERADA -> "PT LIBERADA PARA EMISSÃO"
            PtStatus.EXPIRADA -> "PT EXPIRADA — RENOVE A VALIDADE"
        }
    }

    private fun nextActionHint(flow: PtFlowState): String {
        return when (flow.nextTarget) {
            PtTarget.DADOS -> "Complete empresa, local, responsável e descrição da atividade."
            PtTarget.RISCOS -> "Valide a atividade crítica e responda os controles de risco."
            PtTarget.CHECKLIST -> "Responda todos os itens e corrija qualquer item marcado como Não."
            PtTarget.EQUIPE -> "Adicione trabalhador, evidências e assinatura do responsável."
            PtTarget.VALIDADE -> "A PT venceu. Ajuste início, validade ou término antes de emitir."
            PtTarget.REVISAO -> "Revise os dados finais e gere o PDF da Permissão de Trabalho."
        }
    }

    private fun stepHint(target: PtTarget, flow: PtFlowState): String {
        val blocking = flow.allPending.firstOrNull { it.target == target }
        if (blocking != null) return blocking.message
        return when (target) {
            PtTarget.DADOS -> "Dados mínimos preenchidos"
            PtTarget.RISCOS -> "Riscos controlados"
            PtTarget.CHECKLIST -> "Checklist conforme"
            PtTarget.EQUIPE -> "Equipe e assinatura conforme"
            PtTarget.REVISAO -> if (flow.canEmit) "Pronto para emitir PDF" else "Corrija pendências antes de emitir"
            PtTarget.VALIDADE -> flow.validityLabel
        }
    }

    private fun stepStatus(target: PtTarget, flow: PtFlowState): StepState {
        val hasCritical = flow.critical.any { it.target == target }
        val hasImportant = flow.important.any { it.target == target }
        return when {
            hasCritical -> StepState("BLOQUEADO", Ui.RED)
            hasImportant -> StepState("ATENÇÃO", Ui.AMBER)
            else -> StepState("CONFORME", Ui.GREEN)
        }
    }

    private fun doneCount(flow: PtFlowState): Int {
        return listOf(PtTarget.DADOS, PtTarget.RISCOS, PtTarget.CHECKLIST, PtTarget.EQUIPE, PtTarget.REVISAO)
            .count { stepStatus(it, flow).label == "CONFORME" }
    }

    private fun activityMain(): String {
        return when {
            data.activities.isNotEmpty() -> data.activities.first()
            data.manualActivity.isNotBlank() -> data.manualActivity
            else -> "pendente"
        }
    }

    private fun statusColor(status: PtStatus): Int {
        return when (status) {
            PtStatus.LIBERADA -> Ui.GREEN
            PtStatus.BLOQUEADA, PtStatus.EXPIRADA -> Ui.RED
            PtStatus.RASCUNHO -> Ui.AMBER
        }
    }

    private fun gridParams(): GridLayout.LayoutParams {
        val params = GridLayout.LayoutParams()
        params.width = 0
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.setMargins(5.dp(), 5.dp(), 5.dp(), 5.dp())
        return params
    }

    private data class StepState(val label: String, val color: Int)
    private fun Int.dp(): Int = Ui.dp(activity, this)
}
