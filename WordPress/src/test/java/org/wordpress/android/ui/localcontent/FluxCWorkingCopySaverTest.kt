package org.wordpress.android.ui.localcontent

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import uniffi.wp_api.PostFormat
import uniffi.wp_mobile.LocalContentService
import uniffi.wp_mobile_local_content.FeaturedImage
import uniffi.wp_mobile_local_content.PostWorkingCopy
import uniffi.wp_mobile_local_content.PostWorkingCopyFields
import uniffi.wp_mobile_local_content.PublishDate
import uniffi.wp_mobile_local_content.RemotePostReference
import uniffi.wp_mobile_local_content.SiteIdentity
import uniffi.wp_mobile_local_content.StatusSetting
import uniffi.wp_mobile_local_content.WorkingCopyCommonFields
import uniffi.wp_mobile_local_content.WorkingCopyRemoteTarget
import java.util.Date

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class FluxCWorkingCopySaverTest : BaseUnitTest() {
    @Mock
    lateinit var context: Context

    @Mock
    lateinit var preferences: SharedPreferences

    @Mock
    lateinit var editor: SharedPreferences.Editor

    @Mock
    lateinit var experimentalFeatures: ExperimentalFeatures

    @Mock
    lateinit var serviceProvider: LocalContentServiceProvider

    @Mock
    lateinit var service: LocalContentService

    @Mock
    lateinit var mapper: FluxCWorkingCopyMapper

    @Mock
    lateinit var appLogWrapper: AppLogWrapper

    private lateinit var saver: FluxCWorkingCopySaver

    /** The saver's shared preferences: FluxC local post ID to working copy ID. */
    private val workingCopyIds = mutableMapOf<String, String>()

    private val site = SiteModel()
    private val siteIdentity = SiteIdentity(url = mock(), wordpressComSiteId = null)
    private val remotePost = RemotePostReference(postId = REMOTE_POST_ID, baseModifiedGmt = Date(1_900_000_000_000L))
    private val fields = PostWorkingCopyFields(
        common = WorkingCopyCommonFields(
            title = "Title",
            content = "",
            excerpt = "",
            slug = null,
            status = StatusSetting.Draft(password = null),
            publishDate = PublishDate.Immediately,
            author = null,
            featuredImage = FeaturedImage.None,
            commentStatus = null,
            pingStatus = null
        ),
        categories = emptyList(),
        tags = emptyList(),
        format = PostFormat.Standard,
        sticky = false
    )

    @Before
    fun setUp() {
        whenever(context.getSharedPreferences(any(), any())).thenReturn(preferences)
        whenever(preferences.getString(any(), anyOrNull())).thenAnswer { workingCopyIds[it.getArgument(0)] }
        whenever(preferences.edit()).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenAnswer {
            workingCopyIds[it.getArgument(0)] = it.getArgument(1)
            editor
        }
        whenever(experimentalFeatures.isEnabled(Feature.LOCAL_WORKING_COPIES)).thenReturn(true)
        whenever(serviceProvider.getService()).thenReturn(service)
        whenever(mapper.toSiteIdentity(site)).thenReturn(siteIdentity)
        whenever(mapper.toPostFields(any())).thenReturn(fields)

        saver = FluxCWorkingCopySaver(
            context,
            experimentalFeatures,
            serviceProvider,
            mapper,
            appLogWrapper,
            testScope(),
            testDispatcher()
        )
    }

    @Test
    fun `a post that isn't on the server gets a working copy with no remote post`() {
        val post = post(remotePostId = 0)
        whenever(mapper.toRemotePostReference(post)).thenReturn(null)
        val newPost = WorkingCopyRemoteTarget(site = siteIdentity, remotePost = null)
        whenever(service.createPostWorkingCopy(newPost, fields)).thenReturn(workingCopy(newPost))

        saver.save(post, site)

        verify(service).createPostWorkingCopy(newPost, fields)
        assertThat(workingCopyIds).containsEntry(LOCAL_POST_ID.toString(), WORKING_COPY_ID)
    }

    @Test
    fun `a post on the server with no working copy gets one that points at its remote post`() {
        val post = post(remotePostId = REMOTE_POST_ID)
        whenever(mapper.toRemotePostReference(post)).thenReturn(remotePost)
        whenever(service.readPostWorkingCopyForRemotePost(siteIdentity, REMOTE_POST_ID)).thenReturn(null)
        val target = WorkingCopyRemoteTarget(site = siteIdentity, remotePost = remotePost)
        whenever(service.createPostWorkingCopy(target, fields)).thenReturn(workingCopy(target))

        saver.save(post, site)

        verify(service).createPostWorkingCopy(target, fields)
        assertThat(workingCopyIds).containsEntry(LOCAL_POST_ID.toString(), WORKING_COPY_ID)
    }

    @Test
    fun `a post FluxC uploaded after its working copy was created records the upload as a push`() {
        workingCopyIds[LOCAL_POST_ID.toString()] = WORKING_COPY_ID
        val post = post(remotePostId = REMOTE_POST_ID)
        whenever(mapper.toRemotePostReference(post)).thenReturn(remotePost)
        whenever(service.readPostWorkingCopy(WORKING_COPY_ID))
            .thenReturn(workingCopy(WorkingCopyRemoteTarget(site = siteIdentity, remotePost = null)))

        saver.save(post, site)

        verify(service).savePostWorkingCopy(WORKING_COPY_ID, fields)
        verify(service).recordPostWorkingCopyPush(WORKING_COPY_ID, remotePost)
        verify(service, never()).createPostWorkingCopy(any(), any())
    }

    @Test
    fun `a post whose working copy already has its remote post records no push`() {
        workingCopyIds[LOCAL_POST_ID.toString()] = WORKING_COPY_ID
        val post = post(remotePostId = REMOTE_POST_ID)
        whenever(mapper.toRemotePostReference(post)).thenReturn(remotePost)
        whenever(service.readPostWorkingCopy(WORKING_COPY_ID))
            .thenReturn(workingCopy(WorkingCopyRemoteTarget(site = siteIdentity, remotePost = remotePost)))

        saver.save(post, site)

        verify(service).savePostWorkingCopy(WORKING_COPY_ID, fields)
        verify(service, never()).recordPostWorkingCopyPush(any(), any())
    }

    @Test
    fun `a post with no stored working copy ID finds its working copy by its remote post`() {
        val post = post(remotePostId = REMOTE_POST_ID)
        whenever(mapper.toRemotePostReference(post)).thenReturn(remotePost)
        whenever(service.readPostWorkingCopyForRemotePost(siteIdentity, REMOTE_POST_ID))
            .thenReturn(workingCopy(WorkingCopyRemoteTarget(site = siteIdentity, remotePost = remotePost)))

        saver.save(post, site)

        verify(service).savePostWorkingCopy(WORKING_COPY_ID, fields)
        verify(service, never()).createPostWorkingCopy(any(), any())
        assertThat(workingCopyIds).containsEntry(LOCAL_POST_ID.toString(), WORKING_COPY_ID)
    }

    private fun post(remotePostId: Long) = PostModel().apply {
        setId(LOCAL_POST_ID)
        setRemotePostId(remotePostId)
    }

    private fun workingCopy(remoteTarget: WorkingCopyRemoteTarget) = PostWorkingCopy(
        id = WORKING_COPY_ID,
        fields = fields,
        remoteTarget = remoteTarget,
        createdAt = "2026-11-04T00:00:00Z",
        updatedAt = "2026-11-04T00:00:00Z"
    )

    companion object {
        private const val LOCAL_POST_ID = 1
        private const val REMOTE_POST_ID = 2007L
        private const val WORKING_COPY_ID = "8a6e0804-2bd0-4672-b79d-d97027f3b0d3"
    }
}
