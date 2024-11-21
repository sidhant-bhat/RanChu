package com.example.ranchu

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ranchu.databinding.ActivityPredictionResultBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class PredictionResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPredictionResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPredictionResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve the prediction data from the Intent
        val status = intent.getStringExtra("status") ?: "Unknown"
        val totalPredictions = intent.getIntExtra("total_predictions", 0)
        val ransomwareDetected = intent.getIntExtra("ransomware_detected", 0)
        val timestamps = intent.getStringArrayListExtra("timestamps_affected") ?: arrayListOf()

        // Display the data
        binding.statusTextView.text = "Status: $status"
        binding.totalPredictionsTextView.text = "Total Predictions: $totalPredictions"
        binding.ransomwareDetectedTextView.text = "Ransomware Detected: $ransomwareDetected"
        binding.timestampsTextView.text = "Timestamps Affected: ${timestamps.joinToString(", ")}"

        // Setup Pie Chart
        setupPieChart(ransomwareDetected, totalPredictions - ransomwareDetected)
    }

    private fun setupPieChart(ransomwareCount: Int, safeCount: Int) {
        // Create pie entries
        val entries = ArrayList<PieEntry>().apply {
            add(PieEntry(ransomwareCount.toFloat(), "Ransomware"))
            add(PieEntry(safeCount.toFloat(), "Safe"))
        }

        // Create dataset
        val dataSet = PieDataSet(entries, "Prediction Results")
        dataSet.colors = listOf(Color.RED, Color.GREEN)
        dataSet.valueTextSize = 15f
        dataSet.valueTextColor = Color.WHITE

        // Create pie data
        val data = PieData(dataSet)

        // Configure pie chart
        binding.pieChart.apply {
            this.data = data
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = "Predictions"
            animateY(1400)
            legend.isEnabled = true
        }

        // Refresh the chart
        binding.pieChart.invalidate()
    }
}
