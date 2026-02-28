package com.example.invasive

class ActionAgent {

    fun suggestAction(riskLevel: String): String {

        return when (riskLevel) {

            "HIGH" ->
                "🚨 Remove immediately.\nUse mechanical removal or approved herbicide."

            "MODERATE" ->
                "⚠ Monitor growth.\nRestrict spread and prune regularly."

            "LOW" ->
                "✅ No removal needed.\nMonitor periodically."

            "UNCERTAIN" ->
                "⚠ Low confidence.\nPlease retake clearer image."

            else ->
                "Species not recognized."
        }
    }
}