package org.wordpress.android.ui.posts

import org.junit.Assert.*
import org.junit.Before
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.viewmodel.ResourceProvider

class EditPostPublishedSettingsViewModelTest : BaseUnitTest() {
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var postSettingsUtils: PostSettingsUtils
    private lateinit var viewModel: EditPostPublishedSettingsViewModel
    @Before
    fun setUp() {
        viewModel = EditPostPublishedSettingsViewModel(resourceProvider, postSettingsUtils)
    }
}