package org.wordpress.android.ui.newstats.repository

import org.wordpress.android.ui.newstats.StatsPeriod
import java.time.LocalDate
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

private const val DEFAULT_CAPACITY = 12
private const val WIDE_CAPACITY = 6
private const val LOAD_FACTOR = 0.75f

/**
 * The endpoint a cache entry belongs to. Each bucket is bounded on its own so one chatty endpoint
 * can't evict every other card's entries: selecting a chart bar reloads every card for the bar's
 * own sub-period, so a scrub across a month's chart costs one entry per bar per card.
 *
 * [WIDE_CAPACITY] is for the two endpoints that request unbounded lists (`max = 0`): the referrers
 * detail screen (groups with nested children) and UTM (which also carries a per-post map).
 */
enum class StatsCacheBucket(val capacity: Int) {
    CHART(DEFAULT_CAPACITY),
    BOTTOM_STATS(DEFAULT_CAPACITY),
    MOST_VIEWED(DEFAULT_CAPACITY),
    REFERRERS_DETAIL(WIDE_CAPACITY),
    COUNTRIES(DEFAULT_CAPACITY),
    REGIONS(DEFAULT_CAPACITY),
    CITIES(DEFAULT_CAPACITY),
    AUTHORS(DEFAULT_CAPACITY),
    CLICKS(DEFAULT_CAPACITY),
    SEARCH_TERMS(DEFAULT_CAPACITY),
    VIDEO_PLAYS(DEFAULT_CAPACITY),
    FILE_DOWNLOADS(DEFAULT_CAPACITY),
    DEVICES_SCREENSIZE(DEFAULT_CAPACITY),
    DEVICES_BROWSER(DEFAULT_CAPACITY),
    DEVICES_PLATFORM(DEFAULT_CAPACITY),
    UTM(WIDE_CAPACITY),
    TODAY_AGGREGATES(DEFAULT_CAPACITY),
    HOURLY_VIEWS(DEFAULT_CAPACITY)
}

/**
 * Identifies one cached stats result.
 *
 * [today] is part of the key on purpose: every preset resolves its window against the clock, so an
 * entry stored yesterday — or in another timezone, which `SystemDefaultZoneClock` re-reads on every
 * call — must never be served as today's "Last 7 days". A day rollover simply misses on every key.
 *
 * [variant] carries whatever else distinguishes the request within a bucket (the most-viewed data
 * source, the UTM keys, the hourly day offset). The invariant to preserve: within one bucket, the
 * key must fully determine the request that was sent.
 */
data class StatsCacheKey(
    val bucket: StatsCacheBucket,
    val siteId: Long,
    val today: LocalDate,
    val period: StatsPeriod? = null,
    val variant: String? = null
)

/**
 * In-memory store for recently viewed stats results, so paging back to a period just visited
 * renders from memory instead of the network (CMM-2473).
 *
 * Entries have no expiry: they live until pull-to-refresh clears them or they are evicted as the
 * least recently used of their bucket. Freshness comes from [markAllStale] instead — the stats
 * screen marks every entry on entry, and the first card to serve a stale entry renders it
 * immediately and then refreshes it in the background.
 *
 * A `@Singleton`, because `StatsRepository` is unscoped: every card view model (and every detail
 * screen) gets its own repository instance and they must all share one store.
 */
@Singleton
class StatsResultCache @Inject constructor() {
    private class Entry(val value: Any, var isRevalidated: Boolean)

    private val buckets: Map<StatsCacheBucket, MutableMap<StatsCacheKey, Entry>> =
        StatsCacheBucket.entries.associateWith { newLruMap(it.capacity) }

    /** The cached result for [key], or null when there is none. */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: StatsCacheKey): T? {
        val entry = buckets.getValue(key.bucket)[key] ?: return null
        return entry.value as T
    }

    /** Stores [value]. It came off the network, so it counts as revalidated for this session. */
    fun put(key: StatsCacheKey, value: Any) {
        buckets.getValue(key.bucket)[key] = Entry(value, isRevalidated = true)
    }

    /**
     * Whether [key] can be served from memory. Used before a fetch to decide whether a loading
     * state is needed at all — a cache hit must not flash the card's placeholder.
     */
    fun isCached(key: StatsCacheKey): Boolean = buckets.getValue(key.bucket).containsKey(key)

    /**
     * Whether [key] holds a value from an earlier session that hasn't been refreshed yet. Checked
     * after serving it, so the card can quietly re-fetch once without a loading state.
     */
    fun needsRevalidation(key: StatsCacheKey): Boolean =
        buckets.getValue(key.bucket)[key]?.isRevalidated == false

    /**
     * Marks every entry as needing one background refresh, keeping its value. Called when the stats
     * screen is entered: what's on screen stays instant, and each card refreshes itself once.
     */
    fun markAllStale() {
        buckets.values.forEach { bucket ->
            // A synchronizedMap's own monitor guards it, and iterating one is only safe while held.
            synchronized(bucket) {
                bucket.values.forEach { it.isRevalidated = false }
            }
        }
    }

    /**
     * Drops every entry. Deliberately not a suspending function: pull-to-refresh has to be able to
     * empty the cache before it dispatches the refetches, and a coroutine hop there would race them.
     */
    fun clear() {
        buckets.values.forEach { it.clear() }
    }

    /**
     * A bounded, access-ordered map: reading an entry makes it the most recently used, so the entry
     * evicted is always the one not looked at for longest. Same shape as `RsSiteRestClient`'s media
     * cache. The `synchronizedMap` wrapper is required rather than optional, because with access
     * ordering even a read mutates the map.
     */
    private fun newLruMap(capacity: Int): MutableMap<StatsCacheKey, Entry> =
        Collections.synchronizedMap(
            object : LinkedHashMap<StatsCacheKey, Entry>(capacity, LOAD_FACTOR, true) {
                override fun removeEldestEntry(
                    eldest: MutableMap.MutableEntry<StatsCacheKey, Entry>
                ): Boolean = size > capacity
            }
        )
}
