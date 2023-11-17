package org.wordpress.android.ui.barcodescanner

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

interface InputImageProvider {
    fun provideImage(imageProxy: ImageProxy): InputImage
}
class MediaImageProvider @Inject constructor() : InputImageProvider {
    @androidx.camera.core.ExperimentalGetImage
    override fun provideImage(imageProxy: ImageProxy): InputImage {
        return InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
    }
}
