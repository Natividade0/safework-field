package com.safefield.app

import java.util.Calendar

object InspectionEngine {
    val categories: List<String> = listOf(
        "Condição insegura",
        "Ato inseguro",
        "EPI",
        "Máquinas/equipamentos",
        "Elétrica",
        "Incêndio",
        "Ordem e limpeza",
        "Produtos químicos",
        "Trabalho em altura",
        "Documentação",
        "Meio ambiente",
        "Melhoria",
        "Outro"
    )

    val priorities: List<String> = listOf("Baixa", "Média", "Alta", "Crítica")
    val statuses: List<String> = listOf("Aberto", "Cobrado", "Em andamento", "Resolvido", "Arquivado")

    fun number(seedMillis: Long = System.currentTimeMillis()): String {
        val year = Calendar.getInstance().apply { timeInMillis = seedMillis }.get(Calendar.YEAR)
        val seed = kotlin.math.abs((seedMillis / 1000L).toInt()) % 10000
        return "INSP-$year-${seed.toString().padStart(4, '0')}"
    }

    fun priorityColor(priority: String): Int {
        return when (priority) {
            "Baixa" -> Ui.GREEN
            "Média" -> Ui.AMBER
            "Alta" -> Ui.RED
            "Crítica" -> 0xFF9333EA.toInt()
            else -> Ui.MUTED
        }
    }

    fun statusColor(status: String): Int {
        return when (status) {
            "Resolvido", "Arquivado" -> Ui.GREEN
            "Cobrado", "Em andamento" -> Ui.AMBER
            "Aberto" -> Ui.RED
            else -> Ui.MUTED
        }
    }

    fun pending(data: InspectionData): List<String> {
        val list = mutableListOf<String>()
        if (data.company.isBlank()) list.add("Empresa pendente")
        if (data.place.isBlank()) list.add("Local pendente")
        if (data.inspector.isBlank()) list.add("Inspetor pendente")
        if (data.records.isEmpty()) list.add("Nenhum registro de campo adicionado")
        if (data.records.any { it.description.isBlank() }) list.add("Existe registro sem descrição")
        return list
    }

    fun summary(data: InspectionData): InspectionSummary {
        val total = data.records.size
        val open = data.records.count { it.status == "Aberto" || it.status == "Cobrado" || it.status == "Em andamento" }
        val resolved = data.records.count { it.status == "Resolvido" || it.status == "Arquivado" }
        val high = data.records.count { it.priority == "Alta" || it.priority == "Crítica" }
        val photos = data.records.sumOf { it.photos.size }
        return InspectionSummary(total, open, resolved, high, photos)
    }
}

data class InspectionSummary(
    val total: Int,
    val open: Int,
    val resolved: Int,
    val highCritical: Int,
    val photos: Int
)
