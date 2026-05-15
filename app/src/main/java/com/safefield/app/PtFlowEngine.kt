package com.safefield.app

import java.util.Calendar
import java.util.concurrent.TimeUnit

enum class PtStatus { RASCUNHO, BLOQUEADA, LIBERADA, EXPIRADA }
enum class PendingPriority { CRITICA, IMPORTANTE }
enum class PtTarget { DADOS, RISCOS, CHECKLIST, EQUIPE, REVISAO, VALIDADE }

data class PendingItem(
    val message: String,
    val priority: PendingPriority,
    val target: PtTarget
)

data class PtFlowState(
    val number: String,
    val status: PtStatus,
    val validityLabel: String,
    val validityAlert: String?,
    val critical: List<PendingItem>,
    val important: List<PendingItem>,
    val nextTarget: PtTarget,
    val primaryAction: String
) {
    val allPending: List<PendingItem> get() = critical + important
    val canEmit: Boolean get() = status == PtStatus.LIBERADA
}

object PtFlowEngine {
    fun flow(data: PtData, now: Long = System.currentTimeMillis()): PtFlowState {
        val critical = mutableListOf<PendingItem>()
        val important = mutableListOf<PendingItem>()

        val isDraft = data.company.isBlank() && data.place.isBlank() &&
            data.responsible.isBlank() && data.description.isBlank() &&
            data.activities.isEmpty() && data.manualActivity.isBlank()
        val isExpired = data.endMillis > 0L && now > data.endMillis
        val unsignedWorkers = data.workers.count { it.signatureB64.isBlank() }

        if (isExpired) critical.add(PendingItem("PT expirada — ajuste a validade", PendingPriority.CRITICA, PtTarget.VALIDADE))
        if (data.responsible.isBlank()) critical.add(PendingItem("Sem responsável/emissor definido", PendingPriority.CRITICA, PtTarget.DADOS))
        if (data.workers.isEmpty()) critical.add(PendingItem("Sem trabalhador informado", PendingPriority.CRITICA, PtTarget.EQUIPE))
        if (data.signatureB64.isBlank()) critical.add(PendingItem("Assinatura do responsável pendente", PendingPriority.CRITICA, PtTarget.EQUIPE))
        if (unsignedWorkers > 0) critical.add(PendingItem("$unsignedWorkers envolvido(s) sem assinatura", PendingPriority.CRITICA, PtTarget.EQUIPE))
        if (RiskEngine.checklistItems.any { data.checklist[it] == RiskEngine.NO }) critical.add(PendingItem("Checklist possui item marcado como Não", PendingPriority.CRITICA, PtTarget.CHECKLIST))

        val hasControlPending = RiskEngine.risksFor(data).any { risk ->
            RiskEngine.controls[risk].orEmpty().any { control ->
                data.controls[RiskEngine.controlKey(risk, control)].isNullOrBlank()
            }
        }
        if (hasControlPending) critical.add(PendingItem("Controles de risco sem resposta", PendingPriority.CRITICA, PtTarget.RISCOS))

        val missingFields = mutableListOf<String>()
        if (data.company.isBlank()) missingFields.add("empresa")
        if (data.place.isBlank()) missingFields.add("local")
        if (data.description.isBlank()) missingFields.add("descrição da atividade")
        if (missingFields.isNotEmpty()) important.add(PendingItem("Complete: ${missingFields.joinToString(", ")}", PendingPriority.IMPORTANTE, PtTarget.DADOS))
        if (data.activities.isEmpty() && data.manualActivity.isBlank()) important.add(PendingItem("Informe atividade crítica ou atividade manual", PendingPriority.IMPORTANTE, PtTarget.RISCOS))
        if (RiskEngine.checklistItems.any { data.checklist[it].isNullOrBlank() }) important.add(PendingItem("Checklist incompleto", PendingPriority.IMPORTANTE, PtTarget.CHECKLIST))
        if (data.photoUris.isEmpty()) important.add(PendingItem("Sem fotos/evidências anexadas", PendingPriority.IMPORTANTE, PtTarget.EQUIPE))

        val status = when {
            isExpired -> PtStatus.EXPIRADA
            critical.isNotEmpty() || important.isNotEmpty() -> if (isDraft) PtStatus.RASCUNHO else PtStatus.BLOQUEADA
            else -> PtStatus.LIBERADA
        }

        val next = (critical.firstOrNull() ?: important.firstOrNull())?.target ?: PtTarget.REVISAO
        return PtFlowState(
            number = ptNumber(data),
            status = status,
            validityLabel = validityLabel(data, now),
            validityAlert = validityAlert(data, now),
            critical = critical,
            important = important,
            nextTarget = next,
            primaryAction = primaryAction(next)
        )
    }

    fun ptNumber(data: PtData): String {
        val year = Calendar.getInstance().apply { timeInMillis = data.startMillis }.get(Calendar.YEAR)
        val seed = kotlin.math.abs((data.startMillis / 1000L).toInt()) % 10000
        return "PT-$year-${seed.toString().padStart(4, '0')}"
    }

    private fun primaryAction(target: PtTarget): String = when (target) {
        PtTarget.DADOS -> "Completar dados da PT"
        PtTarget.RISCOS -> "Validar riscos e controles"
        PtTarget.CHECKLIST -> "Responder checklist"
        PtTarget.EQUIPE -> "Completar equipe e assinaturas"
        PtTarget.VALIDADE -> "Renovar validade"
        PtTarget.REVISAO -> "Emitir PDF"
    }

    fun validityLabel(data: PtData, now: Long = System.currentTimeMillis()): String {
        if (data.endMillis <= 0L) return "Validade não definida"
        val diff = data.endMillis - now
        if (diff <= 0L) return "Validade expirada"
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        return "Validade restante: ${hours}h ${minutes}min"
    }

    fun validityAlert(data: PtData, now: Long = System.currentTimeMillis()): String? {
        if (data.endMillis <= 0L) return "Defina o término da PT"
        val diff = data.endMillis - now
        return when {
            diff <= 0L -> "PT expirada — renove a validade"
            diff < 60L * 60L * 1000L -> "Atenção: PT próxima do vencimento"
            else -> null
        }
    }
}
