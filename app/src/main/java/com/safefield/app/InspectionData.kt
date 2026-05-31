package com.safefield.app

data class InspectionRecord(
    val id: Long = System.currentTimeMillis(),
    var category: String = "Outro",
    var location: String = "",
    var description: String = "",
    var risk: String = "",
    var recommendation: String = "",
    var responsible: String = "",
    var deadline: String = "",
    var priority: String = "Média",
    var status: String = "Aberto",
    var notes: String = "",
    var photos: MutableList<String> = mutableListOf()
)

data class InspectionHistoryItem(
    val number: String = "",
    val generatedAt: String = "",
    val company: String = "",
    val place: String = "",
    val inspector: String = "",
    val fileName: String = ""
)

data class InspectionData(
    var number: String = "",
    var company: String = "",
    var area: String = "",
    var place: String = "",
    var inspector: String = "",
    var environmentType: String = "Outro",
    var objective: String = "",
    var dateMillis: Long = System.currentTimeMillis(),
    var records: MutableList<InspectionRecord> = mutableListOf(),
    var inspectorSignatureB64: String = "",
    var history: MutableList<InspectionHistoryItem> = mutableListOf()
)
