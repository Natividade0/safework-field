package com.safefield.app

object RiskEngine {
    const val YES = "Sim"
    const val NO = "Não"
    const val NA = "N/A"

    val activities = listOf("Içamento de carga", "Trabalho em altura", "Escavação", "Trabalho a quente", "Eletricidade / LOTO", "Montagem industrial", "Espaço confinado", "Movimentação de máquinas", "Produtos químicos")

    val checklistItems = listOf("APR válida emitida para esta atividade", "EPIs específicos inspecionados e aprovados", "Treinamento NR em dia para todos os envolvidos", "Área isolada e sinalizada adequadamente", "Responsável presente no local antes do início", "Ferramentas inspecionadas", "Condições climáticas avaliadas", "Comunicação de emergência definida", "Rotas de fuga livres", "Permissões complementares verificadas")

    private val activityRisks = mapOf(
        "Içamento de carga" to listOf("Raio de fogo/carga suspensa", "Queda de objetos", "Tombamento", "Prensagem"),
        "Trabalho em altura" to listOf("Queda de pessoas", "Queda de objetos", "Acesso inseguro", "Ancoragem inadequada"),
        "Escavação" to listOf("Desmoronamento", "Interferência subterrânea", "Queda de pessoas", "Atropelamento"),
        "Trabalho a quente" to listOf("Incêndio/explosão", "Queimadura", "Projeção de partículas", "Fumos metálicos"),
        "Eletricidade / LOTO" to listOf("Choque elétrico", "Arco elétrico", "Energia residual", "Bloqueio inadequado"),
        "Montagem industrial" to listOf("Prensagem", "Corte/perfuração", "Queda de objetos", "Ergonômicos"),
        "Espaço confinado" to listOf("Atmosfera perigosa", "Deficiência de oxigênio", "Resgate dificultado", "Engolfamento"),
        "Movimentação de máquinas" to listOf("Atropelamento", "Colisão", "Zona cega", "Tombamento"),
        "Produtos químicos" to listOf("Inalação", "Contato com pele/olhos", "Derramamento", "Reação química")
    )

    val controls = mapOf(
        "Raio de fogo/carga suspensa" to listOf("Isolar área de içamento", "Proibir permanência sob carga", "Comunicação com operador e sinaleiro"),
        "Queda de objetos" to listOf("Isolamento inferior", "Amarração de ferramentas", "Capacete com jugular"),
        "Tombamento" to listOf("Verificar base/patolamento", "Avaliar solo e nivelamento", "Respeitar capacidade do equipamento"),
        "Prensagem" to listOf("Manter mãos fora da linha de fogo", "Comunicação entre executantes", "Usar luvas adequadas"),
        "Queda de pessoas" to listOf("Cinto tipo paraquedista", "Ancoragem válida", "Acesso seguro"),
        "Acesso inseguro" to listOf("Inspecionar escadas/andaimes", "Manter circulação desobstruída", "Bloquear acesso não autorizado"),
        "Ancoragem inadequada" to listOf("Verificar ponto de ancoragem", "Usar talabarte adequado", "Registrar liberação do acesso"),
        "Desmoronamento" to listOf("Taludamento ou escoramento", "Afastar material da borda", "Inspecionar solo"),
        "Interferência subterrânea" to listOf("Consultar interferências", "Escavação manual próxima a redes", "Sinalizar interferências"),
        "Atropelamento" to listOf("Sinalização de tráfego", "Colete refletivo", "Rotas separadas"),
        "Incêndio/explosão" to listOf("Remover inflamáveis", "Extintor no local", "Vigia de fogo"),
        "Queimadura" to listOf("Luvas de raspa", "Proteção facial", "Delimitar área quente"),
        "Projeção de partículas" to listOf("Óculos de segurança", "Protetor facial", "Anteparo físico"),
        "Fumos metálicos" to listOf("Ventilação/exaustão", "Máscara adequada", "Avaliar exposição"),
        "Choque elétrico" to listOf("Bloqueio e etiquetagem", "Teste de ausência de tensão", "Ferramenta isolada"),
        "Arco elétrico" to listOf("EPI NR-10 adequado", "Distância segura", "Painel bloqueado"),
        "Energia residual" to listOf("Aliviar energia residual", "Teste após bloqueio", "Controle de religamento"),
        "Bloqueio inadequado" to listOf("Cadeado individual", "Etiqueta identificada", "Lista de pontos bloqueados"),
        "Corte/perfuração" to listOf("Luvas adequadas", "Ferramenta em bom estado", "Manter proteção da ferramenta"),
        "Ergonômicos" to listOf("Revezamento", "Postura adequada", "Uso de apoio mecânico"),
        "Atmosfera perigosa" to listOf("Medição de atmosfera", "Ventilação", "Permissão específica de espaço confinado"),
        "Deficiência de oxigênio" to listOf("Medição de O2", "Monitoramento contínuo", "Plano de resgate"),
        "Resgate dificultado" to listOf("Equipe de resgate definida", "Tripé ou sistema de resgate disponível", "Comunicação permanente"),
        "Engolfamento" to listOf("Bloqueio de alimentação", "Controle de material solto", "Vigia externo"),
        "Inalação" to listOf("FISPQ consultada", "Ventilação adequada", "Proteção respiratória"),
        "Contato com pele/olhos" to listOf("Luvas compatíveis", "Óculos ou protetor facial", "Lava-olhos disponível"),
        "Derramamento" to listOf("Kit de contenção disponível", "Área isolada", "Descarte adequado"),
        "Reação química" to listOf("Segregação de produtos incompatíveis", "FISPQ consultada", "Plano de emergência definido")
    )

