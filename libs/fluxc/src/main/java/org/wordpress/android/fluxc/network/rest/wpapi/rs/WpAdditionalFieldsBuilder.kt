package org.wordpress.android.fluxc.network.rest.wpapi.rs

import uniffi.wp_api.JsonValue
import uniffi.wp_api.WpAdditionalFields
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the [WpAdditionalFields] carrier wordpress-rs flattens into a request body, for the fields
 * its typed params cannot express.
 *
 * Wrapped in a class of its own because [WpAdditionalFields] is backed by the Rust library: it
 * cannot be constructed in a JVM unit test, where the JNA native library is not on the classpath.
 * Injecting this lets callers be tested on *which* fields they ask for.
 */
@Singleton
class WpAdditionalFieldsBuilder @Inject constructor() {
    /**
     * Returns a carrier that serializes each of [keys] as an explicit empty JSON array.
     *
     * wp/v2 reads an absent field as "leave this alone", so clearing a list-valued field needs the
     * empty array to actually be in the body.
     */
    fun emptyArrays(keys: List<String>): WpAdditionalFields =
        keys.fold(WpAdditionalFields()) { fields, key ->
            fields.withValue(key, JsonValue.Array(emptyList()))
        }
}
