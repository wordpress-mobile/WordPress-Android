package org.wordpress.android.imageeditor.utils

import android.net.Uri
import android.os.Bundle
import com.yalantis.ucrop.UCrop.EXTRA_INPUT_URI
import com.yalantis.ucrop.UCrop.EXTRA_OUTPUT_URI
import com.yalantis.ucrop.UCrop.Options
import com.yalantis.ucrop.model.AspectRatio
import com.yalantis.ucrop.view.CropImageView
import java.io.File

object UCropUtil {
    private val uCropOptions
        get() = Options().also {
            it.setShowCropGrid(true)
            it.setFreeStyleCropEnabled(true)
            it.setShowCropFrame(true)
            it.setHideBottomControls(false)
            it.setAspectRatioOptions(
                0,
                AspectRatio("1:2", 1f, 2f),
                AspectRatio("3:4", 3f, 4f),
                AspectRatio("Original", CropImageView.DEFAULT_ASPECT_RATIO, CropImageView.DEFAULT_ASPECT_RATIO),
                AspectRatio("16:9", 16f, 9f),
                AspectRatio("1:1", 1f, 1f)
            )
        }

    fun getUCropOptionsBundle(inputFile: File, outputFile: File) = Bundle().also {
        it.putParcelable(EXTRA_INPUT_URI, Uri.fromFile(inputFile))
        it.putParcelable(EXTRA_OUTPUT_URI, Uri.fromFile(outputFile))
        it.putAll(uCropOptions.optionBundle)
    }
}