    fun risksFor(data: PtData): List<String> {
        val result = linkedSetOf<String>()
        data.activities.forEach { result.addAll(activityRisks[it].orEmpty()) }
        val text = data.manualActivity.lowercase()
        if (listOf("altura", "andaime", "escada").any { text.contains(it) }) result.addAll(listOf("Queda de pessoas", "Queda de objetos"))
        if (listOf("solda", "quente", "corte").any { text.contains(it) }) result.addAll(listOf("Incêndio/explosão", "Queimadura", "Projeção de partículas"))
        if (listOf("escavação", "vala").any { text.contains(it) }) result.addAll(listOf("Desmoronamento", "Interferência subterrânea", "Atropelamento"))
        if (listOf("montagem", "tubulação", "caixa").any { text.contains(it) }) result.addAll(listOf("Prensagem", "Corte/perfuração", "Queda de objetos", "Ergonômicos"))
        return result.toList()
    }

    fun controlKey(risk: String, control: String): String = "$risk|$control"

    fun pending(data: PtData): List<String> {
        val p = mutableListOf<String>()
        if (data.company.isBlank()) p.add("Empresa / Planta pendente")
        if (data.place.isBlank()) p.add("Local da atividade pendente")
        if (data.responsible.isBlank()) p.add("Responsável / Emissor pendente")
        if (data.startMillis <= 0L) p.add("Início da PT pendente")
        if (data.endMillis <= 0L) p.add("Término da PT pendente")
        if (data.description.isBlank()) p.add("Descrição detalhada da atividade pendente")
        if (data.activities.isEmpty() && data.manualActivity.isBlank()) p.add("Selecione uma atividade crítica ou informe análise manual")
        if (data.workers.isEmpty()) p.add("Ao menos um trabalhador deve ser adicionado")
        if (data.signatureB64.isBlank()) p.add("Assinatura do responsável pendente")
        checklistItems.forEach {
            val answer = data.checklist[it].orEmpty()
            if (answer.isBlank()) p.add("Checklist pendente: $it")
            if (answer == NO) p.add("Checklist não conforme: $it")
        }
        risksFor(data).forEach { risk ->
            controls[risk].orEmpty().forEach { control ->
                if (data.controls[controlKey(risk, control)].isNullOrBlank()) p.add("Controle pendente: $risk - $control")
            }
        }
        return p
    }

    fun isReleased(data: PtData): Boolean = pending(data).isEmpty()
}
