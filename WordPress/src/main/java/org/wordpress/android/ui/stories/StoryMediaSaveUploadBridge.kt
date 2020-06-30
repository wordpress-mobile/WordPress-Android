package org.wordpress.android.ui.stories

import android.net.Uri
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveResult
import com.wordpress.stories.compose.story.StoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.posts.EditPostActivity.OnPostUpdatedFromUIListener
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.ui.posts.SavePostToDbUseCase
import org.wordpress.android.ui.posts.editor.media.AddLocalMediaToPostUseCase
import org.wordpress.android.ui.posts.editor.media.EditorMediaListener
import org.wordpress.android.ui.uploads.UploadServiceFacade
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.helpers.MediaFile
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

/*
 * StoryMediaSaveUploadBridge listens for StorySaveResult events triggered from the StorySaveService, and
 * then transforms its result data into something the UploadService can use to upload the Story frame media
 * first, then obtain the media Ids and collect them, and finally create a Post with the Story block (
 * (simple WP gallery for alpha) with he obtained media Ids.
 * This is different than uploading media to a regular Post because we don't need to replace the URLs for final Urls as
 * we do in Aztec / Gutenberg.
 * The gallery is only a collection of media Ids, so we really need to first upload the Media, obtain the remote id,
 * and then only create the post and upload it.
 */
