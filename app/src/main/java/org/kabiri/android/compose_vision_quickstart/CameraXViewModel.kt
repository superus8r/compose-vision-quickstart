package org.kabiri.android.compose_vision_quickstart

import android.app.Application
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executor

/** View model for interacting with CameraX.  */
@ExperimentalCoroutinesApi
class CameraXViewModel
/**
 * Create an instance which interacts with the camera service via the given application context.
 */
    (application: Application) : AndroidViewModel(application) {

    private val _detectedObjectText = MutableStateFlow("Detection in progress")
    val detectedObjectText: StateFlow<String>
        get() = _detectedObjectText

    fun clearText() {
        _detectedObjectText.value = ""
    }

    @androidx.camera.core.ExperimentalGetImage
    fun bindPreview(
        lifecycleOwner: LifecycleOwner,
        viewFinder: PreviewView,
        cameraProvider: ProcessCameraProvider,
        executor: Executor = ContextCompat.getMainExecutor(getApplication())
    ) {

        // Set up the preview use case to display camera preview.
        val preview = Preview.Builder().build()

        // Choose the camera by requiring a lens facing
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        // Create and set an analyzer for live tagging
        imageAnalysis.setAnalyzer(executor, { imageProxy ->

            val mediaImage = imageProxy.image
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            mediaImage?.let {
                val image = InputImage.fromMediaImage(it, rotationDegrees)

                // Live detection and tracking
                val options = ObjectDetectorOptions.Builder()
                    .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                    .enableMultipleObjects() // optional
                    .enableClassification()  // Optional
                    .build()

                val objectDetector = ObjectDetection.getClient(options)
                objectDetector.process(image)
                    .addOnSuccessListener { results ->
                        val resultString = StringBuilder()
                            .append("Detection Results:\n")
                        for (result in results) {
                            val label = result.labels.firstOrNull()
                            resultString.append("Index: ${label?.index} Name: ${label?.text}\n")
                        }
                        _detectedObjectText.value = resultString.toString()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "detection failed: $e")
                    }
                    .addOnCompleteListener {
                        // close the image proxy to avoid weird errors or no recognition at all!
                        imageProxy.close()
                    }
            }
        })

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Attach use cases to the camera with the same lifecycle owner
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                imageAnalysis,
                preview,
            )

            // Connect the preview use case to the previewView
            preview.setSurfaceProvider(viewFinder.surfaceProvider)

        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    companion object {
        private const val TAG = "CameraXViewModel"
    }
}