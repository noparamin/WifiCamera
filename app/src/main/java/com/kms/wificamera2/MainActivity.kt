package com.kms.wificamera2

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix

import android.os.Bundle
import android.provider.Settings

import android.util.Size
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition

import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    var imageCapture : ImageCapture? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)
        bindCameraUseCases()
        captureButton.setOnClickListener {
            takePhoto()
        }
    }

    fun takePhoto() {
        imageCapture?.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                textRecognize(bitmap)
                super.onCaptureSuccess(image)
            }
        })
    }

    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {

        val buffer = imageProxy.planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        //Rotate bitmap
        val matrix = Matrix()
        matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun bindCameraUseCases() {
        val rotation = 0
        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetRotation(rotation)
                .build()

            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(960, 1280))
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(rotation)
                .build()

            cameraProvider.unbindAll()

            val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            preview.setSurfaceProvider(viewFinder.getSurfaceProvider())
        }, ContextCompat.getMainExecutor(this))
    }

    fun textRecognize(bitmap: Bitmap) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient()
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                for(block in visionText.textBlocks) {
                    println("block")
                    println(block.text)
                    for(line in block.lines) {
                        println("line")
                        val containPw = line.text.contains("pw", true)
                        val containPassword = line.text.contains("password", true)
                        if(containPw || containPassword) {
                            if(line.text.contains(":"))
                            {
                                var token = line.text.split(':')
                                println(token[1])
                                val password = token[1]
                                val clip: ClipData = ClipData.newPlainText("password", password)
                                clipboard.setPrimaryClip(clip)
                                println(line.text)
                            } else {
                                var token = line.text.split(' ')
                                println(token[1])
                                val password = token[1]
                                val clip: ClipData = ClipData.newPlainText("password", password)
                                clipboard.setPrimaryClip(clip)
                                println(line.text)
                            }
                            Toast.makeText(this,"비밀번호 복사 완료", Toast.LENGTH_LONG)
                            goWifiSetting()
                        }
                        for(element in line.elements) {
                            println("element")
                            println(element.text)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                Toast.makeText(this,"비밀번호 인식 실패! 다시 찍어주세요", Toast.LENGTH_LONG)
            }
    }



    fun goWifiSetting() {
        val goSetting = Intent(Settings.ACTION_WIFI_SETTINGS)
        startActivityForResult(goSetting,0)
    }


}