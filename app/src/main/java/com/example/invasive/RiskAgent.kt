package com.example.invasive

class RiskAgent {

    private var confidenceThreshold = 0.6f

    fun assessRisk(state: PlantState): String {

        if (state.confidence < confidenceThreshold) {
            return "UNCERTAIN"
        }

        return when (state.species) {
            "lantana", "parthenium" -> "HIGH"
            "neltuma" -> "MODERATE"
            "non_invasive" -> "LOW"
            else -> "UNKNOWN"
        }
    }

    fun updateThreshold(newThreshold: Float) {
        confidenceThreshold = newThreshold
    }

    fun getThreshold(): Float {
        return confidenceThreshold
    }
}