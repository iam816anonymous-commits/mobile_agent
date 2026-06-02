package com.android.agentos.core.calibration

import com.android.agentos.core.models.VerificationResult

class ConfidenceCalibrator {
    private val outcomes = mutableListOf<Outcome>()

    fun recordOutcome(confidence: Float, actualSuccess: Boolean) {
        outcomes.add(Outcome(confidence, actualSuccess))
    }

    fun getCalibratedConfidence(rawConfidence: Float): Float {
        val similarOutcomes = outcomes.filter { Math.abs(it.confidence - rawConfidence) < 0.1f }
        if (similarOutcomes.isEmpty()) return rawConfidence

        val actualSuccessRate = similarOutcomes.count { it.actualSuccess }.toFloat() / similarOutcomes.size
        return (rawConfidence + actualSuccessRate) / 2f
    }

    fun getReliabilityCurve(): List<Pair<Float, Float>> {
        return (0..10).map { i ->
            val binStart = i / 10f
            val binOutcomes = outcomes.filter { it.confidence >= binStart && it.confidence < (binStart + 0.1f) }
            val rate = if (binOutcomes.isNotEmpty()) binOutcomes.count { it.actualSuccess }.toFloat() / binOutcomes.size else 0f
            binStart to rate
        }
    }

    private data class Outcome(val confidence: Float, val actualSuccess: Boolean)
}
