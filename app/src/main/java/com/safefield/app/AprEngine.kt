package com.safefield.app

object AprEngine {
    fun generate(data: PtData): List<AprItem> {
        val detected = linkedSetOf<String>()
        data.activities.forEach { detected.add(normalizeActivity(it)) }

        val text = listOf(data.description, data.manualActivity, data.tools, data.products)
            .joinToString(" ")
            .lowercase()

        if (hasAny(text, "altura", "andaime", "escada", "telhado")) detected.add(ALTURA)
        if (hasAny(text, "solda", "quente", "corte", "maçarico", "macarico")) detected.add(SOLDA)
        if (hasAny(text, "escavação", "escavacao", "vala", "trincheira")) detected.add(ESCAVACAO)
        if (hasAny(text, "eletric", "loto", "energia", "painel", "bloqueio")) detected.add(ELETRICIDADE)
        if (hasAny(text, "içamento", "icamento", "guindaste", "munck", "carga suspensa")) detected.add(ICAMENTO)
        if (hasAny(text, "espaço confinado", "espaco confinado", "confinado", "tanque")) detected.add(CONFINADO)
        if (hasAny(text, "montagem", "tubulação", "tubulacao", "caixa")) detected.add(MONTAGEM)

        val items = linkedMapOf<String, AprItem>()
        detected.forEach { activity ->
            templates(activity).forEach { item ->
                items["${item.activity}|${item.risk}|${item.control}"] = item
            }
        }
        return items.values.toList()
    }

    fun statusColor(classification: String): Int {
        return when (classification.uppercase()) {
            "BAIXO" -> Ui.GREEN
            "MEDIO" -> Ui.AMBER
            "ALTO" -> Ui.RED
            "CRITICO" -> 0xFF9333EA.toInt()
            else -> Ui.MUTED
        }
    }

    private fun templates(activity: String): List<AprItem> {
        return when (activity) {
            ALTURA -> listOf(
                item(activity, "Trabalho em nivel elevado", "Queda de pessoas", "Lesao grave ou fatalidade", "Usar ancoragem valida, acesso seguro e isolamento inferior", "Cinturao, talabarte, capacete com jugular, trava-quedas", 5, 3),
                item(activity, "Materiais em altura", "Queda de objetos", "Atingimento de pessoas e danos", "Amarrar ferramentas, isolar area inferior e manter organizacao", "Capacete com jugular, oculos, luvas", 4, 3)
            )
            SOLDA -> listOf(
                item(activity, "Fonte de calor e fagulhas", "Incendio/explosao", "Queimaduras, danos e parada da atividade", "Remover inflamaveis, manter extintor e vigia de fogo", "Mascara de solda, avental, mangote, luvas de raspa", 5, 3),
                item(activity, "Fumos e particulas", "Inalacao e projecao", "Irritacao, intoxicacao ou lesao ocular", "Garantir ventilacao, anteparo fisico e protecao respiratoria", "Protecao respiratoria, oculos, protetor facial", 4, 3)
            )
            ESCAVACAO -> listOf(
                item(activity, "Solo instavel", "Desmoronamento", "Soterramento e fatalidade", "Aplicar taludamento ou escoramento e inspecionar solo", "Capacete, botina, colete refletivo", 5, 3),
                item(activity, "Redes enterradas e maquinas", "Interferencia subterranea e atropelamento", "Choque, vazamento ou atropelamento", "Consultar interferencias, sinalizar e separar rotas", "Colete refletivo, botina, luvas", 4, 3)
            )
            ELETRICIDADE -> listOf(
                item(activity, "Energia eletrica", "Choque eletrico", "Lesao grave ou fatalidade", "Bloquear, etiquetar e testar ausencia de tensao", "EPI NR-10, luvas isolantes, ferramenta isolada", 5, 3),
                item(activity, "Energia residual", "Arco eletrico ou religamento", "Queimaduras e danos severos", "Aliviar energia residual e controlar religamento", "Vestimenta anti-chama, protetor facial, balaclava", 5, 2)
            )
            ICAMENTO -> listOf(
                item(activity, "Carga suspensa", "Raio de fogo", "Atingimento, prensagem ou fatalidade", "Isolar area, proibir permanencia sob carga e usar sinaleiro", "Capacete com jugular, luvas, botina", 5, 3),
                item(activity, "Equipamento e solo", "Tombamento", "Danos graves e colapso da carga", "Verificar base, patolamento, solo e capacidade", "Colete refletivo, capacete, botina", 5, 2)
            )
            CONFINADO -> listOf(
                item(activity, "Atmosfera perigosa", "Deficiencia de oxigenio ou intoxicacao", "Asfixia, intoxicacao ou fatalidade", "Medir atmosfera, ventilar e monitorar continuamente", "Monitor multigas, protecao respiratoria, cinto de resgate", 5, 4),
                item(activity, "Acesso restrito", "Resgate dificultado", "Atraso no socorro e agravamento", "Definir equipe de resgate, comunicacao e sistema de retirada", "Tripé ou sistema de resgate, radio, capacete", 5, 3)
            )
            MONTAGEM -> listOf(
                item(activity, "Montagem e ajuste de pecas", "Prensagem e corte", "Lesoes em maos e membros", "Manter maos fora da linha de fogo e usar ferramenta adequada", "Luvas adequadas, oculos, botina", 4, 3),
                item(activity, "Movimentacao manual", "Esforco ergonomico", "Dor, fadiga e lesao muscular", "Usar apoio mecanico, revezamento e postura adequada", "Luvas, cinta se aplicavel, botina", 3, 3)
            )
            else -> emptyList()
        }
    }

    private fun item(
        activity: String,
        danger: String,
        risk: String,
        consequence: String,
        control: String,
        epi: String,
        severity: Int,
        probability: Int
    ): AprItem {
        return AprItem(activity, danger, risk, consequence, control, epi, severity, probability, classify(severity, probability))
    }

    private fun classify(severity: Int, probability: Int): String {
        val score = severity * probability
        return when {
            score <= 4 -> "BAIXO"
            score <= 9 -> "MEDIO"
            score <= 15 -> "ALTO"
            else -> "CRITICO"
        }
    }

    private fun normalizeActivity(value: String): String {
        val lower = value.lowercase()
        return when {
            lower.contains("altura") -> ALTURA
            lower.contains("quente") || lower.contains("solda") -> SOLDA
            lower.contains("escava") -> ESCAVACAO
            lower.contains("eletric") || lower.contains("loto") -> ELETRICIDADE
            lower.contains("içamento") || lower.contains("icamento") -> ICAMENTO
            lower.contains("confinado") -> CONFINADO
            lower.contains("montagem") -> MONTAGEM
            else -> value
        }
    }

    private fun hasAny(text: String, vararg terms: String): Boolean {
        return terms.any { text.contains(it) }
    }

    private const val ALTURA = "Trabalho em altura"
    private const val SOLDA = "Trabalho a quente"
    private const val ESCAVACAO = "Escavacao"
    private const val ELETRICIDADE = "Eletricidade / LOTO"
    private const val ICAMENTO = "Icamento de carga"
    private const val CONFINADO = "Espaco confinado"
    private const val MONTAGEM = "Montagem industrial"
}
