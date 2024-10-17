package org.wordpress.android.fluxc.utils.extensions

@Suppress("UNCHECKED_CAST")
fun <K, V : Any> Map<out K, V?>.filterNotNull(): Map<K, V> =
    filterValues { it != null } as Map<K, V>

fun <T, K> MutableMap<T, K>.putIfNotNull(vararg pairs: Pair<T, K?>) = apply {
    pairs.forEach { pair ->
        pair.second?.let { put(pair.first, it) }
    }
}
