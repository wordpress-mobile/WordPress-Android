package org.wordpress.android.ui.stories.usecase

import android.net.Uri
import dagger.Reusable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.editor.media.CopyMediaToAppStorageUseCase
import org.wordpress.android.ui.posts.editor.media.CopyMediaToAppStorageUseCase.CopyMediaResult
import org.wordpress.android.ui.posts.editor.media.OptimizeMediaUseCase
import org.wordpress.android.ui.posts.editor.media.OptimizeMediaUseCase.OptimizeMediaResult
import javax.inject.Inject

/**
 * Processes a list of local media items in the background (optimizing, resizing, rotating, etc.)
 */
@Reusable
class FetchAndOptimizeLocalMediaIfNeededUseCase @Inject constructor(
    private val copyMediaToAppStorageUseCase: CopyMediaToAppStorageUseCase,
    private val optimizeMediaUseCase: OptimizeMediaUseCase
) {
    /**
     * Copies files to app storage and optimizes them
     */
    suspend fun copyAndOptimizeMedia(
        uriList: List<Uri>,
        site: SiteModel,
        freshlyTaken: Boolean
    ): OptimizeMediaResult {
        // Copy files to apps storage to make sure they are permanently accessible.
        val copyFilesResult: CopyMediaResult = copyMediaToAppStorageUseCase.copyFilesToAppStorageIfNecessary(uriList)

        // Optimize and rotate the media
        return optimizeMediaUseCase.optimizeMediaIfSupportedAsync(
                        site,
                        freshlyTaken,
                        copyFilesResult.permanentlyAccessibleUris
                )
    }
}
