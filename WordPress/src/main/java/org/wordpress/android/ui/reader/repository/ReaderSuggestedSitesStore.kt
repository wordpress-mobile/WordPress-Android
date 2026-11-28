package org.wordpress.android.ui.reader.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.modules.APPLICATION_SCOPE
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/** A suggested site, and whether the reader has followed it from the suggestions */
data class ReaderSuggestedSiteItem(
    val site: ReaderSuggestedSite,
    val isFollowing: Boolean = false,
    val isFollowEnabled: Boolean = true,
) {
    val key: String get() = "${site.blogId}:${site.feedId}:${site.url}"
}

/**
 * The sites WordPress.com suggests to this reader, shared by everywhere the Reader offers them (the empty
 * Subscriptions feed, the end of a post, a site preview after subscribing). Fetched once and kept for a while, so
 * each place shows the same sites without asking again, and a site followed in one place shows as followed in all.
 */
@Singleton
class ReaderSuggestedSitesStore @Inject constructor(
    private val blogRepository: ReaderBlogRepository,
    private val tagsAndSitesUpdater: ReaderTagsAndSitesUpdater,
    private val accountStore: AccountStore,
    @Named(APPLICATION_SCOPE) private val applicationScope: CoroutineScope,
) {
    private val _sites = MutableStateFlow<List<ReaderSuggestedSiteItem>>(emptyList())
    val sites: StateFlow<List<ReaderSuggestedSiteItem>> = _sites.asStateFlow()

    private var fetchedAt = 0L
    private var fetchJob: Job? = null

    /** Fetches the suggestions unless they're fresh or being fetched; only WordPress.com accounts get any */
    fun refreshIfStale() {
        if (!accountStore.hasAccessToken() || fetchJob?.isActive == true) return
        val isFresh = System.currentTimeMillis() - fetchedAt < STALE_AFTER_MS
        if (isFresh && _sites.value.isNotEmpty()) return
        fetchJob = applicationScope.launch {
            val fetched = blogRepository.fetchSuggestedSites()
            fetchedAt = System.currentTimeMillis()
            // a failed fetch keeps what's showing
            if (fetched.isNotEmpty()) _sites.value = fetched.map { ReaderSuggestedSiteItem(it) }
        }
    }

    /** Runs in the app scope, so leaving the screen doesn't lose the follow */
    fun toggleFollow(item: ReaderSuggestedSiteItem, source: String) {
        val follow = !item.isFollowing
        updateItem(item.key) { it.copy(isFollowing = follow, isFollowEnabled = false) }
        applicationScope.launch {
            val site = item.site
            val succeeded = blogRepository.followSite(site.blogId, site.feedId, follow, source)
            updateItem(item.key) { it.copy(isFollowing = if (succeeded) follow else !follow, isFollowEnabled = true) }
            // the Subscriptions feed and its filters list the followed sites
            if (succeeded) tagsAndSitesUpdater.update(setOf(UpdateTask.TAGS, UpdateTask.FOLLOWED_BLOGS))
        }
    }

    private fun updateItem(key: String, transform: (ReaderSuggestedSiteItem) -> ReaderSuggestedSiteItem) =
        _sites.update { items -> items.map { if (it.key == key) transform(it) else it } }

    companion object {
        private const val STALE_AFTER_MS = 30L * 60 * 1000
    }
}
