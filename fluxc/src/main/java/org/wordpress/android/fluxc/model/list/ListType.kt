package org.wordpress.android.fluxc.model.list

/**
 * This enum indicates the primary type of a list. There should be one to one relationship with this type and either a
 * Store (i.e. PostStore) or a model (i.e. PostModel). Further differentiation between different lists should be done
 * through different `ListDescriptor`s, most likely `ListFilter` instances.
 *
 * Since these values are kept in the DB, they should not be changed without a migration.
 */
enum class ListType(val value: Int) {
    POST(100),
    WOO_ORDER(101);

    companion object {
        // If the type is missing we want the app to crash so we can fix it immediately
        fun fromValue(value: Int?): ListType = ListType.values().firstOrNull { it.value == value }!!
    }
}
