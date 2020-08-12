package org.wordpress.android.ui.stories

import android.app.Activity
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.PagePostCreationSourcesDetail
import org.wordpress.android.ui.PagePostCreationSourcesDetail.STORY_FROM_MY_SITE
import org.wordpress.android.ui.media.MediaBrowserActivity
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.photopicker.PhotoPickerActivity
import org.wordpress.android.ui.stories.usecase.FetchAndOptimizeLocalMediaIfNeededUseCase
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.UTILS
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class StoriesMediaPickerResultHandler @Inject constructor(
    private val fetchAndOptimizeLocalMediaIfNeededUseCase: FetchAndOptimizeLocalMediaIfNeededUseCase,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : CoroutineScope {
    // region Fields
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = mainDispatcher + job

    /* return true if MediaPickerResult was handled */
    fun handleMediaPickerResultForStories(
        data: Intent,
        activity: Activity?,
        selectedSite: SiteModel?
    ): Boolean {
        if (data.getBooleanExtra(PhotoPickerActivity.EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED, false)) {
            ActivityLauncher.addNewStoryForResult(
                    activity,
                    selectedSite,
                    PagePostCreationSourcesDetail.STORY_FROM_MY_SITE
            )
            return true
        } else if (isWPStoriesMediaBrowserTypeResult(data)) {
            if (data.hasExtra(MediaBrowserActivity.RESULT_IDS)) {
                ActivityLauncher.addNewStoryWithMediaIdsForResult(
                        activity,
                        selectedSite,
                        PagePostCreationSourcesDetail.STORY_FROM_MY_SITE,
                        data.getLongArrayExtra(
                                MediaBrowserActivity.RESULT_IDS
                        )
                )
                return true
            } else {
                val mediaUriStringsArray = data.getStringArrayExtra(
                        PhotoPickerActivity.EXTRA_MEDIA_URIS
                )
                if (mediaUriStringsArray.isNullOrEmpty()) {
                    AppLog.e(
                            UTILS,
                            "Can't resolve picked or captured image"
                    )
                    return false
                }

                launch {
                    val optimizedResult = fetchAndOptimizeLocalMediaIfNeededUseCase.copyAndOptimizeMedia(
                            convertStringArrayIntoUrisList(mediaUriStringsArray),
                            requireNotNull(selectedSite),
                            false
                    )

                    ActivityLauncher.addNewStoryWithMediaUrisForResult(
                            activity,
                            selectedSite,
                            STORY_FROM_MY_SITE,
                            convertUrisListToStringArray(optimizedResult.optimizedMediaUris)
                    )
                }
                return true
            }
        }
        return false
    }

    private fun isWPStoriesMediaBrowserTypeResult(data: Intent): Boolean {
        if (data.hasExtra(MediaBrowserActivity.ARG_BROWSER_TYPE)) {
            val browserType = data.getSerializableExtra(MediaBrowserActivity.ARG_BROWSER_TYPE)
            return (browserType as MediaBrowserType).isWPStoriesPicker
        }
        return false
    }

    private fun convertStringArrayIntoUrisList(stringArray: Array<String>): List<Uri> {
        val uris: MutableList<Uri> = ArrayList(stringArray.size)
        for (stringUri in stringArray) {
            uris.add(Uri.parse(stringUri))
        }
        return uris
    }

    private fun convertUrisListToStringArray(uris: List<Uri?>): Array<String?>? {
        val stringUris = arrayOfNulls<String>(uris.size)
        for (i in uris.indices) {
            stringUris[i] = uris[i].toString()
        }
        return stringUris
    }
}
