package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import org.wordpress.android.fluxc.module.ApplicationPasswordsClientId
import java.util.Optional
import javax.inject.Inject

data class ApplicationPasswordsConfiguration @Inject constructor(
    @ApplicationPasswordsClientId private val applicationNameOptional: Optional<String>
) {
    val isEnabled: Boolean
        get() = applicationNameOptional.isPresent

    val applicationName: String
        get() = applicationNameOptional.orElseThrow {
            NoSuchElementException(
                "Please make sure to inject a String instance with " +
                    "the annotation @${ApplicationPasswordsClientId::class.simpleName} to the Dagger graph" +
                    "to be able to use the Application Passwords feature"
            )
        }
}
