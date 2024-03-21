package org.wordpress.android.fluxc.model.blaze

data class BlazePaymentMethods(
    val savedPaymentMethods: List<BlazePaymentMethod>,
    val addPaymentMethodUrls: BlazePaymentMethodUrls? // TODO make this non nullable when API returns URLs
)

data class BlazePaymentMethod(
    val id: String,
    val name: String,
    val info: PaymentMethodInfo
) {
    sealed interface PaymentMethodInfo {
        data class CreditCardInfo(
            val lastDigits: String,
            val expMonth: Int,
            val expYear: Int,
            val type: String,
            val nickname: String,
            val cardHolderName: String
        ) : PaymentMethodInfo

        object Unknown : PaymentMethodInfo
    }
}

data class BlazePaymentMethodUrls(
    val formUrl: String,
    val successUrl: String,
    val idUrlParameter: String
)
