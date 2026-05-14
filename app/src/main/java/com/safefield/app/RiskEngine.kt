package com.safefield.app

object RiskEngine {
    val riskBank = mapOf(
        "Içamento de carga" to listOf("Raio de fogo/carga suspensa","Queda de objetos","Tombamento","Prensagem"),
        "Trabalho em altura" to listOf("Queda de pessoas","Queda de objetos","Acesso inseguro","Ancoragem inadequada"),
        "Escavação" to listOf("Desmoronamento","Interferência subterrânea","Queda de pessoas","Atropelamento"),
        "Trabalho a quente" to listOf("Incêndio/explosão","Queimadura","Projeção de partículas","Fumos metálicos"),
        "Eletricidade / LOTO" to listOf("Choque elétrico","Arco elétrico","Energia residual","Bloqueio inadequado"),
        "Montagem industrial" to listOf("Prensagem","Corte/perfuração","Queda de objetos","Ergonômicos"),
        "Espaço confinado" to listOf("Atmosfera perigosa","Deficiência de oxigênio","Resgate dificultado","Engolfamento"),
        "Movimentação de máquinas" to listOf("Atropelamento","Colisão","Zona cega","Tombamento"),
        "Produtos químicos" to listOf("Inalação","Contato com pele/olhos","Derramamento","Reação química")
    )
    val controls = mapOf(
        "Raio de fogo/carga suspensa" to listOf("Isolar área de içamento","Proibir permanência sob carga","Comunicação com operador e sinaleiro"),
        "Queda de objetos" to listOf("Isolamento inferior","Amarração de ferramentas","Capacete com jugular"),
        "Tombamento" to listOf("Verificar base/patolamento","Avaliar solo e nivelamento","Respeitar capacidade do equipamento"),
        "Prensagem" to listOf("Manter mãos fora da linha de fogo","Comunicação entre executantes","Usar luvas adequadas"),
        "Queda de pessoas" to listOf("Cinto tipo paraquedista","Ancoragem válida","Acesso seguro"),
        "Acesso inseguro" to listOf("Inspecionar escadas/andaimes","Manter circulação desobstruída","Bloquear acesso não autorizado"),
        "Ancoragem inadequada" to listOf("Verificar ponto de ancoragem","Usar talabarte adequado","Registrar liberação do acesso"),
        "Desmoronamento" to listOf("Taludamento ou escoramento","Afastar material da borda","Inspecionar solo"),
        "Interferência subterrânea" to listOf("Consultar interferências","Escavação manual próxima a redes","Sinalizar interferências"),
        "Atropelamento" to listOf("Sinalização de tráfego","Colete refletivo","Rotas separadas"),
        "Incêndio/explosão" to listOf("Remover inflamáveis","Extintor no local","Vigia de fogo"),
        "Queimadura" to listOf("Luvas de raspa","Proteção facial","Delimitar área quente"),
        "Projeção de partículas" to listOf("Óculos de segurança","Protetor facial","Anteparo físico"),
        "Fumos metálicos" to listOf("Ventilação/exaustão","Máscara adequada","Avaliar exposição"),
        "Choque elétrico" to listOf("Bloqueio e etiquetagem","Teste de ausência de tensão","Ferramenta isolada"),
        "Arco elétrico" to listOf("EPI NR-10 adequado","Distância segura","Painel bloqueado"),
        "Energia residual" to listOf("Aliviar energia residual","Teste após bloqueio","Controle de religamento"),
        "Bloqueio inadequado" to listOf("Cadeado individual","Etiqueta identificada","Lista de pontos bloqueados"),
        "Corte/perfuração" to listOf("Luvas adequadas","Ferramenta em bom estado","Manter proteção da ferramenta"),
        "Ergonômicos" to listOf("Revezamento","Postura adequada","Uso de apoio mecânico"),
        "Atmosfera perigosa" to listOf("Medição de atmosfera","Ventilação","Permissão específica de espaço confinado"),
        "Deficiência de oxigênio" to listOf("Medição de O2","Monitoramento contínuo","Plano de resgate"),
        "Resgate dificultado" to listOf("Equipe de resgate definida","Tripé ou sistema de resgate disponível","Comunicação permanente"),
        "Engolfamento" to listOf("Bloqueio de alimentação","Controle de material solto","Vigia externo")
    )
    fun analyzeManual(text: String): Set<String> { val t=text.lowercase(); val out=mutableSetOf<String>(); if(listOf("altura","andaime","escada").any{it in t}) out.addAll(listOf("Queda de pessoas","Queda de objetos")); if(listOf("solda","quente","corte").any{it in t}) out.addAll(listOf("Incêndio/explosão","Queimadura","Projeção de partículas")); if(listOf("escavação","vala").any{it in t}) out.addAll(listOf("Desmoronamento","Interferência subterrânea")); if(listOf("montagem","tubulação","caixa").any{it in t}) out.addAll(listOf("Prensagem","Corte/perfuração","Queda de objetos")); return out }
}
