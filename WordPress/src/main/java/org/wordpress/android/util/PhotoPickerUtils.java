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

    /**
     * Used to modify an ImageView's content description to mark it as selected. Checks to ensure it hasn't been marked
     * as such in the current session.
     *
     * @param imageView         that will be marked as selected
     * @param imageSelectedText text that will be appended to the current content description.
     */
    public static void setImageViewContentDescriptionAsSelectedForAccessibility(@NonNull final ImageView imageView,
                                                                                @NonNull final String
                                                                                        imageSelectedText) {
        if (!imageView.getContentDescription().toString().contains(imageSelectedText)) {
            imageView.setContentDescription(
                    imageView.getContentDescription() + " "
                    + imageSelectedText);
        }
    }
}
