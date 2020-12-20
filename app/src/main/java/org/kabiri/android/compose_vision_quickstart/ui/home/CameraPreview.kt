package org.kabiri.android.compose_vision_quickstart.ui.home

import android.content.Context
import android.view.ViewGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.ClickableText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AmbientLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kabiri.android.compose_vision_quickstart.CameraXViewModel
import java.util.concurrent.Executor
import androidx.compose.runtime.getValue

@ExperimentalCoroutinesApi
@androidx.camera.core.ExperimentalGetImage
@Composable
fun SimpleCameraPreview(
    context: Context,
    cameraXViewModel: CameraXViewModel,
    cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    executor: Executor,
) {
    val lifecycleOwner = AmbientLifecycleOwner.current
    val cameraView = remember {
        // Creates camera view
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }
    Box {
        CameraPreview(
            cameraView = cameraView,
            cameraProviderFuture = cameraProviderFuture,
            cameraXViewModel = cameraXViewModel,
            lifecycleOwner = lifecycleOwner,
            executor = executor
        )
        TextOverlay(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .align(Alignment.BottomCenter),
            cameraXViewModel = cameraXViewModel,
        )
    }
}

@ExperimentalCoroutinesApi
@Composable
fun TextOverlay(
    modifier: Modifier,
    cameraXViewModel: CameraXViewModel
) {
    val text by cameraXViewModel.detectedObjectText.collectAsState()
    Snackbar(
        modifier = modifier,
        text = { Text(text = text) },
        action = {
            ClickableText(
                text = with(AnnotatedString.Builder()) {
                    pushStyle(
                        SpanStyle(
                            fontStyle = MaterialTheme.typography.h6.fontStyle,
                            fontWeight = MaterialTheme.typography.h6.fontWeight,
                            color = Color.White,
                        )
                    )
                    append("Clear")
                    pop()
                    toAnnotatedString()
                },
                onClick = {
                    cameraXViewModel.clearText()
                }
            )
        },
        actionOnNewLine = true,
    )
}

@ExperimentalCoroutinesApi
@androidx.camera.core.ExperimentalGetImage
@Composable
fun CameraPreview(
    cameraView: PreviewView,
    cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    cameraXViewModel: CameraXViewModel,
    lifecycleOwner: LifecycleOwner,
    executor: Executor
) {
    AndroidView({ cameraView }) { previewView ->
        cameraProviderFuture.addListener({
            // Camera provider is now guaranteed to be available
            val cameraProvider = cameraProviderFuture.get()
            // Set up the preview use case to display camera preview.
            cameraXViewModel.bindPreview(
                lifecycleOwner = lifecycleOwner,
                viewFinder = previewView,
                cameraProvider = cameraProvider
            )
        }, executor)
    }
}