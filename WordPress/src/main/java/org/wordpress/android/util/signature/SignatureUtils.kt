package org.wordpress.android.util.signature

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.Signature
import android.os.Build
import java.security.MessageDigest
import javax.inject.Inject

class SignatureUtils @Inject constructor() {
    @Suppress("DEPRECATION", "SwallowedException")
    fun getSignatureHash(ctxt: Context, packageName: String): String {
        val md: MessageDigest = MessageDigest.getInstance("SHA-256")
        val sig: Signature = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ctxt.packageManager.getPackageInfo(
                        packageName, PackageManager.GET_SIGNING_CERTIFICATES
                ).signingInfo.signingCertificateHistory[0]
            } else {
                ctxt.packageManager
                        .getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures[0]
            }
        } catch (nameNotFoundException: NameNotFoundException) {
            throw SignatureNotFoundException()
        }
        return toHexStringWithColons(md.digest(sig.toByteArray()))
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
