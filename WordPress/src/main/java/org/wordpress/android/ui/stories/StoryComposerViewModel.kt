package org.wordpress.android.ui.stories

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.push.NotificationType
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.PostEditorAnalyticsSession
import org.wordpress.android.ui.posts.PostEditorAnalyticsSession.Outcome.CANCEL
import org.wordpress.android.ui.posts.SavePostToDbUseCase
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import javax.inject.Inject

class StoryComposerViewModel @Inject constructor(
    private val systemNotificationsTracker: SystemNotificationsTracker,
    private val saveInitialPostUseCase: SaveInitialPostUseCase,
    private val savePostToDbUseCase: SavePostToDbUseCase,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    private val dispatcher: Dispatcher
) : ViewModel(), LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    private lateinit var editPostRepository: EditPostRepository
    private lateinit var site: SiteModel
    private lateinit var postEditorAnalyticsSession: PostEditorAnalyticsSession

    init {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun start(
        site: SiteModel,
        editPostRepository: EditPostRepository,
        postId: LocalId,
        intent: Intent,
        postEditorAnalyticsSession: PostEditorAnalyticsSession?,
        notificationType: NotificationType?
    ) {
        this.editPostRepository = editPostRepository
        this.site = site

        if (postId.value == 0) {
            // Create a new post
            saveInitialPostUseCase.saveInitialPost(editPostRepository, site)
            // Bump post created analytics only once, first time the editor is opened
            analyticsUtilsWrapper.trackEditorCreatedPost(
                    intent.action,
                    intent,
                    site,
                    editPostRepository.getPost()
            )
        } else {
            editPostRepository.loadPostByLocalPostId(postId.value)
        }

        setupPostEditorAnalyticsSession(postEditorAnalyticsSession)

        notificationType?.let {
            systemNotificationsTracker.trackTappedNotification(it)
        }

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        updateStoryPostWithChanges()
    }

    private fun setupPostEditorAnalyticsSession(postEditorAnalyticsSession: PostEditorAnalyticsSession?) {
        this.postEditorAnalyticsSession = postEditorAnalyticsSession ?: createPostEditorAnalyticsSessionTracker(
                editPostRepository.getPost(),
                site
        )
        this.postEditorAnalyticsSession.start(null)
    }

    private fun createPostEditorAnalyticsSessionTracker(
        post: PostImmutableModel?,
        site: SiteModel?
    ): PostEditorAnalyticsSession {
        return PostEditorAnalyticsSession.getNewPostEditorAnalyticsSession(
                PostEditorAnalyticsSession.Editor.WP_STORIES_CREATOR,
                post, site, true
        )
    }

    fun writeToBundle(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, site)
        outState.putInt(StoryComposerActivity.STATE_KEY_POST_LOCAL_ID, editPostRepository.id)
        outState.putSerializable(StoryComposerActivity.STATE_KEY_EDITOR_SESSION_DATA, postEditorAnalyticsSession)
    }

    fun onStoryDiscarded() {
        // delete empty post from database
        dispatcher.dispatch(PostActionBuilder.newRemovePostAction(editPostRepository.getEditablePost()))
        postEditorAnalyticsSession.setOutcome(CANCEL)
    }

    private fun updateStoryPostWithChanges() {
        editPostRepository.postChanged.observe(this, Observer {
            savePostToDbUseCase.savePostToDb(editPostRepository, site)
        })
    }

    override fun onCleared() {
        super.onCleared()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        postEditorAnalyticsSession.end()
    }
}
