package com.safefield.app

data class Worker(val name: String, val role: String)

data class PtHistoryItem(val emittedAt: String, val place: String, val fileName: String, val status: String = "LIBERADA")

data class PtData(
    var company: String = "",
    var area: String = "",
    var place: String = "",
    var responsible: String = "",
    var startMillis: Long = System.currentTimeMillis(),
    var validityHours: Int = 8,
    var endMillis: Long = System.currentTimeMillis() + 8L * 60L * 60L * 1000L,
    var teamName: String = "",
    var description: String = "",
    var tools: String = "",
    var products: String = "",
    var manualActivity: String = "",
    var activities: MutableSet<String> = mutableSetOf(),
    var checklist: MutableMap<String, String> = mutableMapOf(),
    var controls: MutableMap<String, String> = mutableMapOf(),
    var workers: MutableList<Worker> = mutableListOf(),
    var photoUris: MutableList<String> = mutableListOf(),
    var signatureB64: String = "",
    var history: MutableList<PtHistoryItem> = mutableListOf()
)
