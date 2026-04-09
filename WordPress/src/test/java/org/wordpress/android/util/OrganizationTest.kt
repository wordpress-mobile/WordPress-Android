package org.wordpress.android.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.Organization

class OrganizationTest {
    @Test
    fun `fromOrgId returns correct Organization`() {
        assertThat(Organization.fromOrgId(0)).isEqualTo(Organization.NO_ORGANIZATION)
        assertThat(Organization.fromOrgId(1)).isEqualTo(Organization.A8C)
        assertThat(Organization.fromOrgId(2)).isEqualTo(Organization.P2)
        assertThat(Organization.fromOrgId(3)).isEqualTo(Organization.UNKNOWN)
    }
}
