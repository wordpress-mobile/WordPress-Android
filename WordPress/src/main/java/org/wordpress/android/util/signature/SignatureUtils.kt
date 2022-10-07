package org.wordpress.android.util.signature

import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import org.wordpress.android.viewmodel.ContextProvider
import java.security.MessageDigest
import javax.inject.Inject

class SignatureUtils @Inject constructor(
    private val contextProvider: ContextProvider
) {
    private val messageDigest = MessageDigest.getInstance("SHA-256")

    fun checkSignatureHash(
        trustedPackageId: String,
        trustedSignatureHash: String
    ) = if (VERSION.SDK_INT >= VERSION_CODES.P) {
        checkSignatureHashAfterApi28(trustedPackageId, trustedSignatureHash)
    } else {
        checkSignatureHashBeforeBeforeApi28(trustedPackageId, trustedSignatureHash)
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun checkSignatureHashAfterApi28(
        trustedPackageId: String,
        trustedSignatureHash: String
    ) = try {
        val signingInfo = contextProvider.getContext().packageManager.getPackageInfo(
                trustedPackageId, PackageManager.GET_SIGNING_CERTIFICATES
        ).signingInfo
        val allSignaturesMatch = signingInfo.signingCertificateHistory.all {
            toHexStringWithColons(messageDigest.digest(it.toByteArray())) == trustedSignatureHash
        }
        if (allSignaturesMatch) {
            true
        } else throw SignatureNotFoundException()
    } catch (exception: Exception) {
        throw SignatureNotFoundException()
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun checkSignatureHashBeforeBeforeApi28(
        trustedPackageId: String,
        trustedSignatureHash: String
    ): Boolean {
        try {
            val signatures = contextProvider.getContext().packageManager
                    .getPackageInfo(trustedPackageId, PackageManager.GET_SIGNATURES).signatures
            val allSignaturesMatch = signatures.all {
                toHexStringWithColons(messageDigest.digest(it.toByteArray())) == trustedSignatureHash
            }
            return if (allSignaturesMatch) {
                true
            } else throw SignatureNotFoundException()
        } catch (exception: Exception) {
            throw SignatureNotFoundException()
        }
    }

    @Suppress("MagicNumber")
    private fun toHexStringWithColons(bytes: ByteArray): String {
        val hexString = StringBuilder()
        for (element in bytes) {
            val hex = Integer.toHexString(0xFF and element.toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }
}

class SignatureNotFoundException : Exception()
