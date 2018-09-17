package org.wordpress.android.util

inline fun <T1, T2> ifNotNull(value1: T1?, value2: T2?, bothNotNull: (T1, T2) -> (Unit)) {
    if (value1 != null && value2 != null) {
        bothNotNull(value1, value2)
    }
}

inline fun <T1, T2, T3> ifNotNull(value1: T1?, value2: T2?, value3: T3?, allNotNull: (T1, T2, T3) -> (Unit)) {
    if (value1 != null && value2 != null && value3 != null) {
        allNotNull(value1, value2, value3)
    }
}

inline fun <T1, T2, T3, T4> ifNotNull(
    value1: T1?,
    value2: T2?,
    value3: T3?,
    value4: T4?,
    allNotNull: (T1, T2, T3, T4) -> (Unit)
) {
    if (value1 != null && value2 != null && value3 != null && value4 != null) {
        allNotNull(value1, value2, value3, value4)
    }
}
