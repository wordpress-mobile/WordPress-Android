package org.wordpress.android.ui

enum class Organization(val orgId: Int) {
    UNKNOWN(-1),
    NO_ORGANIZATION(0),
    A8C(1),
    P2(2);

    companion object {
        private val map = values().associateBy(Organization::orgId)

        @JvmStatic
        fun fromOrgId(orgId: Int) = map[orgId] ?: UNKNOWN
    }
}
