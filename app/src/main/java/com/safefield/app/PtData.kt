package com.safefield.app

import android.graphics.Bitmap
import android.net.Uri

data class PtData(
    val fields: MutableMap<String, String> = mutableMapOf(),
    val selectedActivities: MutableSet<String> = mutableSetOf(),
    val risks: MutableSet<String> = mutableSetOf(),
    val manualRisks: MutableSet<String> = mutableSetOf(),
    val checklist: MutableMap<Int, String> = mutableMapOf(),
    val controls: MutableMap<String, String> = mutableMapOf(),
    val workers: MutableList<String> = mutableListOf(),
    val photos: MutableList<Uri> = mutableListOf(),
    val history: MutableList<String> = mutableListOf(),
    var signature: Bitmap? = null
)
