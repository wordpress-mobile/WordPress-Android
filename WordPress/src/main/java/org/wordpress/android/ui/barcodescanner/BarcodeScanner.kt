package org.wordpress.android.ui.barcodescanner

import android.content.res.Configuration
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.wordpress.android.ui.compose.theme.AppTheme
import androidx.camera.core.Preview as CameraPreview

@Composable
fun BarcodeScanner(
    codeScanner: CodeScanner,
    onScannedResult: (Flow<CodeScannerStatus>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { context ->
                val previewView = PreviewView(context)
                val preview = CameraPreview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                val selector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
                val imageAnalysis = ImageAnalysis.Builder().setTargetResolution(
                    Size(
                        previewView.width,
                        previewView.height
                    )
                )
                    .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    onScannedResult(codeScanner.startScan(imageProxy))
                }
                try {
                    cameraProviderFuture.get().bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
                } catch (e: IllegalStateException) {
                    onScannedResult(
                        flowOf(
                            CodeScannerStatus.Failure(
                                e.message
                                    ?: "Illegal state exception while binding camera provider to lifecycle",
                                CodeScanningErrorType.Other(e)
                            )
                        )
                    )
                } catch (e: IllegalArgumentException) {
                    onScannedResult(
                        flowOf(
                            CodeScannerStatus.Failure(
                                e.message
                                    ?: "Illegal argument exception while binding camera provider to lifecycle",
                                CodeScanningErrorType.Other(e)
                            )
                        )
                    )
                }
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

class DummyCodeScanner : CodeScanner {
    override fun startScan(imageProxy: ImageProxy): Flow<CodeScannerStatus> {
        return flowOf(CodeScannerStatus.Success("", GoogleBarcodeFormatMapper.BarcodeFormat.FormatUPCA))
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BarcodeScannerScreenPreview() {
    AppTheme {
        BarcodeScanner(codeScanner = DummyCodeScanner(), onScannedResult = {})
    }
}
