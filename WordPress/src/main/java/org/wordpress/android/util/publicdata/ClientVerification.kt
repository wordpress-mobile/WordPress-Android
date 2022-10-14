package org.wordpress.android.util.publicdata

import org.wordpress.android.util.signature.SignatureUtils
import javax.inject.Inject

class ClientVerification @Inject constructor(
    private val jetpackPublicData: JetpackPublicData,
    private val signatureUtils: SignatureUtils,
) {
    fun canTrust(callerPackage: String?): Boolean {
        if (callerPackage == null) {
            return false
        }
        val callerExpectedPackageId = jetpackPublicData.currentPackageId()
        val canTrustPackageId = callerPackage == callerExpectedPackageId
        val callerExpectedSignatureHash = jetpackPublicData.currentPublicKeyHash()
        val canTrustSignatureHash = signatureUtils.checkSignatureHash(
                callerExpectedPackageId, callerExpectedSignatureHash
        )
        return canTrustPackageId && canTrustSignatureHash
    }
}
