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
import uniffi.wp_mobile_local_content.WorkingCopyId
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Saves a post the editor saves through FluxC to the local content database as well, while the
 * [Feature.LOCAL_WORKING_COPIES] experimental feature is on.
 *
 * A post's working copy is found by the FluxC local post ID it was first saved under, kept in shared
 * preferences. Each working copy is made as a new post on the site: none points at the remote post
 * it was edited from.
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
        if (post.isPage) {
            val page = mapper.toPageWorkingCopy(post) ?: return
            launchSave(
                localPostId,
                create = { it.createPageWorkingCopy(page, siteIdentity) },
                save = { service, id -> service.savePageWorkingCopy(id, page) }
            )
        } else {
            val workingCopy = mapper.toPostWorkingCopy(post) ?: return
            launchSave(
                localPostId,
                create = { it.createPostWorkingCopy(workingCopy, siteIdentity) },
                save = { service, id -> service.savePostWorkingCopy(id, workingCopy) }
            )
        }
    }

    private fun launchSave(
        localPostId: String,
        create: (LocalContentService) -> WorkingCopyId,
        save: (LocalContentService, WorkingCopyId) -> Unit
    ) {
        appScope.launch(ioDispatcher) {
            saveLock.withLock {
                try {
                    val service = serviceProvider.getService()
                    val id = workingCopyIds.getString(localPostId, null)
                        ?.takeIf { saveIfFound(service, it, save) }
                        ?: create(service)
                    workingCopyIds.edit().putString(localPostId, id).apply()
                    appLogWrapper.d(T.POSTS, "Local content: saved local post $localPostId as working copy $id")
                } catch (e: LocalContentException) {
                    appLogWrapper.e(T.POSTS, "Local content: couldn't save local post $localPostId: $e")
                }
            }
        }
    }

    /** Returns `false` if the working copy is gone, for example because it was deleted. */
    private fun saveIfFound(
        service: LocalContentService,
        id: WorkingCopyId,
        save: (LocalContentService, WorkingCopyId) -> Unit
    ): Boolean = try {
        save(service, id)
        true
    } catch (e: LocalContentException.WorkingCopyNotFound) {
        false
    }
}
