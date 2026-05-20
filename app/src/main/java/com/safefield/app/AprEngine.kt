package com.safefield.app

import java.util.Calendar

object AprEngine {
    val activities: List<String> = listOf(
        "Trabalho em altura",
        "Trabalho a quente / solda",
        "Escavacao",
        "Eletricidade / LOTO",
        "Icamento de carga",
        "Espaco confinado",
        "Movimentacao de maquinas",
        "Produtos quimicos",
        "Montagem industrial"
    )

    fun generate(data: PtData): List<AprItem> {
        return generate(data.activities, data.description, data.manualActivity, data.tools)
    }

    fun generate(data: AprData): List<AprItem> {
        return generate(data.selectedActivities, data.activityDescription, data.manualActivity, data.tools)
    }

    fun generate(selected: Set<String>, description: String, manual: String, tools: String = ""): List<AprItem> {
        val detected = linkedSetOf<String>()
        selected.forEach { detected.add(normalizeActivity(it)) }
        val text = listOf(description, manual, tools).joinToString(" ").lowercase()
        if (hasAny(text, "altura", "andaime", "escada", "telhado")) detected.add(ALTURA)
        if (hasAny(text, "solda", "corte", "quente", "lixadeira", "macarico")) detected.add(SOLDA)
        if (hasAny(text, "escavacao", "escava", "vala", "solo")) detected.add(ESCAVACAO)
        if (hasAny(text, "eletrica", "eletrico", "energia", "painel", "loto", "bloqueio")) detected.add(ELETRICIDADE)
        if (hasAny(text, "guindaste", "icamento", "carga suspensa", "munck")) detected.add(ICAMENTO)
        if (hasAny(text, "espaco confinado", "tanque", "silo", "confinado")) detected.add(CONFINADO)
        if (hasAny(text, "produto quimico", "solvente", "tinta", "quimico")) detected.add(QUIMICOS)
        if (hasAny(text, "maquina", "empilhadeira", "movimentacao")) detected.add(MAQUINAS)
        if (hasAny(text, "montagem", "tubulacao", "estrutura", "caixa")) detected.add(MONTAGEM)

        val items = linkedMapOf<String, AprItem>()
        detected.forEach { activity ->
            templates(activity).forEach { item ->
                items["${item.activity}|${item.danger}|${item.risk}"] = item
            }
        }
        return items.values.toList()
    }

