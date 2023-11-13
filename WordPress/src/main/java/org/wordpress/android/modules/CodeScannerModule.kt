package org.wordpress.android.modules

import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.ui.barcodescanner.CodeScanner
import org.wordpress.android.ui.barcodescanner.GoogleBarcodeFormatMapper
import org.wordpress.android.ui.barcodescanner.GoogleCodeScannerErrorMapper
import org.wordpress.android.ui.barcodescanner.GoogleMLKitCodeScanner
import org.wordpress.android.ui.barcodescanner.MediaImageProvider

@InstallIn(SingletonComponent::class)
@Module
class CodeScannerModule {
    @Provides
    @Reusable
    fun provideGoogleCodeScanner(
        barcodeScanner: BarcodeScanner,
        googleCodeScannerErrorMapper: GoogleCodeScannerErrorMapper,
        barcodeFormatMapper: GoogleBarcodeFormatMapper,
        inputImageProvider: MediaImageProvider,
    ): CodeScanner {
        return GoogleMLKitCodeScanner(
            barcodeScanner,
            googleCodeScannerErrorMapper,
            barcodeFormatMapper,
            inputImageProvider,
        )
    }

    @Provides
    @Reusable
    fun providesGoogleBarcodeScanner() = BarcodeScanning.getClient()

    @Provides
    @Reusable
    fun provideInputImageProvider() = MediaImageProvider()
}
