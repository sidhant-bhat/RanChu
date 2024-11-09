package com.example.ranchu

data class PredictionResponse
    (
    val status: String,
    val total_predictions: Int,
    val ransomware_detected: Int,
    val timestamps_affected: List<String>
)
