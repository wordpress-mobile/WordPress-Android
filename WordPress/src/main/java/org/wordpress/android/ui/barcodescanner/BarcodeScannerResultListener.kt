package org.wordpress.android.ui.barcodescanner

interface BarcodeScannerResultListener {
    fun onBarcodeScanned(status: CodeScannerStatus)
}

