package org.wordpress.android.ui.sitecreation.domains

import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.RobolectricSetupTest
import org.wordpress.android.util.UrlUtilsWrapper

class SiteCreationDomainValidatorTest : RobolectricSetupTest() {
    private val domainResolver = SiteCreationDomainValidator(
            UrlUtilsWrapper(),
            PublicSuffixDatabaseWrapper()
    )

    @Test
    fun validateDomains() {
        assertTrue(domainResolver.validateDomain("http://www.wordpress.com").isDomainValid)
        assertTrue(domainResolver.validateDomain("johnbrown").host == "johnbrown.wordpress.com")
        assertTrue(domainResolver.validateDomain("https://www.john.home.blog").host == "john.home.blog")
        assertTrue(domainResolver.validateDomain("johnbrown.com").isDomainValid)
        assertTrue(domainResolver.validateDomain("my.square.com").isDomainValid)

        assertFalse(domainResolver.validateDomain("johnbrown&").isDomainValid)
        assertFalse(domainResolver.validateDomain("john_brown.com").isDomainValid)
        assertFalse(domainResolver.validateDomain("http://johnbrown&").isDomainValid)
    }
}
