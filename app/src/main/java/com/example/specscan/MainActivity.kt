package com.example.specscan

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.specscan.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var csvStore: CsvStore

    private var imageCapture: ImageCapture? = null
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to scan labels", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        csvStore = CsvStore(this)
        updateRowCount()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.scanButton.setOnClickListener { captureAndRecognize() }

        binding.saveButton.setOnClickListener {
            val text = binding.resultEditText.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "Nothing to save yet — scan a label first", Toast.LENGTH_SHORT).show()
            } else {
                val serial = csvStore.appendEntry(text)
                Toast.makeText(this, "Saved as entry #$serial", Toast.LENGTH_SHORT).show()
                binding.resultEditText.setText("")
                binding.saveButton.isEnabled = false
                updateRowCount()
            }
        }

        binding.shareButton.setOnClickListener { shareCsv() }

        binding.resultEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                binding.saveButton.isEnabled = !s.isNullOrBlank()
            }
        })
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera init failed: ${exc.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureAndRecognize() {
        val capture = imageCapture ?: return
        val photoFile = File(cacheDir, "label_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        binding.scanButton.isEnabled = false

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    runTextRecognition(photoFile)
                }

                override fun onError(exc: ImageCaptureException) {
                    binding.scanButton.isEnabled = true
                    Toast.makeText(this@MainActivity, "Capture failed: ${exc.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun runTextRecognition(photoFile: File) {
        val image = InputImage.fromFilePath(this, Uri.fromFile(photoFile))
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                binding.scanButton.isEnabled = true
                val text = visionText.text.trim()
                if (text.isEmpty()) {
                    Toast.makeText(this, "No text detected — try moving closer to the label", Toast.LENGTH_SHORT).show()
                } else {
                    binding.resultEditText.setText(text)
                    binding.saveButton.isEnabled = true
                }
                photoFile.delete()
            }
            .addOnFailureListener { e ->
                binding.scanButton.isEnabled = true
                Toast.makeText(this, "Text recognition failed: ${e.message}", Toast.LENGTH_LONG).show()
                photoFile.delete()
            }
    }

    private fun shareCsv() {
        val csvFile = csvStore.getFile()
        if (!csvFile.exists() || csvStore.rowCount() == 0) {
            Toast.makeText(this, "No entries saved yet", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", csvFile)
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(android.content.Intent.createChooser(shareIntent, "Share spectacles_labels.csv"))
    }

    private fun updateRowCount() {
        binding.rowCountText.text = "Saved entries: ${csvStore.rowCount()}"
    }

    override fun onDestroy() {
        super.onDestroy()
        recognizer.close()
    }
}
