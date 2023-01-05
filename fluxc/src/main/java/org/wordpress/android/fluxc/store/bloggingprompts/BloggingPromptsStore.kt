package org.wordpress.android.fluxc.store.bloggingprompts

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsError
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsPayload
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient.BloggingPromptsListResponse
import org.wordpress.android.fluxc.persistence.bloggingprompts.BloggingPromptsDao
import org.wordpress.android.fluxc.store.Store
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BloggingPromptsStore @Inject constructor(
    private val restClient: BloggingPromptsRestClient,
    private val promptsDao: BloggingPromptsDao,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchPrompts(
        site: SiteModel,
        number: Int,
        from: Date
    ): BloggingPromptsResult<List<BloggingPromptModel>> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchPrompts") {
            val payload = restClient.fetchPrompts(site, number, from)
            storePrompts(site, payload)
        }
    }

    private suspend fun storePrompts(
        site: SiteModel,
        payload: BloggingPromptsPayload<BloggingPromptsListResponse>
    ): BloggingPromptsResult<List<BloggingPromptModel>> = when {
        payload.isError -> handlePayloadError(payload.error)
        payload.response != null -> handlePayloadResponse(site, payload.response)
        else -> BloggingPromptsResult(BloggingPromptsError(INVALID_RESPONSE))
    }

    private fun handlePayloadError(
        error: BloggingPromptsError
    ): BloggingPromptsResult<List<BloggingPromptModel>> = when (error.type) {
        AUTHORIZATION_REQUIRED -> {
            promptsDao.clear()
            BloggingPromptsResult()
        }
        else -> BloggingPromptsResult(error)
    }

    fun getPromptForDate(
        site: SiteModel,
        date: Date
    ): Flow<BloggingPromptsResult<BloggingPromptModel>> {
        return promptsDao.getPromptForDate(site.id, date).map { prompts ->
            BloggingPromptsResult(prompts.firstOrNull()?.toBloggingPrompt())
        }
    }

    fun getPromptById(
        site: SiteModel,
        promptId: Int
    ): Flow<BloggingPromptsResult<BloggingPromptModel>> {
        return promptsDao.getPrompt(site.id, promptId).map { prompts ->
            BloggingPromptsResult(prompts.firstOrNull()?.toBloggingPrompt())
        }
    }

    fun getPrompts(site: SiteModel): Flow<BloggingPromptsResult<List<BloggingPromptModel>>> {
        return promptsDao.getAllPrompts(site.id).map { prompts ->
            BloggingPromptsResult(prompts.map { it.toBloggingPrompt() })
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun handlePayloadResponse(
        site: SiteModel,
        response: BloggingPromptsListResponse
    ): BloggingPromptsResult<List<BloggingPromptModel>> = try {
        val prompts = response.toBloggingPrompts()
        promptsDao.insertForSite(site.id, prompts)
        BloggingPromptsResult(prompts)
    } catch (e: Exception) {
        BloggingPromptsResult(BloggingPromptsError(GENERIC_ERROR))
    }

    data class BloggingPromptsResult<T>(
        val model: T? = null,
        val cached: Boolean = false
    ) : Store.OnChanged<BloggingPromptsError>() {
        constructor(error: BloggingPromptsError) : this() {
            this.error = error
        }
    }
}
