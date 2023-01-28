package org.wordpress.android.ui.stories.media

import android.content.Context
import android.net.Uri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveResult
import com.wordpress.stories.compose.story.StoryFrameItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.posts.EditPostActivity.OnPostUpdatedFromUIListener
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.ui.posts.SavePostToDbUseCase
import org.wordpress.android.ui.posts.editor.media.AddLocalMediaToPostUseCase
import org.wordpress.android.ui.posts.editor.media.EditorMediaListener
import org.wordpress.android.ui.stories.SaveStoryGutenbergBlockUseCase
import org.wordpress.android.ui.stories.StoriesTrackerHelper
import org.wordpress.android.ui.stories.StoryComposerActivity
import org.wordpress.android.ui.stories.StoryRepositoryWrapper
import org.wordpress.android.ui.stories.prefs.StoriesPrefs
import org.wordpress.android.ui.stories.prefs.StoriesPrefs.TempId
import org.wordpress.android.ui.uploads.UploadServiceFacade
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.LONG
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.helpers.MediaFile
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

/*
 * StoryMediaSaveUploadBridge listens for StorySaveResult events triggered from the StorySaveService, and
 * then transforms its result data into something the UploadService can use to upload the Story frame media
 * first, then obtain the media Ids and collect them, and finally create a Post with the Story block
 * with the obtained media Ids.
 * This is different than uploading media to a regular Post because we don't need to replace the URLs for final Urls as
 * we do in Aztec / Gutenberg.
 */
