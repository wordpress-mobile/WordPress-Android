package org.wordpress.android.ui.mysite

sealed class MySiteCardAndItemBuilderParams {
    data class DomainRegistrationCardBuilderParams(
        val isDomainCreditAvailable: Boolean,
        val domainRegistrationClick: () -> Unit
    ) : MySiteCardAndItemBuilderParams()
}
