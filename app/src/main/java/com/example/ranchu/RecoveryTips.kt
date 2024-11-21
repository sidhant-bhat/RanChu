package com.example.ranchu

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ranchu.databinding.ActivityLogInBinding
import com.example.ranchu.databinding.ActivityPredictionResultBinding
import com.example.ranchu.databinding.ActivityRecoveryTipsBinding

class RecoveryTips : AppCompatActivity() {
    // private val binding: ActivityRecoveryTipsBinding by lazy { ActivityRecoveryTipsBinding.inflate(layoutInflater) }
    private lateinit var binding: ActivityRecoveryTipsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecoveryTipsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.learnMoreButton.setOnClickListener {
            val query = "how to recover from ransomware?"

            // Create an intent to open a browser
            val searchIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://www.google.com/search?q=$query")
            }

            // Start the browser activity
            startActivity(searchIntent)
        }


        enableEdgeToEdge()

//        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//           insets
//        }
    }
}