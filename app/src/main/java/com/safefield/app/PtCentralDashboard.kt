package com.safefield.app

import android.content.Intent
import android.view.ViewGroup
import android.widget.LinearLayout

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
        container.addView(closePtCard().margin(0, 8.dp()))
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
        numberBox.addView(Ui.label(activity, "Permissao de Trabalho Digital"))
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
        card.addView(infoLine("Responsavel / Emissor", data.responsible.ifBlank { "pendente" }))
        card.addView(infoLine("Atividade principal", mainActivity()).margin(0, 0.dp()))
        card.addView(infoLine("Inicio", Ui.fmt(data.startMillis)))
        card.addView(infoLine("Fim", Ui.fmt(data.endMillis)))
        return card
    }

    private fun nextStepCard(flow: PtFlowState): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Proximo passo"))
        card.addView(Ui.value(activity, nextStepText(flow), Ui.TEXT).margin(0, 8.dp()))
        val color = if (flow.canEmit) Ui.GREEN else if (flow.status == PtStatus.EXPIRADA) Ui.RED else Ui.AMBER
        val button = Ui.button(activity, buttonText(flow), color)
        button.setOnClickListener { openTarget(flow.nextTarget) }
        card.addView(button.margin(0, 8.dp()))
        return card
    }

    private fun sectionsCard(flow: PtFlowState): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Secoes da PT"))
        card.addView(Ui.label(activity, "Preencha como uma permissao digital: secao por secao, sem painel confuso.").margin(0, 6.dp()))
        card.addView(sectionRow("1", "Informacoes Gerais", sectionStatus(PtTarget.DADOS, flow), sectionHint(PtTarget.DADOS, flow), openData).margin(0, 6.dp()))
        card.addView(sectionRow("2", "Lista Geral de Verificacao", sectionStatus(PtTarget.CHECKLIST, flow), sectionHint(PtTarget.CHECKLIST, flow), openChecklist).margin(0, 6.dp()))
        card.addView(sectionRow("3", "Riscos e Medidas de Controle", sectionStatus(PtTarget.RISCOS, flow), sectionHint(PtTarget.RISCOS, flow), openRisks).margin(0, 6.dp()))
        card.addView(sectionRow("4", "Emergencia e Observacoes", sectionStatus(PtTarget.DADOS, flow), "Ponto de emergencia, observacoes e condicoes do servico", openData).margin(0, 6.dp()))
        card.addView(sectionRow("5", "Envolvidos e Assinaturas", sectionStatus(PtTarget.EQUIPE, flow), sectionHint(PtTarget.EQUIPE, flow), openTeam).margin(0, 6.dp()))
        card.addView(sectionRow("6", "Evidencia Fotografica", evidenceStatus(), evidenceHint(), openTeam).margin(0, 6.dp()))
        card.addView(sectionRow("7", "Revisar e Emitir", sectionStatus(PtTarget.REVISAO, flow), sectionHint(PtTarget.REVISAO, flow), openReview).margin(0, 6.dp()))
        return card
    }

    private fun closePtCard(): LinearLayout {
        val card = Ui.card(activity)
        val closedAt = data.closedAt.ifBlank { data.closureAt }
        val closed = closedAt.isNotBlank() && (data.closeSignatureB64.isNotBlank() || data.closureSignatureB64.isNotBlank())
        val expired = data.endMillis > 0L && System.currentTimeMillis() > data.endMillis
        card.addView(Ui.chip(activity, if (closed) "PT ENCERRADA" else "ENCERRAMENTO", if (closed) Ui.GREEN else if (expired) Ui.RED else Ui.AMBER))
        card.addView(Ui.title(activity, if (closed) "Encerramento registrado" else "Encerrar PT", 18f).margin(0, 8.dp()))
        card.addView(Ui.label(activity, if (closed) "Encerrada em $closedAt" else "Registre condicao final, fotos finais e assinatura do responsavel pelo encerramento."))
        if (expired && !closed) card.addView(Ui.value(activity, "PT vencida. Registre o encerramento com atencao.", Ui.RED).margin(0, 6.dp()))
        val button = Ui.button(activity, if (closed) "Ver encerramento" else "Encerrar PT", if (closed) Ui.GREEN else Ui.AMBER)
        button.setOnClickListener { activity.startActivity(Intent(activity, ClosePtActivity::class.java)) }
        card.addView(button.margin(0, 8.dp()))
        return card
    }

    private fun pendingSummary(flow: PtFlowState): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Pendencias"))
        if (flow.allPending.isEmpty()) {
            card.addView(Ui.value(activity, "Nenhuma pendencia bloqueante. PT pronta para emissao.", Ui.GREEN))
        } else {
            flow.critical.take(4).forEach { card.addView(Ui.value(activity, it.message, Ui.RED).margin(0, 4.dp())) }
            flow.important.take(4).forEach { card.addView(Ui.value(activity, it.message, Ui.AMBER).margin(0, 4.dp())) }
            if (flow.allPending.size > 8) card.addView(Ui.label(activity, "+ ${flow.allPending.size - 8} pendencias"))
        }
        return card
    }

    private fun historyCard(): LinearLayout {
        val card = Ui.card(activity)
        card.addView(Ui.section(activity, "Historico local"))
        if (data.history.isEmpty()) {
            card.addView(Ui.label(activity, "Nenhuma PT emitida neste aparelho."))
        } else {
            data.history.take(5).forEach { item ->
                val status = historyStatus(item)
                card.addView(Ui.value(activity, "${item.ptNumber.ifBlank { "PT" }} - $status", historyColor(status)).margin(0, 4.dp()))
                card.addView(Ui.label(activity, "${item.emittedAt.ifBlank { "sem data" }} - ${item.place.ifBlank { "sem local" }}"))
                if (item.closedAt.isNotBlank()) card.addView(Ui.label(activity, "Encerrada em: ${item.closedAt}"))
            }
        }
        return card
    }

    private fun sectionRow(number: String, title: String, state: SectionState, hint: String, action: () -> Unit): LinearLayout {
        val row = Ui.card(activity)
        val top = Ui.row(activity)
        top.addView(Ui.iconBubble(activity, number, state.color))
        val textBox = Ui.vbox(activity)
        textBox.setPadding(12.dp(), 0, 0, 0)
        textBox.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        textBox.addView(Ui.value(activity, title, Ui.TEXT))
        textBox.addView(Ui.label(activity, hint))
        top.addView(textBox)
        top.addView(Ui.chip(activity, state.label, state.color))
        row.addView(top)
        row.setOnClickListener { action() }
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

    private fun evidenceStatus(): SectionState = if (data.photoUris.isEmpty()) SectionState("PENDENTE", Ui.AMBER) else SectionState("OK", Ui.GREEN)
    private fun evidenceHint(): String = if (data.photoUris.isEmpty()) "Anexe fotos do local e das condicoes da atividade" else "${data.photoUris.size} foto(s) anexada(s)"

    private fun sectionHint(target: PtTarget, flow: PtFlowState): String {
        return (flow.critical + flow.important).firstOrNull { it.target == target }?.message ?: "Secao concluida"
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

    private fun nextStepText(flow: PtFlowState): String {
        if (flow.canEmit) return "A PT esta pronta para revisao e emissao do PDF."
        return flow.allPending.firstOrNull()?.message ?: "Continue preenchendo a PT."
    }

    private fun buttonText(flow: PtFlowState): String = if (flow.canEmit) "Revisar e emitir PT" else flow.primaryAction

    private fun statusMessage(status: PtStatus): String {
        return when (status) {
            PtStatus.RASCUNHO -> "PT EM RASCUNHO"
            PtStatus.BLOQUEADA -> "PT BLOQUEADA - NAO EMITIR"
            PtStatus.LIBERADA -> "PT LIBERADA PARA EMISSAO"
            PtStatus.EXPIRADA -> "PT EXPIRADA - RENOVE A VALIDADE"
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

    private fun statusColor(status: PtStatus): Int {
        return when (status) {
            PtStatus.LIBERADA -> Ui.GREEN
            PtStatus.BLOQUEADA, PtStatus.EXPIRADA -> Ui.RED
            PtStatus.RASCUNHO -> Ui.AMBER
        }
    }

    private fun historyStatus(item: PtHistoryItem): String {
        return when {
            item.status == "ENCERRADA" -> "ENCERRADA"
            item.endMillis > 0L && System.currentTimeMillis() > item.endMillis -> "VENCIDA"
            else -> item.status.ifBlank { "EMITIDA" }
        }
    }

    private fun historyColor(status: String): Int {
        return when (status) {
            "ENCERRADA" -> Ui.MUTED
            "VENCIDA" -> Ui.RED
            else -> Ui.GREEN
        }
    }

    private fun infoLine(label: String, value: String): LinearLayout {
        val row = Ui.row(activity)
        val l = Ui.label(activity, label)
        l.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(l)
        row.addView(Ui.value(activity, value, Ui.TEXT))
        return row
    }

    private fun mainActivity(): String {
        return data.activities.firstOrNull() ?: data.manualActivity.ifBlank { data.description.ifBlank { "pendente" } }
    }

    private data class SectionState(val label: String, val color: Int)
    private fun Int.dp(): Int = Ui.dp(activity, this)
}
