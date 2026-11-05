package org.wordpress.android.ui.localcontent

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.AppLog.T
import uniffi.wp_api.ParseUrlException
import uniffi.wp_mobile.LocalContentService
import uniffi.wp_mobile_local_content.LocalContentException
import uniffi.wp_mobile_local_content.PageWorkingCopyFields
import uniffi.wp_mobile_local_content.PostWorkingCopyFields
import uniffi.wp_mobile_local_content.RemotePostReference
import uniffi.wp_mobile_local_content.SiteIdentity
import uniffi.wp_mobile_local_content.WorkingCopyId
import uniffi.wp_mobile_local_content.WorkingCopyRemoteTarget
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Saves a post the editor saves through FluxC to the local content database as well, while the
 * [Feature.LOCAL_WORKING_COPIES] experimental feature is on.
 *
 * A post's working copy is found by the FluxC local post ID it was first saved under, kept in shared
 * preferences, or else by its remote post. A post that isn't on the server yet gets a working copy
 * with no remote post.
 *
 * FluxC uploads the post, not this class. So that the working copy follows FluxC, a save records a
 * push when FluxC has a remote post or `lastModified` that the working copy doesn't have yet.
 */
@Singleton
class FluxCWorkingCopySaver @Inject constructor(
    @ApplicationContext context: Context,
    private val experimentalFeatures: ExperimentalFeatures,
    private val serviceProvider: LocalContentServiceProvider,
    private val mapper: FluxCWorkingCopyMapper,
    private val appLogWrapper: AppLogWrapper,
    @Named(APPLICATION_SCOPE) private val appScope: CoroutineScope,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) {
    private val workingCopyIds =
        context.getSharedPreferences("local_content_working_copy_ids", Context.MODE_PRIVATE)

    // Two first saves of one post racing would make two working copies.
    private val saveLock = Mutex()

    fun save(post: PostModel, site: SiteModel) {
        if (!experimentalFeatures.isEnabled(Feature.LOCAL_WORKING_COPIES)) return

        // Map before returning: the editor keeps changing the same PostModel.
        val localPostId = post.id.toString()
        val siteIdentity = try {
            mapper.toSiteIdentity(site)
        } catch (e: ParseUrlException) {
            appLogWrapper.e(T.POSTS, "Local content: can't parse the site URL ${site.url}: $e")
            return
        }
        val remotePost = mapper.toRemotePostReference(post)
        if (post.remotePostId > 0 && remotePost == null) {
            appLogWrapper.e(T.POSTS, "Local content: local post $localPostId has no last modified date")
            return
        }
        val edit = if (post.isPage) {
            mapper.toPageFields(post)?.let { Edit.Page(it) }
        } else {
            mapper.toPostFields(post)?.let { Edit.Post(it) }
        } ?: return
        val remoteTarget = WorkingCopyRemoteTarget(site = siteIdentity, remotePost = remotePost)

        appScope.launch(ioDispatcher) {
            saveLock.withLock {
                try {
                    val id = saveOrCreate(serviceProvider.getService(), localPostId, remoteTarget, edit)
                    workingCopyIds.edit().putString(localPostId, id).apply()
                    appLogWrapper.d(T.POSTS, "Local content: saved local post $localPostId as working copy $id")
                } catch (e: LocalContentException) {
                    appLogWrapper.e(T.POSTS, "Local content: couldn't save local post $localPostId: $e")
                }
            }
        }
    }

    /** Saves [edit] to the local post's working copy, creating one if it has none, and returns its ID. */
    private fun saveOrCreate(
        service: LocalContentService,
        localPostId: String,
        remoteTarget: WorkingCopyRemoteTarget,
        edit: Edit
    ): WorkingCopyId {
        val remotePost = remoteTarget.remotePost
        val stored = workingCopyIds.getString(localPostId, null)?.let { service.read(edit, it) }
            ?: remotePost?.let { service.readForRemotePost(edit, remoteTarget.site, it.postId) }
            ?: return service.create(edit, remoteTarget)

        service.save(edit, stored.id)
        if (remotePost != null && remotePost != stored.remotePost) {
            service.recordPush(edit, stored.id, remotePost)
        }
        return stored.id
    }
}

/** The fields the editor saved, as a post's or as a page's. */
private sealed interface Edit {
    data class Post(val fields: PostWorkingCopyFields) : Edit
    data class Page(val fields: PageWorkingCopyFields) : Edit
}

/** A stored working copy's ID, and the remote post it points at. */
private data class StoredWorkingCopy(val id: WorkingCopyId, val remotePost: RemotePostReference?)

private fun LocalContentService.read(edit: Edit, id: WorkingCopyId): StoredWorkingCopy? = when (edit) {
    is Edit.Post -> readPostWorkingCopy(id)?.let { StoredWorkingCopy(it.id, it.remoteTarget.remotePost) }
    is Edit.Page -> readPageWorkingCopy(id)?.let { StoredWorkingCopy(it.id, it.remoteTarget.remotePost) }
}

private fun LocalContentService.readForRemotePost(
    edit: Edit,
    site: SiteIdentity,
    postId: Long
): StoredWorkingCopy? = when (edit) {
    is Edit.Post -> readPostWorkingCopyForRemotePost(site, postId)
        ?.let { StoredWorkingCopy(it.id, it.remoteTarget.remotePost) }
    is Edit.Page -> readPageWorkingCopyForRemotePost(site, postId)
        ?.let { StoredWorkingCopy(it.id, it.remoteTarget.remotePost) }
}

private fun LocalContentService.create(edit: Edit, remoteTarget: WorkingCopyRemoteTarget): WorkingCopyId =
    when (edit) {
        is Edit.Post -> createPostWorkingCopy(remoteTarget, edit.fields).id
        is Edit.Page -> createPageWorkingCopy(remoteTarget, edit.fields).id
    }

private fun LocalContentService.save(edit: Edit, id: WorkingCopyId) = when (edit) {
    is Edit.Post -> savePostWorkingCopy(id, edit.fields)
    is Edit.Page -> savePageWorkingCopy(id, edit.fields)
}

private fun LocalContentService.recordPush(edit: Edit, id: WorkingCopyId, remotePost: RemotePostReference) =
    when (edit) {
        is Edit.Post -> recordPostWorkingCopyPush(id, remotePost)
        is Edit.Page -> recordPageWorkingCopyPush(id, remotePost)
    }
