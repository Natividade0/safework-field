package com.safefield.app

data class AprItem(
    val activity: String,
    val danger: String,
    val risk: String,
    val consequence: String,
    val control: String,
    val epi: String,
    val severity: Int,
    val probability: Int,
    val classification: String,
    val step: String = "",
    val recommendedEpi: String = epi,
    val riskLevel: String = classification
)
