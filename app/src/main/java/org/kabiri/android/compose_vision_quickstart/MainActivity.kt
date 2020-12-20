package org.kabiri.android.compose_vision_quickstart

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.kabiri.android.compose_vision_quickstart.ui.home.SimpleCameraPreview
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.activity.viewModels
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.remember
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity() {

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private val cameraXViewModel by viewModels<CameraXViewModel>()

    @androidx.camera.core.ExperimentalGetImage
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Request camera permissions
            if (allPermissionsGranted()) {
                Box {
                    val cameraProviderFuture =
                            remember { ProcessCameraProvider.getInstance(this@MainActivity) }
                    SimpleCameraPreview(
                            context = this@MainActivity,
                            cameraXViewModel = cameraXViewModel,
                            cameraProviderFuture = cameraProviderFuture,
                            executor = ContextCompat.getMainExecutor(this@MainActivity)
                    )
                }
            } else {
                ActivityCompat.requestPermissions(
                        this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}