package com.safefield.app

import java.util.Calendar

object InspectionEngine {
    val environments: List<String> = listOf(
        "Obra / canteiro",
        "Indústria / usina",
        "Manutenção",
        "Armazém / logística",
        "Área administrativa",
        "Área externa",
        "Outro"
    )

    val defaultCategories: List<String> = listOf(
        "Condição insegura",
        "Ato inseguro",
        "EPI",
        "Equipamento",
        "Elétrica",
        "Incêndio",
        "Ordem e limpeza",
        "Documentação",
        "Meio ambiente",
        "Melhoria",
        "Outro"
    )

    val categories: List<String> = defaultCategories

    fun categoriesFor(environment: String): List<String> {
        val list = when (environment) {
            "Obra / canteiro" -> listOf(
                "Trabalho em altura",
                "Andaimes e escadas",
                "Escavação",
                "Máquinas e equipamentos",
                "Içamento de carga",
                "Sinalização e isolamento",
                "Ordem e limpeza",
                "Instalações elétricas provisórias",
                "EPI",
                "Ferramentas",
                "Armazenamento de materiais",
                "Banheiros / vivência"
            )
            "Indústria / usina" -> listOf(
                "Máquinas e proteções",
                "Bloqueio e etiquetagem / LOTO",
                "Energia elétrica",
                "Produtos químicos",
                "Espaço confinado",
                "Trabalho a quente",
                "Ruído / calor / agentes físicos",
                "Rotas de fuga",
                "Combate a incêndio",
                "Ordem e limpeza",
                "EPI",
                "Sinalização"
            )
            "Manutenção" -> listOf(
                "Isolamento da área",
                "Ferramentas manuais",
                "Ferramentas elétricas",
                "Bloqueio de energia",
                "Trabalho em altura",
                "Trabalho a quente",
                "Partes móveis",
                "Organização da frente de serviço",
                "EPI",
                "Permissão de Trabalho",
                "Teste após manutenção"
            )
            "Armazém / logística" -> listOf(
                "Empilhadeiras",
                "Paleteiras",
                "Circulação de pedestres",
                "Corredores e rotas",
                "Armazenamento de materiais",
                "Empilhamento",
                "Docas",
                "Sinalização",
                "Ordem e limpeza",
                "EPI",
                "Iluminação"
            )
            "Área administrativa" -> listOf(
                "Ergonomia",
                "Instalações elétricas",
                "Rotas de fuga",
                "Extintores",
                "Organização",
                "Iluminação",
                "Climatização",
                "Piso / queda de mesmo nível",
                "Sinalização"
            )
            "Área externa" -> listOf(
                "Circulação de veículos",
                "Piso irregular",
                "Drenagem / acúmulo de água",
                "Iluminação externa",
                "Sinalização",
                "Animais peçonhentos",
                "Vegetação",
                "Cercamento / acesso",
                "Exposição ao sol / calor",
                "Ordem e limpeza"
            )
            else -> defaultCategories
        }
        return if (list.contains("Outro")) list else list + "Outro"
    }

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
        if (data.records.isEmpty()) list.add("Nenhum achado de campo adicionado")
        if (data.records.any { it.description.isBlank() }) list.add("Existe achado sem descrição")
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
