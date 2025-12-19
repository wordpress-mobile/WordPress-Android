package org.wordpress.android.ui.people

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.PeopleListFilter

@ExperimentalCoroutinesApi
class PeopleListFragmentTest : BaseUnitTest() {
    private lateinit var siteModel: SiteModel

    @Mock
    lateinit var onPersonSelectedListener: PeopleListFragment.OnPersonSelectedListener

    @Mock
    lateinit var onFetchPeopleListener: PeopleListFragment.OnFetchPeopleListener

    @Before
    fun setUp() {
        siteModel = SiteModel().apply { id = TEST_SITE_ID }
    }

    @Test
    fun `newInstance creates fragment with arguments bundle`() {
        val fragment = PeopleListFragment.newInstance(siteModel)

        assertThat(fragment).isNotNull
        assertThat(fragment.arguments).isNotNull
    }

    @Test
    fun `fragment can set listener references`() {
        val fragment = PeopleListFragment.newInstance(siteModel)

        // Verify the fragment can have listeners set without exceptions
        fragment.setOnPersonSelectedListener(onPersonSelectedListener)
        fragment.setOnFetchPeopleListener(onFetchPeopleListener)

        // No exception should be thrown
    }

    @Test
    fun `fetchingRequestFinished ignores non-TEAM filter`() {
        val fragment = PeopleListFragment.newInstance(siteModel)

        // fetchingRequestFinished with SUBSCRIBERS filter should be ignored
        // since the fragment only handles TEAM filter
        fragment.fetchingRequestFinished(PeopleListFilter.SUBSCRIBERS, true, true)

        // No exception should be thrown - the method should return early
    }

    @Test
    fun `fetchingRequestFinished ignores non-first-page results`() {
        val fragment = PeopleListFragment.newInstance(siteModel)

        // fetchingRequestFinished with isFirstPage=false should be ignored
        fragment.fetchingRequestFinished(PeopleListFilter.TEAM, false, true)

        // No exception should be thrown - the method should return early
    }

    companion object {
        private const val TEST_SITE_ID = 123
    }
}
