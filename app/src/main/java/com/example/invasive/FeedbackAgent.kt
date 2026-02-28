package com.example.invasive

class FeedbackAgent(private val riskAgent: RiskAgent) {

    private var score = 0

    fun correctPrediction() {
        score += 1
        adjustPolicy()
    }

    fun wrongPrediction() {
        score -= 2
        adjustPolicy()
    }

    private fun adjustPolicy() {

        if (score < -3) {
            riskAgent.updateThreshold(0.75f)
        } else if (score > 3) {
            riskAgent.updateThreshold(0.55f)
        }
    }
}