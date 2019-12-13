package org.wordpress.android.util;

import android.widget.ImageView;

import androidx.annotation.NonNull;

import org.wordpress.android.R;

public class PhotoPickerUtils {
    public static void announceSelectedImageForAccessibility(@NonNull ImageView imageThumbnail, boolean itemSelected) {
        if (itemSelected) {
            imageThumbnail.announceForAccessibility(
                    imageThumbnail.getContext()
                                  .getString(R.string.photo_picker_image_thumbnail_selected));
        } else {
            imageThumbnail.announceForAccessibility(
                    imageThumbnail.getContext().getString(R.string.photo_picker_image_thumbnail_unselected));
        }
    }
}