    fun aprNumber(seedMillis: Long = System.currentTimeMillis()): String {
        val year = Calendar.getInstance().apply { timeInMillis = seedMillis }.get(Calendar.YEAR)
        val seed = kotlin.math.abs((seedMillis / 1000L).toInt()) % 10000
        return "APR-$year-${seed.toString().padStart(4, '0')}"
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

    fun severityLabel(value: Int): String {
        return when {
            value >= 5 -> "Critica"
            value >= 4 -> "Alta"
            value >= 3 -> "Media"
            else -> "Baixa"
        }
    }

    fun probabilityLabel(value: Int): String {
        return when {
            value >= 4 -> "Alta"
            value >= 3 -> "Media"
            else -> "Baixa"
        }
    }

    private fun templates(activity: String): List<AprItem> {
        return when (activity) {
            ALTURA -> listOf(
                item(activity, "Acesso em nivel elevado", "Queda de pessoas", "Lesao grave ou fatal", "Usar sistema de protecao contra queda, ancoragem valida, acesso seguro e isolamento inferior", "Cinturao paraquedista, talabarte, capacete com jugular, trava-quedas", 4, 3),
                item(activity, "Ferramentas e materiais em altura", "Queda de objetos", "Atingimento de pessoas e danos materiais", "Amarrar ferramentas, organizar materiais e isolar area inferior", "Capacete com jugular, oculos, luvas", 4, 3)
            )
            SOLDA -> listOf(
                item(activity, "Fonte de ignicao", "Incendio/explosao", "Queimaduras, danos materiais ou fatalidade", "Remover inflamaveis, manter extintor no local, emitir permissao complementar e manter vigia de fogo", "Mascara de solda, luvas de raspa, avental, mangote, protecao respiratoria", 4, 3),
                item(activity, "Fumos, fagulhas e particulas", "Inalacao e projecao", "Irritacao, intoxicacao ou lesao ocular", "Garantir ventilacao, exaustao, anteparo fisico e protecao coletiva", "Respirador adequado, oculos, protetor facial, luvas", 4, 3)
            )
            ESCAVACAO -> listOf(
                item(activity, "Solo instavel", "Desmoronamento", "Soterramento ou lesao grave", "Escoramento/taludamento, afastar material da borda, inspecionar solo e controlar acesso", "Capacete, botina, luvas, colete refletivo", 4, 3),
                item(activity, "Interferencias subterraneas", "Rompimento de rede", "Choque, vazamento, explosao ou parada operacional", "Consultar interferencias, sinalizar redes e escavar manualmente proximo a utilidades", "Luvas, botina, capacete, oculos", 4, 2)
            )
            ELETRICIDADE -> listOf(
                item(activity, "Energia eletrica", "Choque eletrico/arco eletrico", "Queimadura grave ou fatalidade", "Bloqueio e etiquetagem, teste de ausencia de tensao, cadeado individual e ferramentas isoladas", "Luvas isolantes, vestimenta NR-10, protetor facial, calcado isolante", 4, 3),
                item(activity, "Energia residual", "Religamento ou descarga inesperada", "Lesao grave, queimadura ou fatalidade", "Aliviar energia residual, testar apos bloqueio e controlar religamento", "EPI NR-10, ferramenta isolada, protetor facial", 5, 2)
            )
            ICAMENTO -> listOf(
                item(activity, "Carga suspensa", "Raio de fogo", "Atingimento, prensagem ou fatalidade", "Isolar area, proibir permanencia sob carga, usar plano de rigging e sinaleiro", "Capacete com jugular, luvas, botina, colete refletivo", 5, 3),
                item(activity, "Patolamento e solo", "Tombamento do equipamento", "Danos severos, colapso da carga ou fatalidade", "Verificar base, patolamento, nivelamento, solo e capacidade do equipamento", "Capacete, colete refletivo, botina", 5, 2)
            )
            CONFINADO -> listOf(
                item(activity, "Atmosfera perigosa", "Deficiencia de oxigenio/intoxicacao", "Mal subito, asfixia ou fatalidade", "Medicao de atmosfera, ventilacao, vigia externo, plano de resgate e permissao especifica", "Detector multigas, cinto, sistema de resgate, respirador adequado", 5, 4),
                item(activity, "Acesso restrito", "Resgate dificultado", "Atraso no socorro e agravamento da emergencia", "Definir equipe de resgate, comunicacao permanente e sistema de retirada", "Cinto, sistema de resgate, radio, capacete", 5, 3)
            )
            MAQUINAS -> listOf(
                item(activity, "Equipamentos moveis", "Atropelamento/colisao", "Lesao grave ou fatalidade", "Separar rotas, sinalizar area, controlar velocidade e eliminar zona cega", "Colete refletivo, botina, capacete", 4, 3),
                item(activity, "Partes moveis", "Prensagem", "Lesoes em maos e membros", "Bloquear acesso, manter distancia segura e usar comunicacao entre executantes", "Luvas adequadas, oculos, botina", 4, 3)
            )
            QUIMICOS -> listOf(
                item(activity, "Produto quimico", "Inalacao/contato com pele e olhos", "Irritacao, intoxicacao ou queimadura quimica", "Consultar FISPQ, ventilar area, segregar produtos e manter lava-olhos/kit de emergencia", "Luvas compativeis, oculos ou protetor facial, respirador adequado", 4, 3),
                item(activity, "Derramamento", "Contaminacao e reacao quimica", "Dano ambiental, exposicao e emergencia", "Manter kit de contencao, isolar area e descartar residuos corretamente", "Luvas compativeis, avental, botas, oculos", 4, 2)
            )
            MONTAGEM -> listOf(
                item(activity, "Montagem e ajuste de pecas", "Prensagem/corte", "Lesoes em maos e membros", "Manter maos fora da linha de fogo, usar ferramenta adequada e comunicar manobras", "Luvas adequadas, oculos, botina", 4, 3),
                item(activity, "Movimentacao manual", "Esforco ergonomico", "Dor, fadiga e lesao muscular", "Usar apoio mecanico, revezamento e postura adequada", "Luvas, botina, apoio mecanico quando aplicavel", 3, 3)
            )
            else -> emptyList()
        }
    }

    private fun item(activity: String, danger: String, risk: String, consequence: String, control: String, epi: String, severity: Int, probability: Int): AprItem {
        return AprItem(activity, danger, risk, consequence, control, epi, severity, probability, classify(severity, probability), step = "Execucao da atividade")
    }

    private fun classify(severity: Int, probability: Int): String {
        val score = severity * probability
        return when {
            severity >= 5 && probability >= 3 -> "CRITICO"
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
            lower.contains("icamento") -> ICAMENTO
            lower.contains("confinado") -> CONFINADO
            lower.contains("maquina") || lower.contains("movimenta") -> MAQUINAS
            lower.contains("quim") -> QUIMICOS
            lower.contains("montagem") -> MONTAGEM
            else -> value
        }
    }

    private fun hasAny(text: String, vararg terms: String): Boolean = terms.any { text.contains(it) }

    private const val ALTURA = "Trabalho em altura"
    private const val SOLDA = "Trabalho a quente / solda"
    private const val ESCAVACAO = "Escavacao"
    private const val ELETRICIDADE = "Eletricidade / LOTO"
    private const val ICAMENTO = "Icamento de carga"
    private const val CONFINADO = "Espaco confinado"
    private const val MAQUINAS = "Movimentacao de maquinas"
    private const val QUIMICOS = "Produtos quimicos"
    private const val MONTAGEM = "Montagem industrial"
}
