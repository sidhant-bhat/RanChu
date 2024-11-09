package com.example.ranchu

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.example.ranchu.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.net.SocketTimeoutException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var selectedFiles: MutableList<Uri>
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedFiles = mutableListOf()

        // Initialize the file picker launcher
        filePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { data ->
                    handleFileSelection(data)
                }
            } else {
                Toast.makeText(this, "File selection cancelled", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize permission launcher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                openFilePicker()
            } else {
                Toast.makeText(
                    this,
                    "Storage permission is required to select files",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Set click listener for the insert button
        binding.insertBtn.setOnClickListener {
            checkPermissions()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "text/csv",
                "text/comma-separated-values",
                "application/csv",
                "application/excel",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            filePickerLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e("FilePicker", "Error launching file picker", e)
            Toast.makeText(this, "Unable to open file picker: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleFileSelection(data: Intent) {
        selectedFiles.clear()
        try {
            when {
                data.clipData != null -> {
                    // Handle multiple files
                    val count = data.clipData!!.itemCount
                    for (i in 0 until count) {
                        val uri = data.clipData!!.getItemAt(i).uri
                        selectedFiles.add(uri)
                        Log.d("FileSelection", "Selected file $i: $uri")
                    }
                }
                data.data != null -> {
                    // Handle single file
                    val uri = data.data!!
                    selectedFiles.add(uri)
                    Log.d("FileSelection", "Selected single file: $uri")
                }
            }

            if (selectedFiles.isNotEmpty()) {
                Toast.makeText(this, "${selectedFiles.size} file(s) selected", Toast.LENGTH_SHORT).show()
                uploadFiles(selectedFiles)
            } else {
                Toast.makeText(this, "No files selected", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("FileSelection", "Error handling file selection", e)
            Toast.makeText(this, "Error selecting files: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 and above - no need for storage permission
            openFilePicker()
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openFilePicker()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    showPermissionExplanationDialog()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Storage permission is required to select files for upload.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private suspend fun uriToFile(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        val documentFile = DocumentFile.fromSingleUri(context, uri)
        val fileName = documentFile?.name ?: "temp_${System.currentTimeMillis()}.csv"
        val tempFile = File(context.cacheDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    }
    private fun uploadFiles() {
        lifecycleScope.launch {
            try {
                // Create MultipartBody.Part for each file
                val fileAccessPart = createMultipartFromUri("file_access", fileAccessUri!!)
                val networkTrafficPart = createMultipartFromUri("network_traffic", networkTrafficUri!!)
                val systemPerformancePart = createMultipartFromUri("system_performance", systemPerformanceUri!!)
                val userBehaviorPart = createMultipartFromUri("user_behavior", userBehaviorUri!!)

                // Make API call
                val response = RetrofitClient.api.uploadFiles(
                    fileAccessPart,
                    networkTrafficPart,
                    systemPerformancePart,
                    userBehaviorPart
                )

                if (response.isSuccessful) {
                    val result = response.body()
                    // Handle successful response
                    Toast.makeText(this@MainActivity, "Upload successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Upload failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: SocketTimeoutException) {
                Toast.makeText(this@MainActivity, "Connection timed out", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createMultipartFromUri(paramName: String, uri: Uri): MultipartBody.Part {
        val inputStream = contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes() ?: ByteArray(0)
        val requestBody = RequestBody.create("text/csv".toMediaTypeOrNull(), bytes)
        return MultipartBody.Part.createFormData(paramName, "file.csv", requestBody)
    }

}