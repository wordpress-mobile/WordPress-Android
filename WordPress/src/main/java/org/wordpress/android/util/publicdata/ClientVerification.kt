package org.wordpress.android.util.publicdata

import org.wordpress.android.util.signature.SignatureUtils
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class ClientVerification @Inject constructor(
    private val jetpackPublicData: JetpackPublicData,
    private val signatureUtils: SignatureUtils,
    private val contextProvider: ContextProvider,
) {

    fun canTrust(callerPackage: String?): Boolean {
        if (callerPackage == null) {
            return false
        }
        val callerExpectedPackageId = jetpackPublicData.currentPackageId()
        val callerSignatureHash = signatureUtils.getSignatureHash(contextProvider.getContext(), callerExpectedPackageId)
        val callerExpectedSignatureHash = jetpackPublicData.currentPublicKeyHash()
        return callerPackage == callerExpectedPackageId && callerSignatureHash == callerExpectedSignatureHash
    }
}
