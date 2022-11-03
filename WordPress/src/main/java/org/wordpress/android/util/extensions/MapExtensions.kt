package org.wordpress.android.util.extensions

@Suppress("UNCHECKED_CAST")
fun <K, V> Map<K?, V?>.filterNull(): Map<K, V> =
        (filterValues { it != null } as Map<K?, V>)
                .filterKeys { it != null } as Map<K, V>
