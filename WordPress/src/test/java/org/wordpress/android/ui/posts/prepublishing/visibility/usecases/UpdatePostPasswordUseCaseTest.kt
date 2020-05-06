package org.wordpress.android.ui.posts.prepublishing.visibility.usecases

import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.EditPostRepository

class UpdatePostPasswordUseCaseTest : BaseUnitTest() {
    lateinit var editPostRepository: EditPostRepository
    lateinit var updatePostPasswordUseCase: UpdatePostPasswordUseCase

    @InternalCoroutinesApi
    @Before
    fun setup() {
        updatePostPasswordUseCase = UpdatePostPasswordUseCase()
        editPostRepository = EditPostRepository(mock(), mock(), mock(), TEST_DISPATCHER, TEST_DISPATCHER)
        editPostRepository.set { PostModel() }
    }

    @Test
    fun `verify that when updatePassword is called onPostPasswordUpdated is invoked`() {
        // arrange
        var passwordUpdated = false

        // act
        updatePostPasswordUseCase.updatePassword("password", editPostRepository) {
            passwordUpdated = true
        }

        // assert
        assertThat(passwordUpdated).isTrue()
    }

    @Test
    fun `verify that when updatePassword is called the post password is updated`() {
        // arrange
        val password = "password"

        // act
        updatePostPasswordUseCase.updatePassword(password, editPostRepository) {}

        // assert
        assertThat(editPostRepository.getPost()?.password).isEqualTo(password)
    }
}
