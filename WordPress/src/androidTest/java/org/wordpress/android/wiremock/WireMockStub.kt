package org.wordpress.android.wiremock

/**
 * Represents a WireMock#stubFor call on a WireMockServer.
 * @property apiPathUrl The API path we are matching against.
 * @property fileName The filename used to return as the body stored in the androidTest resources sub directory.
 */
data class WireMockStub(
    val urlPath: WireMockUrlPath,
    val fileName: String,
)

/**
 * Enum used to represent supported URLs we can stub.
 */
enum class WireMockUrlPath(val path: String) {
    FEATURE_RESPONSE("/wpcom/v2/mobile/feature-flags/")
}
