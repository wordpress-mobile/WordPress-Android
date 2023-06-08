package org.wordpress.android.fluxc.store.jetpackai

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JetpackAIStore @Inject constructor(
    private val jetpackAIRestClient: JetpackAIRestClient,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchJetpackAICompletions(
        site: SiteModel,
        prompt: String
    ) = coroutineEngine.withDefaultContext(
        tag = AppLog.T.API,
        caller = this,
        loggedMessage = "fetch Jetpack AI completions"
    ) {
        jetpackAIRestClient.fetchJetpackAICompletions(site, prompt)
    }
}
