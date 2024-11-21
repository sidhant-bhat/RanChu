package com.example.ranchu

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ranchu.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var fileAccessUri: Uri? = null
    private var networkTrafficUri: Uri? = null
    private var systemPerformanceUri: Uri? = null
    private var userBehaviorUri: Uri? = null
    private var currentFileType: String? = null

    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        filePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                result.data?.data?.let { uri ->
                    Log.d("FilePicker", "File selected: $uri, Type: $currentFileType")
                    when (currentFileType) {
                        "file_access" -> {
                            fileAccessUri = uri
                            updateFileStatus(binding.fileAccessStatus, uri)
                        }
                        "network_traffic" -> {
                            networkTrafficUri = uri
                            updateFileStatus(binding.networkTrafficStatus, uri)
                        }
                        "system_performance" -> {
                            systemPerformanceUri = uri
                            updateFileStatus(binding.systemPerformanceStatus, uri)
                        }
                        "user_behavior" -> {
                            userBehaviorUri = uri
                            updateFileStatus(binding.userBehaviorStatus, uri)
                        }
                    }
                    currentFileType = null
                }
            } else {
                Log.d("FilePicker", "File selection cancelled or failed")
            }
        }

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                openFilePickersForAll()
            } else {
                Toast.makeText(
                    this,
                    "Storage permission is required to select files",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        binding.selectFileAccessButton.setOnClickListener { openFilePickerFor("file_access") }
        binding.selectNetworkTrafficButton.setOnClickListener { openFilePickerFor("network_traffic") }
        binding.selectSystemPerformanceButton.setOnClickListener { openFilePickerFor("system_performance") }
        binding.selectUserBehaviorButton.setOnClickListener { openFilePickerFor("user_behavior") }
        binding.uploadButton.setOnClickListener { checkAndUploadFiles() }
    }

    private fun openFilePickersForAll() {
        openFilePickerFor("file_access")
        openFilePickerFor("network_traffic")
        openFilePickerFor("system_performance")
        openFilePickerFor("user_behavior")
    }

    private fun openFilePickerFor(fileType: String) {
        currentFileType = fileType
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(intent)
    }

    private fun updateFileStatus(statusTextView: TextView, uri: Uri) {
        val fileName = getFileNameFromUri(uri) ?: "Unknown file"
        statusTextView.text = "File selected: $fileName"

        statusTextView.setTextColor(Color.GREEN)
        statusTextView.visibility = View.VISIBLE
    }


    private fun getFileNameFromUri(uri: Uri): String? {
        return if (uri.scheme == "content") {
            // If the URI is a content URI, use the content resolver to query for the file name
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("_display_name")
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            }
        } else {
            // If the URI is a file URI, get the file name directly from the path
            uri.path?.substringAfterLast("/")
        }
    }


    private fun checkAndUploadFiles() {
        if (fileAccessUri != null && networkTrafficUri != null && systemPerformanceUri != null && userBehaviorUri != null) {
            uploadFiles(fileAccessUri!!, networkTrafficUri!!, systemPerformanceUri!!, userBehaviorUri!!)
        } else {
            Toast.makeText(this, "Please select all files before uploading", Toast.LENGTH_LONG).show()
        }
    }

    private fun uploadFiles(
        fileAccessUri: Uri,
        networkTrafficUri: Uri,
        systemPerformanceUri: Uri,
        userBehaviorUri: Uri
    ) {
        lifecycleScope.launch {
            try {
                val fileAccessPart = createMultipartFromUri(this@MainActivity, "file_access", fileAccessUri)
                val networkTrafficPart = createMultipartFromUri(this@MainActivity, "network_traffic", networkTrafficUri)
                val systemPerformancePart = createMultipartFromUri(this@MainActivity, "system_performance", systemPerformanceUri)
                val userBehaviorPart = createMultipartFromUri(this@MainActivity, "user_behavior", userBehaviorUri)

                val response = RetrofitClient.api.uploadFiles(
                    file_access = fileAccessPart,
                    network_traffic = networkTrafficPart,
                    system_performance = systemPerformancePart,
                    user_behavior = userBehaviorPart
                )

                if (response.isSuccessful) {
                    val responseBody = response.body()

                    if (responseBody != null) {
                        // Start PredictionResultActivity and pass the data
                        val intent = Intent(this@MainActivity, PredictionResultActivity::class.java).apply {
                            putExtra("status", responseBody.status)
                            putExtra("total_predictions", responseBody.total_predictions)
                            putExtra("ransomware_detected", responseBody.ransomware_detected)
                            putStringArrayListExtra("timestamps_affected", ArrayList(responseBody.timestamps_affected))
                        }
                        startActivity(intent)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(this@MainActivity, "Upload failed: Upload files correctly", Toast.LENGTH_SHORT).show()
                    Log.e("MainActivity", "Upload failed: $errorBody")
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "Upload failed: ${e.message}")
            }
        }
    }


    private suspend fun createMultipartFromUri(context: Context, paramName: String, uri: Uri): MultipartBody.Part {
        val file = uriToFile(context, uri)
        val requestBody = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(paramName, file.name, requestBody)
    }

    private suspend fun uriToFile(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File(context.cacheDir, "${System.currentTimeMillis()}.temp")
        tempFile.outputStream().use { outputStream ->
            inputStream?.copyTo(outputStream)
        }
        return tempFile
    }
}
