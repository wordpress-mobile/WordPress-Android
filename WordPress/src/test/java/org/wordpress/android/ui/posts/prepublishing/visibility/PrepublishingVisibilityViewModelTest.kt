package org.wordpress.android.ui.posts.prepublishing.visibility

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.posts.EditPostRepository

class PrepublishingVisibilityViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: PrepublishingVisibilityViewModel

    @Mock lateinit var getPostVisibilityUseCase: GetPostVisibilityUseCase
    @Mock lateinit var updatePostPasswordUseCase: UpdatePostPasswordUseCase
    @Mock lateinit var updatePostStatusUseCase: UpdatePostStatusUseCase
    @Mock lateinit var editPostRepository: EditPostRepository

    @Before
    fun setup() {
        viewModel = PrepublishingVisibilityViewModel(
                getPostVisibilityUseCase,
                updatePostPasswordUseCase,
                updatePostStatusUseCase
        )
    }

    @Test
    fun test() {
        viewModel.start(editPostRepository)
    }
}
