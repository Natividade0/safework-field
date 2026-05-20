package com.safefield.app

data class AprWorker(
    val name: String,
    val role: String,
    val signatureB64: String = "",
    val signedAt: String = ""
)

data class AprHistoryItem(
    val aprNumber: String = "",
    val emittedAt: String = "",
    val company: String = "",
    val place: String = "",
    val responsible: String = "",
    val fileName: String = "",
    val status: String = "EMITIDA"
)

data class AprData(
    var aprNumber: String = "",
    var company: String = "",
    var area: String = "",
    var place: String = "",
    var responsible: String = "",
    var activityDescription: String = "",
    var tools: String = "",
    var observations: String = "",
    var dateMillis: Long = System.currentTimeMillis(),
    var selectedActivities: MutableSet<String> = mutableSetOf(),
    var manualActivity: String = "",
    var items: MutableList<AprItem> = mutableListOf(),
    var workers: MutableList<AprWorker> = mutableListOf(),
    var photos: MutableList<String> = mutableListOf(),
    var responsibleSignatureB64: String = "",
    var history: MutableList<AprHistoryItem> = mutableListOf()
)
