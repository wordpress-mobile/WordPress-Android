package org.wordpress.android.ui.main.utils

import com.gravatar.BuildConfig as GravatarBuildConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.AppLog.T
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

/**
 * Where to send a user who asked to edit their Gravatar profile.
 */
enum class GravatarEditorTarget {
    /** The in-app Quick Editor sheet, i.e. the normal path. */
    QUICK_EDITOR,

    /** Gravatar on the web, because the sheet would only show its unrecoverable error card. */
    WEB_EDITOR
}

/**
 * Builds the client used to probe Gravatar.
 *
 * A throwaway client per probe is deliberate: the SDK's `GravatarSdkContainer` builds a new
 * [OkHttpClient] for every profile request, so it never reuses a connection. Sharing one here would
 * hand the probe a warm connection the SDK itself never gets, and the probe would stop predicting
 * what the SDK is about to experience.
 */
class GravatarProbeClientFactory @Inject constructor() {
    /** Returns the [Call.Factory] interface rather than the concrete client, so tests can stub it. */
    fun create(): Call.Factory = OkHttpClient.Builder()
        .callTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val PROBE_TIMEOUT_SECONDS = 4L
    }
}

/**
 * Works out whether the Gravatar Quick Editor is going to work, before we show it.
 *
 * Context (CMM-1100): `GravatarQuickEditor.show()` takes an `updateHandler` and an `onDismiss` but
 * has no error callback, so when its one network call is refused the user is stranded on an
 * unrecoverable "Ooops / Something went terribly wrong." card with a Try again button that never
 * recovers. The SDK also drops the status code before anything can log it, which is why the issue
 * went months without a diagnosis. This runs the request the SDK is about to run, so the caller can
 * send the user to the web editor instead and so the real status code reaches our logs.
 *
 * The probe only predicts the SDK's result if it looks the same on the wire, so it mirrors the SDK
 * deliberately: a fresh client per call, the same headers, and OkHttp's default User-Agent. Do not
 * swap in the app's shared OkHttp client — that one sends `wp-android/...` as its User-Agent, reuses
 * connections and carries our interceptors, which are exactly the properties the block is suspected
 * of keying on, so it could well succeed while the SDK is still refused. Re-check this against the
 * SDK's `SdkVersionInterceptor` whenever the gravatar dependency moves.
 *
 * One divergence is deliberate: the probe caps the whole call at [GravatarProbeClientFactory]'s
 * timeout, where the SDK leaves OkHttp's 10s-per-phase defaults. A refusal that arrives later than
 * that is missed and we fall open to the sheet, which is the pre-existing behaviour anyway.
 */
class GravatarEditorTargetResolver @Inject constructor(
    private val clientFactory: GravatarProbeClientFactory,
    private val appLogWrapper: AppLogWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) {
    /**
     * Returns [GravatarEditorTarget.WEB_EDITOR] only when Gravatar refused the request in a way the
     * sheet cannot recover from. The question is not "did this fail?" but "can the sheet do anything
     * useful with this failure?", which is why 401 and 5xx stay on the sheet while 403 and 429 do not.
     *
     * Anything we cannot judge — no token, no connectivity, a timeout — also falls open to
     * [GravatarEditorTarget.QUICK_EDITOR] so a flaky network never blocks a working flow. The sheet
     * has its own "no internet" card, which is better than silently diverting the user to a browser.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun resolve(accessToken: String): GravatarEditorTarget = withContext(bgDispatcher) {
        if (accessToken.isEmpty()) return@withContext GravatarEditorTarget.QUICK_EDITOR
        try {
            clientFactory.create().newCall(buildRequest(accessToken)).execute().use { it.toTarget() }
        } catch (e: Exception) {
            appLogWrapper.d(T.MAIN, "Gravatar: profile probe could not complete (${e.javaClass.simpleName})")
            GravatarEditorTarget.QUICK_EDITOR
        }
    }

    private fun buildRequest(accessToken: String): Request = Request.Builder()
        .url(PROFILE_URL)
        .header("Authorization", "Bearer $accessToken")
        .header("Accept", "application/json")
        .header("X-Platform", "Android")
        .header("X-SDK-Version", GravatarBuildConfig.SDK_VERSION)
        .build()

    private fun Response.toTarget(): GravatarEditorTarget = when {
        // A 2xx the SDK cannot parse strands the user just as a refusal does: its ApiClient only
        // handles JSON here and throws on anything else, which surfaces as the same "Ooops" card. An
        // absent Content-Type is fine — the SDK treats that as JSON too.
        isSuccessful -> if (hasJsonContentType()) {
            GravatarEditorTarget.QUICK_EDITOR
        } else {
            appLogWrapper.w(
                T.MAIN,
                "Gravatar: profile probe got an unparseable $code, using the web editor. ${describe()}"
            )
            GravatarEditorTarget.WEB_EDITOR
        }

        // The sheet shows its own "Session expired" card for 401. In Bearer mode — what MeFragment
        // passes — that card only offers Close, so there is no in-app recovery, but a 401 needs a
        // token Gravatar rejects that the app still believes in, and a dead WP.com token normally
        // trips the re-auth flow elsewhere first. Narrow enough to leave to the sheet's own messaging.
        code == HTTP_UNAUTHORIZED -> {
            appLogWrapper.d(T.MAIN, "Gravatar: profile probe got 401, letting the editor handle the session")
            GravatarEditorTarget.QUICK_EDITOR
        }

        // The SDK maps 5xx to a "couldn't connect to Gravatar servers" card whose Try again actually
        // re-runs the fetch, so a blip is recoverable in place. Diverting would trade that for a
        // browser that asks the user to log in to WordPress.com again.
        code in SERVER_ERRORS -> {
            appLogWrapper.w(T.MAIN, "Gravatar: profile probe got $code, letting the editor offer its retry")
            GravatarEditorTarget.QUICK_EDITOR
        }

        else -> {
            appLogWrapper.w(
                T.MAIN,
                "Gravatar: profile probe refused with $code, using the web editor. ${describe()}"
            )
            GravatarEditorTarget.WEB_EDITOR
        }
    }

    /**
     * Mirrors the SDK's own check in `ApiClient.responseBody`: an absent Content-Type is parsed as
     * JSON, and otherwise only `application/...json` is. Deliberately the same shape, including the
     * lack of trimming, so the probe agrees with the SDK on borderline values.
     */
    private fun Response.hasJsonContentType(): Boolean {
        val mediaType = header("Content-Type")?.substringBefore(";")?.lowercase(Locale.US)
        return mediaType == null ||
            (mediaType.startsWith(JSON_TYPE_PREFIX) && mediaType.endsWith(JSON_TYPE_SUFFIX))
    }

    /** Content type plus a short body excerpt — the diagnostic the SDK throws away. */
    private fun Response.describe(): String {
        val contentType = header("Content-Type").orEmpty()
        val body = runCatching { peekBody(MAX_LOGGED_BODY_BYTES).string() }.getOrDefault("")
        return "type=$contentType body=${body.replace(WHITESPACE, " ").trim()}"
    }

    companion object {
        private val WHITESPACE = Regex("\\s+")
        private val SERVER_ERRORS = 500..599
        private const val PROFILE_URL = "https://api.gravatar.com/v3/me/profile"
        private const val HTTP_UNAUTHORIZED = 401
        private const val MAX_LOGGED_BODY_BYTES = 512L
        private const val JSON_TYPE_PREFIX = "application/"
        private const val JSON_TYPE_SUFFIX = "json"
    }
}
