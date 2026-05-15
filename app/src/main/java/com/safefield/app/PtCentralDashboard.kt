package com.safefield.app

import android.view.Gravity
import android.view.ViewGroup
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
        container.addView(documentHeader(flow).margin(0, 8.dp()))
        container.addView(nextStepCard(flow).margin(0, 8.dp()))
        container.addView(sectionsCard(flow).margin(0, 8.dp()))
        container.addView(pendingSummary(flow).margin(0, 8.dp()))
        container.addView(historyCard().margin(0, 8.dp()))
        val clear = Ui.ghostButton(activity, "Limpar rascunho")
        clear.setOnClickListener { clearDraft() }
        container.addView(clear.margin(0, 8.dp()))
    }

    private fun documentHeader(flow: PtFlowState): LinearLayout {
        val card = Ui.heroCard(activity)
        val color = statusColor(flow.status)
        val top = Ui.row(activity)
        val numberBox = Ui.vbox(activity)
        numberBox.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        numberBox.addView(Ui.label(activity, "Permissão de Trabalho Digital"))
        numberBox.addView(Ui.title(activity, flow.number, 24f))
        top.addView(numberBox)
        top.addView(Ui.chip(activity, statusLabel(flow.status), color))
        card.addView(top)

        val message = Ui.title(activity, statusMessage(flow.status), 20f)
        message.setTextColor(color)
        card.addView(message.margin(0, 10.dp()))
        card.addView(Ui.value(activity, flow.validityLabel, if (flow.status == PtStatus.EXPIRADA) Ui.RED else Ui.TEXT).margin(0, 4.dp()))
        flow.validityAlert?.let { card.addView(Ui.value(activity, it, Ui.AMBER_SOFT).margin(0, 3.dp())) }
        card.addView(Ui.divider(activity).margin(0, 12.dp()))

        card.addView(infoLine("Empresa / Planta", data.company.ifBlank { "pendente" }))
        card.addView(infoLine("Local da atividade", data.place.ifBlank { "pendente" }))
        card.addView(infoLine("Responsável / Emissor", data.responsible.ifBlank { "pendente" }))
        card.addView(infoLine("Atividade principal", mainActivity()).margin(0, 0.dp()))
        card.addView(infoLine("Início", Ui.fmt(data.startMillis)))
        card.addView(infoLine("Fim", Ui.fmt(data.endMillis)))
        return card
    }

    private fun nextStepCard(flow: PtFlowState): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Próximo passo"))
        card.addView(Ui.value(activity, nextStepText(flow), Ui.TEXT).margin(0, 8.dp()))
        val color = if (flow.canEmit) Ui.GREEN else if (flow.status == PtStatus.EXPIRADA) Ui.RED else Ui.AMBER
        val button = Ui.button(activity, buttonText(flow), color)
        button.setOnClickListener { openTarget(flow.nextTarget) }
        card.addView(button.margin(0, 8.dp()))
        return card
    }

    private fun sectionsCard(flow: PtFlowState): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Seções da PT"))
        card.addView(Ui.label(activity, "Preencha como uma permissão digital: seção por seção, sem painel confuso.").margin(0, 6.dp()))
        card.addView(sectionRow("1", "Informações Gerais", sectionStatus(PtTarget.DADOS, flow), sectionHint(PtTarget.DADOS, flow), openData).margin(0, 6.dp()))
        card.addView(sectionRow("2", "Lista Geral de Verificação", sectionStatus(PtTarget.CHECKLIST, flow), sectionHint(PtTarget.CHECKLIST, flow), openChecklist).margin(0, 6.dp()))
        card.addView(sectionRow("3", "Riscos e Medidas de Controle", sectionStatus(PtTarget.RISCOS, flow), sectionHint(PtTarget.RISCOS, flow), openRisks).margin(0, 6.dp()))
        card.addView(sectionRow("4", "Emergência e Observações", sectionStatus(PtTarget.DADOS, flow), "Ponto de emergência, observações e condições do serviço", openData).margin(0, 6.dp()))
        card.addView(sectionRow("5", "Envolvidos e Assinaturas", sectionStatus(PtTarget.EQUIPE, flow), sectionHint(PtTarget.EQUIPE, flow), openTeam).margin(0, 6.dp()))
        card.addView(sectionRow("6", "Evidência Fotográfica", evidenceStatus(), evidenceHint(), openTeam).margin(0, 6.dp()))
        card.addView(sectionRow("7", "Revisar e Emitir", sectionStatus(PtTarget.REVISAO, flow), sectionHint(PtTarget.REVISAO, flow), openReview).margin(0, 6.dp()))
        return card
    }

    private fun sectionRow(number: String, title: String, status: SectionState, hint: String, action: () -> Unit): LinearLayout {
        val row = Ui.card(activity)
        row.setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
        val line = Ui.row(activity)
        val badge = TextView(activity)
        badge.text = number
        badge.gravity = Gravity.CENTER
        badge.textSize = 16f
        badge.setTextColor(status.color)
        badge.background = Ui.bg(Ui.PANEL, 14.dp(), status.color, 1)
        line.addView(badge, LinearLayout.LayoutParams(42.dp(), 42.dp()))

        val textBox = Ui.vbox(activity)
        textBox.setPadding(12.dp(), 0, 0, 0)
        textBox.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        textBox.addView(Ui.value(activity, title, Ui.TEXT))
        textBox.addView(Ui.label(activity, hint))
        line.addView(textBox)
        line.addView(Ui.chip(activity, status.label, status.color))
        row.addView(line)
        row.setOnClickListener { action() }
        return row
    }

    private fun pendingSummary(flow: PtFlowState): LinearLayout {
        val card = Ui.card(activity)
        if (flow.critical.isEmpty() && flow.important.isEmpty()) {
            card.addView(Ui.chip(activity, "LIBERADA", Ui.GREEN))
            card.addView(Ui.title(activity, "PT pronta para emissão", 20f).margin(0, 8.dp()))
            card.addView(Ui.label(activity, "Todas as seções obrigatórias foram preenchidas."))
            return card
        }
        card.addView(Ui.section(activity, "Pendências"))
        if (flow.critical.isNotEmpty()) {
            card.addView(Ui.chip(activity, "CRÍTICAS", Ui.RED).margin(0, 8.dp()))
            flow.critical.take(4).forEach { item ->
                card.addView(pendingLine(item.message, Ui.RED, item.target).margin(0, 4.dp()))
            }
        }
        if (flow.important.isNotEmpty()) {
            card.addView(Ui.chip(activity, "IMPORTANTES", Ui.AMBER).margin(0, 10.dp()))
            flow.important.take(4).forEach { item ->
                card.addView(pendingLine(item.message, Ui.AMBER, item.target).margin(0, 4.dp()))
            }
        }
        return card
    }

    private fun pendingLine(message: String, color: Int, target: PtTarget): LinearLayout {
        val row = Ui.row(activity)
        val mark = TextView(activity)
        mark.text = "!"
        mark.gravity = Gravity.CENTER
        mark.setTextColor(color)
        mark.background = Ui.bg(Ui.PANEL, 999.dp(), color, 1)
        row.addView(mark, LinearLayout.LayoutParams(28.dp(), 28.dp()))
        val text = Ui.value(activity, message, Ui.TEXT)
        text.setPadding(10.dp(), 0, 0, 0)
        text.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(text)
        row.setOnClickListener { openTarget(target) }
        return row
    }

    private fun historyCard(): LinearLayout {
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

    private fun infoLine(label: String, value: String): LinearLayout {
        val row = Ui.row(activity)
        val left = Ui.label(activity, label)
        left.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(left)
        row.addView(Ui.value(activity, value, if (value == "pendente") Ui.RED else Ui.TEXT))
        return row
    }

    private fun sectionStatus(target: PtTarget, flow: PtFlowState): SectionState {
        val hasCritical = flow.critical.any { it.target == target }
        val hasImportant = flow.important.any { it.target == target }
        return when {
            hasCritical -> SectionState("BLOQUEIA", Ui.RED)
            hasImportant -> SectionState("PENDENTE", Ui.AMBER)
            else -> SectionState("OK", Ui.GREEN)
        }
    }

    private fun sectionHint(target: PtTarget, flow: PtFlowState): String {
        val pending = flow.allPending.firstOrNull { it.target == target }
        if (pending != null) return pending.message
        return when (target) {
            PtTarget.DADOS -> "Empresa, local, responsável, descrição e validade"
            PtTarget.RISCOS -> "Riscos e controles respondidos"
            PtTarget.CHECKLIST -> "Lista geral respondida"
            PtTarget.EQUIPE -> "Equipe e assinatura registradas"
            PtTarget.REVISAO -> if (flow.canEmit) "Pronto para emitir PDF" else "Revise as pendências"
            PtTarget.VALIDADE -> flow.validityLabel
        }
    }

    private fun evidenceStatus(): SectionState {
        return if (data.photoUris.isEmpty()) SectionState("OPCIONAL", Ui.AMBER) else SectionState("OK", Ui.GREEN)
    }

    private fun evidenceHint(): String {
        return if (data.photoUris.isEmpty()) "Sem fotos anexadas" else "${data.photoUris.size} foto(s) anexada(s)"
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
            PtStatus.RASCUNHO -> "PT EM RASCUNHO"
            PtStatus.BLOQUEADA -> "PT BLOQUEADA — NÃO EMITIR"
            PtStatus.LIBERADA -> "PT LIBERADA PARA EMISSÃO"
            PtStatus.EXPIRADA -> "PT EXPIRADA — RENOVE A VALIDADE"
        }
    }

    private fun statusLabel(status: PtStatus): String {
        return when (status) {
            PtStatus.RASCUNHO -> "RASCUNHO"
            PtStatus.BLOQUEADA -> "BLOQUEADA"
            PtStatus.LIBERADA -> "LIBERADA"
            PtStatus.EXPIRADA -> "EXPIRADA"
        }
    }

    private fun buttonText(flow: PtFlowState): String {
        return if (flow.canEmit) "Revisar e emitir PT" else flow.primaryAction
    }

    private fun nextStepText(flow: PtFlowState): String {
        return when (flow.nextTarget) {
            PtTarget.DADOS -> "Comece pelas informações gerais da atividade."
            PtTarget.RISCOS -> "Confira os riscos e marque as medidas de controle."
            PtTarget.CHECKLIST -> "Responda a lista geral de verificação."
            PtTarget.EQUIPE -> "Registre envolvidos, assinaturas e evidências."
            PtTarget.VALIDADE -> "Ajuste a validade antes de continuar."
            PtTarget.REVISAO -> "Revise a PT e gere o PDF."
        }
    }

    private fun mainActivity(): String {
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

    private data class SectionState(val label: String, val color: Int)
    private fun Int.dp(): Int = Ui.dp(activity, this)
}
