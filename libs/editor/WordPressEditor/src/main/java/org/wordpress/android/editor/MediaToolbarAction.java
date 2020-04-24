package org.wordpress.android.editor;


import org.jetbrains.annotations.NotNull;
import org.wordpress.aztec.AztecTextFormat;
import org.wordpress.aztec.ITextFormat;
import org.wordpress.aztec.toolbar.IToolbarAction;
import org.wordpress.aztec.toolbar.ToolbarActionType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum MediaToolbarAction implements IToolbarAction {
    GALLERY(R.id.media_bar_button_gallery, ToolbarActionType.OTHER,
            new HashSet<ITextFormat>(Collections.singletonList(AztecTextFormat.FORMAT_NONE)),
            R.drawable.media_bar_button_image_multiple_selector),
    CAMERA(R.id.media_bar_button_camera, ToolbarActionType.OTHER,
            new HashSet<ITextFormat>(Collections.singletonList(AztecTextFormat.FORMAT_NONE)),
            R.drawable.media_bar_button_camera_selector),
    LIBRARY(R.id.media_bar_button_library, ToolbarActionType.OTHER,
            new HashSet<ITextFormat>(Collections.singletonList(AztecTextFormat.FORMAT_NONE)),
            R.drawable.media_bar_button_library_selector);

    private final int mButtonId;
    private final ToolbarActionType mActionType;
    private final Set<ITextFormat> mTextFormats;
    private final int mButtonDrawableRes;

    MediaToolbarAction(
            int buttonId,
            ToolbarActionType actionType,
            Set<ITextFormat> textFormats,
            int buttonDrawableRes
    ) {
        this.mButtonId = buttonId;
        this.mActionType = actionType;
        this.mTextFormats = textFormats;
        this.mButtonDrawableRes = buttonDrawableRes;
    }

    @Override
    public int getButtonId() {
        return mButtonId;
    }

    @NotNull
    @Override
    public ToolbarActionType getActionType() {
        return mActionType;
    }

    @NotNull
    @Override
    public Set<ITextFormat> getTextFormats() {
        return mTextFormats;
    }

    @Override public int getButtonDrawableRes() {
        return mButtonDrawableRes;
    }

    @Override
    public boolean isStylingAction() {
        return false;
    }

    public interface MediaToolbarButtonClickListener {
        void onMediaToolbarButtonClicked(MediaToolbarAction button);
    }
}
