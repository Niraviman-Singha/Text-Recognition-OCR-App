package com.example.textrecognitionocrapp

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var cameraImage: ImageView
    private lateinit var captureImageBtn: Button
    private lateinit var resultText: TextView
    private lateinit var copyTextBtn: Button

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>

    private var currentPhotoPath: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraImage = findViewById(R.id.cameraImage)
        captureImageBtn = findViewById(R.id.captureImageBtn)
        resultText = findViewById(R.id.resultText)
        copyTextBtn = findViewById(R.id.copyTextBtn)

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){
            isGranted->
            if (isGranted){
                captureImage()
            }
            else{
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()){
            success->
            if (success){
                currentPhotoPath?.let { path->
                    val bitmap = BitmapFactory.decodeFile(path)
                    cameraImage.setImageBitmap(bitmap)
                    recognizeText(bitmap)
                }
            }
        }

        captureImageBtn.setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }


    }

    private fun createImageFile(): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun captureImage() {
        val photoFile: File? = try {
            createImageFile()
        }
        catch (ex: IOException){
           Toast.makeText(this,"Error occurred while creating the file ", Toast.LENGTH_SHORT).show()
            null
        }
        photoFile?.also {
            val photoUri:Uri = FileProvider.getUriForFile(this,"${applicationContext.packageName}.provider", it)
            takePictureLauncher.launch(photoUri)
        }
    }

    private fun recognizeText(bitmap: Bitmap){
        val image = InputImage.fromBitmap(bitmap,0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image).addOnSuccessListener { ocrText->
            resultText.text = ocrText.text
            resultText.movementMethod = ScrollingMovementMethod()
            copyTextBtn.visibility = Button.VISIBLE
            copyTextBtn.setOnClickListener {
                val clipboard = ContextCompat.getSystemService(this, android.content.ClipboardManager::class.java)
                val clip = android.content.ClipData.newPlainText("recognizer text", ocrText.text)
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(this, "Text Copied", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {e ->
            Toast.makeText(this, "Failed to recognize text: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}