class StoryMediaSaveUploadBridge @Inject constructor(
    private val addLocalMediaToPostUseCase: AddLocalMediaToPostUseCase,
    private val savePostToDbUseCase: SavePostToDbUseCase,
    private val storiesPrefs: StoriesPrefs,
    private val uploadService: UploadServiceFacade,
    private val networkUtils: NetworkUtilsWrapper,
    private val postUtils: PostUtilsWrapper,
    private val eventBusWrapper: EventBusWrapper,
    private val storyRepositoryWrapper: StoryRepositoryWrapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : CoroutineScope, DefaultLifecycleObserver {
    // region Fields
    private var job: Job = Job()
    private lateinit var appContext: Context

    override val coroutineContext: CoroutineContext
        get() = mainDispatcher + job

    @Inject
    lateinit var editPostRepository: EditPostRepository

    @Inject
    lateinit var storiesTrackerHelper: StoriesTrackerHelper

    @Inject
    lateinit var saveStoryGutenbergBlockUseCase: SaveStoryGutenbergBlockUseCase

    override fun onCreate(owner: LifecycleOwner) {
        eventBusWrapper.register(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        // note: not sure whether this is ever going to get called if we attach it to the lifecycle of the Application
        // class, but leaving it here prepared for the case when this class is attached to some other LifeCycleOwner
        // other than the Application.
        cancelAddMediaToEditorActions()
        eventBusWrapper.unregister(this)
    }

    fun init(context: Context) {
        appContext = context
    }

    // region Adding new composed / processed frames to a Story post
    private fun addNewStoryFrameMediaItemsToPostAndUploadAsync(site: SiteModel, saveResult: StorySaveResult) {
        // let's invoke the UploadService and enqueue all the files that were saved by the FrameSaveService
        val frames = storyRepositoryWrapper.getStoryAtIndex(saveResult.storyIndex).frames
        addNewMediaItemsInStoryFramesToPostAsync(site, frames, saveResult.isEditMode)
    }

    private fun addNewMediaItemsInStoryFramesToPostAsync(
        site: SiteModel,
        frames: List<StoryFrameItem>,
        isEditMode: Boolean
    ) {
        val uriList = frames.map { Uri.fromFile(it.composedFrameFile) }

        // this is similar to addNewMediaItemsToEditorAsync in EditorMedia
        launch {
            val localEditorMediaListener = object : EditorMediaListener {
                override fun appendMediaFiles(mediaFiles: Map<String, MediaFile>) {
                    if (!isEditMode) {
                        saveStoryGutenbergBlockUseCase.assignAltOnEachMediaFile(frames, ArrayList(mediaFiles.values))
                        saveStoryGutenbergBlockUseCase.buildJetpackStoryBlockInPost(
                            editPostRepository,
                            ArrayList(mediaFiles.values)
                        )
                    } else {
                        // no op: in edit mode, we're handling replacing of the block's mediaFiles in Gutenberg
                    }
                }

                override fun getImmutablePost(): PostImmutableModel {
                    return editPostRepository.getPost()!!
                }

                override fun syncPostObjectWithUiAndSaveIt(listener: OnPostUpdatedFromUIListener?) {
                    // no op
                    // WARNING: don't remove this, we need to call the listener no matter what,
                    // so save & upload actually happen
                    listener?.onPostUpdatedFromUI(null)
                }

                override fun advertiseImageOptimization(listener: () -> Unit) {
                    // no op
                }

                override fun onMediaModelsCreatedFromOptimizedUris(oldUriToMediaFiles: Map<Uri, MediaModel>) {
                    // in order to support Story editing capabilities, we save a serialized version of the Story slides
                    // after their composedFrameFiles have been processed.

                    // here we change the ids on the actual StoryFrameItems, and also update the flattened / composed
                    // image urls with the new URLs which may have been replaced after image optimization
                    // find the MediaModel for a given Uri from composedFrameFile
                    for (frame in frames) {
                        // if the old URI in frame.composedFrameFile exists as a key in the passed map, then update that
                        // value with the new (probably optimized) URL and also keep track of the new id.
                        val oldUri = Uri.fromFile(frame.composedFrameFile)
                        val mediaModel = oldUriToMediaFiles.get(oldUri)
                        mediaModel?.let {
                            val oldTemporaryId = frame.id ?: ""
                            frame.id = it.id.toString()

                            // set alt text on MediaModel too
                            mediaModel.alt = StoryFrameItem.getAltTextFromFrameAddedViews(frame)

                            // if prefs has this Slide with the temporary key, replace it
                            // if not, let's now save the new slide with the local key
                            storiesPrefs.replaceTempMediaIdKeyedSlideWithLocalMediaIdKeyedSlide(
                                TempId(oldTemporaryId),
                                LocalId(it.id),
                                it.localSiteId.toLong()
                            ) ?: storiesPrefs.saveSlideWithLocalId(
                                it.localSiteId.toLong(),
                                // use the local id to save the original, will be replaced later
                                // with mediaModel.mediaId after uploading to the remote site
                                LocalId(it.id),
                                frame
                            )

                            // for editMode, we'll need to tell the Gutenberg Editor to replace their mediaFiles
                            // ids with the new MediaModel local ids are created so, broadcasting the event.
                            if (isEditMode) {
                                // finally send the event that this frameId has changed
                                eventBusWrapper.postSticky(
                                    StoryFrameMediaModelCreatedEvent(
                                        oldTemporaryId,
                                        it.id,
                                        oldUri.toString(),
                                        frame
                                    )
                                )
                            }
                        }
                    }
                }

                override fun showVideoDurationLimitWarning(fileName: String) {
                    ToastUtils.showToast(
                        appContext,
                        string.error_media_video_duration_exceeds_limit,
                        LONG
                    )
                }
            }

            addLocalMediaToPostUseCase.addNewMediaToEditorAsync(
                uriList,
                site,
                freshlyTaken = false, // we don't care about this
                editorMediaListener = localEditorMediaListener,
                doUploadAfterAdding = true,
                trackEvent = false // Already tracked event when media were first added to the story
            )

            // only save this post if we're not currently in edit mode
            // In edit mode, we'll let the Gutenberg editor save the edited block if / when needed.
            if (!isEditMode) {
                postUtils.preparePostForPublish(requireNotNull(editPostRepository.getEditablePost()), site)
                savePostToDbUseCase.savePostToDb(editPostRepository, site)

                if (networkUtils.isNetworkAvailable()) {
                    postUtils.trackSavePostAnalytics(
                        editPostRepository.getPost(),
                        site
                    )
                    uploadService.uploadPost(appContext, editPostRepository.id, true)
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
    }
    // endregion

    private fun cancelAddMediaToEditorActions() {
        job.cancel()
    }

    @Subscribe(sticky = true, threadMode = MAIN)
    fun onEventMainThread(event: StorySaveResult) {
        // track event
        storiesTrackerHelper.trackStorySaveResultEvent(event)

        event.metadata?.let { bundle ->
            val site = bundle.getSerializableCompat<SiteModel>(WordPress.SITE)
            val story = storyRepositoryWrapper.getStoryAtIndex(event.storyIndex)
            site?.let { siteModel ->
                saveStoryGutenbergBlockUseCase.saveNewLocalFilesToStoriesPrefsTempSlides(
                    siteModel,
                    event.storyIndex,
                    story.frames
                )
            }

            // only trigger the bridge preparation and the UploadService if the Story is now complete
            // otherwise we can be receiving successful retry events for individual frames we shouldn't care about just
            // yet.
            if (isStorySavingComplete(event) && !event.isRetry) {
                // only remove it if it was successful - we want to keep it and show a snackbar once when the user
                // comes back to the app if it wasn't, see MySiteFragment for details.
                eventBusWrapper.removeStickyEvent(event)
                editPostRepository.loadPostByLocalPostId(bundle.getInt(StoryComposerActivity.KEY_POST_LOCAL_ID))
                // media upload tracking already in addLocalMediaToPostUseCase.addNewMediaToEditorAsync
                site?.let { siteModel -> addNewStoryFrameMediaItemsToPostAndUploadAsync(siteModel, event) }
            }
        }
    }

    private fun isStorySavingComplete(event: StorySaveResult): Boolean {
        return (event.isSuccess() &&
                event.frameSaveResult.size == storyRepositoryWrapper.getStoryAtIndex(event.storyIndex).frames.size)
    }

    data class StoryFrameMediaModelCreatedEvent(
        val oldId: String,
        val newId: Int,
        val oldUrl: String,
        val frame: StoryFrameItem
    )
}
