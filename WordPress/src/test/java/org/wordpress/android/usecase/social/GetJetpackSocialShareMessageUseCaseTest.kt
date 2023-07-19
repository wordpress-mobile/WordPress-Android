package org.wordpress.android.usecase.social

import junit.framework.TestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.store.PostStore

@ExperimentalCoroutinesApi
class GetJetpackSocialShareMessageUseCaseTest : BaseUnitTest() {
    private val postStore: PostStore = mock()
    private val classToTest = GetJetpackSocialShareMessageUseCase(
        ioDispatcher = testDispatcher(),
        postStore = postStore,
    )

    @Test
    fun `Should return empty string if post is not found`() = test {
        val localPostId = 123
        whenever(postStore.getPostByLocalPostId(localPostId)).thenReturn(null)
        val expected = ""
        val actual = classToTest.execute(localPostId)
        TestCase.assertEquals(expected, actual)
    }

    @Test
    fun `Should return title if autoShareMessage is null`() = test {
        val postTitle = "title"
        val localPostId = 123
        val postModel = PostModel().apply {
            setAutoShareMessage(null)
            setTitle(postTitle)
        }
        whenever(postStore.getPostByLocalPostId(localPostId)).thenReturn(postModel)
        val expected = postTitle
        val actual = classToTest.execute(localPostId)
        TestCase.assertEquals(expected, actual)
    }

    @Test
    fun `Should return title if autoShareMessage is empty`() = test {
        val postTitle = "title"
        val localPostId = 123
        val postModel = PostModel().apply {
            setAutoShareMessage("")
            setTitle(postTitle)
        }
        whenever(postStore.getPostByLocalPostId(localPostId)).thenReturn(postModel)
        val expected = postTitle
        val actual = classToTest.execute(localPostId)
        TestCase.assertEquals(expected, actual)
    }

    @Test
    fun `Should return autoShareMessage if autoShareMessage is NOT null or empty`() = test {
        val message = "Share message"
        val localPostId = 123
        val postModel = PostModel().apply {
            setAutoShareMessage(message)
        }
        whenever(postStore.getPostByLocalPostId(localPostId)).thenReturn(postModel)
        val expected = message
        val actual = classToTest.execute(localPostId)
        TestCase.assertEquals(expected, actual)
    }
}
