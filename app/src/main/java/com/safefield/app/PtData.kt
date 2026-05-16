package com.safefield.app

data class Worker(
    val name: String,
    val role: String,
    val signatureB64: String = "",
    val signedAt: String = ""
)

data class PtHistoryItem(
    val ptNumber: String = "",
    val emittedAt: String = "",
    val place: String = "",
    val company: String = "",
    val responsible: String = "",
    val fileName: String = "",
    val status: String = "LIBERADA",
    val startMillis: Long = 0L,
    val endMillis: Long = 0L,
    val closedAt: String = "",
    val closeNote: String = "",
    val closeResponsible: String = "",
    val closeIncident: Boolean = false,
    val closePhotoCount: Int = 0
) {
    constructor(emittedAt: String, place: String, fileName: String, status: String = "LIBERADA") : this(
        ptNumber = fileName.removePrefix("SafeField_").removeSuffix(".pdf").ifBlank { "PT emitida" },
        emittedAt = emittedAt,
        place = place,
        company = "",
        responsible = "",
        fileName = fileName,
        status = status
    )
}

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
    var emergencyPoint: String = "",
    var emergencyPhone: String = "",
    var emergencyProcedure: String = "",
    var observations: String = "",
    var manualActivity: String = "",
    var activities: MutableSet<String> = mutableSetOf(),
    var checklist: MutableMap<String, String> = mutableMapOf(),
    var controls: MutableMap<String, String> = mutableMapOf(),
    var workers: MutableList<Worker> = mutableListOf(),
    var photoUris: MutableList<String> = mutableListOf(),
    var signatureB64: String = "",
    var closureAt: String = "",
    var closureResponsible: String = "",
    var closureAreaCondition: String = "",
    var closureNotes: String = "",
    var closurePending: String = "",
    var closureIncident: Boolean = false,
    var closurePhotoUris: MutableList<String> = mutableListOf(),
    var closureSignatureB64: String = "",
    var history: MutableList<PtHistoryItem> = mutableListOf()
)
