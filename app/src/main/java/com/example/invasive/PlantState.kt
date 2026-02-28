package com.example.invasive

data class PlantState(
    val species: String,
    val confidence: Float
)

data class AgentDecision(
    val riskLevel: String,
    val recommendation: String
)