class StoryMediaSaveUploadBridge @Inject constructor(
    private val addLocalMediaToPostUseCase: AddLocalMediaToPostUseCase,
    private val savePostToDbUseCase: SavePostToDbUseCase,
    private val uploadService: UploadServiceFacade,
    private val networkUtils: NetworkUtilsWrapper,
    private val postUtils: PostUtilsWrapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : CoroutineScope, LifecycleObserver, EditorMediaListener {
    // region Fields
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = mainDispatcher + job

    @Inject lateinit var editPostRepository: EditPostRepository
    @Inject lateinit var storiesTrackerHelper: StoriesTrackerHelper
    @Inject lateinit var saveStoryGutenbergBlockUseCase: SaveStoryGutenbergBlockUseCase

    // TODO remove this variable. We only need to remember this so we can tell if this is the alpha test site
    private lateinit var currentSiteModel: SiteModel

    @Suppress("unused")
    @OnLifecycleEvent(ON_CREATE)
    fun onCreate(source: LifecycleOwner) {
        EventBus.getDefault().register(this)
    }

    @Suppress("unused")
    @OnLifecycleEvent(ON_DESTROY)
    fun onDestroy(source: LifecycleOwner) {
        // note: not sure whether this is ever going to get called if we attach it to the lifecycle of the Application
        // class, but leaving it here prepared for the case when this class is attached to some other LifeCycleOwner
        // other than the Application.
        cancelAddMediaToEditorActions()
        EventBus.getDefault().unregister(this)
    }

    // region Adding new composed / processed frames to a Story post
    private fun addNewStoryFrameMediaItemsToPostAndUploadAsync(site: SiteModel, saveResult: StorySaveResult) {
        // let's invoke the UploadService and enqueue all the files that were saved by the FrameSaveService
        val frames = StoryRepository.getStoryAtIndex(saveResult.storyIndex).frames
        val uriList = frames.map { Uri.fromFile(it.composedFrameFile) }
        addNewMediaItemsToPostAsync(site, uriList)
    }

    private fun addNewMediaItemsToPostAsync(site: SiteModel, uriList: List<Uri>) {
        // this is similar to addNewMediaItemsToEditorAsync in EditorMedia
        launch {
            addLocalMediaToPostUseCase.addNewMediaToEditorAsync(
                    uriList,
                    site,
                    freshlyTaken = false, // we don't care about this
                    editorMediaListener = this@StoryMediaSaveUploadBridge,
                    doUploadAfterAdding = true
            )
            postUtils.preparePostForPublish(editPostRepository.getEditablePost()!!, site)
            savePostToDbUseCase.savePostToDb(WordPress.getContext(), editPostRepository, site)

            if (networkUtils.isNetworkAvailable()) {
                postUtils.trackSavePostAnalytics(
                        editPostRepository.getPost(),
                        site
                )
                uploadService.uploadPost(WordPress.getContext(), editPostRepository.id, true)
                // SAVED_ONLINE
                storiesTrackerHelper.trackStoryPostSavedEvent(uriList.size, site, false)
            } else {
                // SAVED_LOCALLY
                storiesTrackerHelper.trackStoryPostSavedEvent(uriList.size, site, true)
                // no op, when network is available the offline mode in WPAndroid will gather the queued Post
                // and try to upload.
            }
        }
    }
    // endregion

    private fun cancelAddMediaToEditorActions() {
        job.cancel()
    }

    @Subscribe(sticky = true, threadMode = MAIN)
    fun onEventMainThread(event: StorySaveResult) {
        // track event
        storiesTrackerHelper.trackStorySaveResultEvent(event)

        // only trigger the bridge preparation and the UploadService if the Story is now complete
        // otherwise we can be receiving successful retry events for individual frames we shoulnd't care about just
        // yet.
        if (isStorySuccessfullySavedAndComplete(event)) {
            // only remove it if it was successful - we want to keep it and show a snackbar once when the user
            // comes back to the app if it wasn't, see MySiteFrament for details.
            EventBus.getDefault().removeStickyEvent(event)
            event.metadata?.let {
                val site = it.getSerializable(WordPress.SITE) as SiteModel
                currentSiteModel = site
                editPostRepository.loadPostByLocalPostId(it.getInt(StoryComposerActivity.KEY_POST_LOCAL_ID))
                // media upload tracking already in addLocalMediaToPostUseCase.addNewMediaToEditorAsync
                addNewStoryFrameMediaItemsToPostAndUploadAsync(site, event)
            }
        }
    }

    private fun isStorySuccessfullySavedAndComplete(event: StorySaveResult): Boolean {
        return (event.isSuccess() &&
                event.frameSaveResult.size == StoryRepository.getStoryAtIndex(event.storyIndex).frames.size)
    }

    override fun appendMediaFiles(mediaFiles: Map<String, MediaFile>) {
        // Create a gallery shortcode and placeholders for Media Ids
        // TODO remove this code, for now it checks whether this is one of our test sites that supports
        // the new GB block or not. Fallbacks to WP Gallery when not the case.
        // is it the test site or not?
        // https://dekervit.wpsandbox.me/
        // https://particular-wildcat.jurassic.ninja/
        if (isThisAnAlphaTestSite(mediaFiles.entries.iterator().next().value)) {
            saveStoryGutenbergBlockUseCase.buildJetpackStoryBlockInPost(editPostRepository, mediaFiles)
        } else {
            // just create a WP Gallery
            saveStoryGutenbergBlockUseCase.buildWPGalleryInPost(editPostRepository, mediaFiles)
        }
    }

    override fun getImmutablePost(): PostImmutableModel {
        return editPostRepository.getPost()!!
    }

    override fun syncPostObjectWithUiAndSaveIt(listener: OnPostUpdatedFromUIListener?) {
        // no op
        // WARNING: don't remove this, we need to call the listener no matter what, so save & upload actually happen
        listener?.onPostUpdatedFromUI()
    }

    override fun advertiseImageOptimization(listener: () -> Unit) {
        // no op
    }

    private fun isThisAnAlphaTestSite(mediaFile: MediaFile): Boolean {
        val siteUrl = requireNotNull(currentSiteModel.url)
        return (Uri.parse(siteUrl).host in alphaSites && mediaFile.blogId.toInt() == currentSiteModel.id)
    }

    companion object {
        val alphaSites =
                arrayOf("particular-wildcat.jurassic.ninja", "dekervit.wpsandbox.me")
    }
}
