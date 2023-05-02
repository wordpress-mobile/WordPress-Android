package org.wordpress.android.util.config

import org.wordpress.android.annotation.RemoteFieldDefaultGenerater
import org.wordpress.android.util.config.BloggingPromptsEndpointConfig.Companion.BLOGGING_PROMPTS_ENDPOINT_V2
import javax.inject.Inject

private const val BLOGGING_PROMPTS_ENDPOINT_REMOTE_FIELD = "blogging_prompts_endpoint"
private const val BLOGGING_PROMPTS_ENDPOINT_DEFAULT = BLOGGING_PROMPTS_ENDPOINT_V2

// TODO this class is temporary until we can remove v2 completely and only use the v3 endpoint
@RemoteFieldDefaultGenerater(
    remoteField = BLOGGING_PROMPTS_ENDPOINT_REMOTE_FIELD,
    defaultValue = BLOGGING_PROMPTS_ENDPOINT_DEFAULT
)
class BloggingPromptsEndpointConfig @Inject constructor(
    appConfig: AppConfig,
) : RemoteConfigField<String>(
    appConfig = appConfig,
    remoteField = BLOGGING_PROMPTS_ENDPOINT_REMOTE_FIELD
) {
    // anything that is not v3 should use v2
    fun shouldUseV2(): Boolean = getValue<String>().lowercase() != BLOGGING_PROMPTS_ENDPOINT_V3

    companion object {
        const val BLOGGING_PROMPTS_ENDPOINT_V2 = "v2"
        const val BLOGGING_PROMPTS_ENDPOINT_V3 = "v3"
    }
}
