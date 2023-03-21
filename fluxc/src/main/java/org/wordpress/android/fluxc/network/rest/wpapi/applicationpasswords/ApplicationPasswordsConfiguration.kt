package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import org.wordpress.android.fluxc.module.ApplicationPasswordsClientId
import java.util.Optional
import javax.inject.Inject

/**
 * Note: the [ApplicationPasswordsClientId] is provided as [Optional] because we want to keep the feature optional and
 * to not force the client apps to provide it. With this change, we will keep Dagger happy, and we move from a compile
 * error to a runtime error if it's missing.
 */
internal data class ApplicationPasswordsConfiguration @Inject constructor(
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
