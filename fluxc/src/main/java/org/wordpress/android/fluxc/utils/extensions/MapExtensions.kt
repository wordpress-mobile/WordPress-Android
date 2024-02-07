package org.wordpress.android.fluxc.utils.extensions

@Suppress("UNCHECKED_CAST")
fun <K, V : Any> Map<out K, V?>.filterNotNull(): Map<K, V> = filterValues { it != null } as Map<K, V>
