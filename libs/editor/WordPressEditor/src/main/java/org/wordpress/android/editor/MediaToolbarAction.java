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
            new HashSet<ITextFormat>(Collections.singletonList(AztecTextFormat.FORMAT_NONE))),
    CAMERA(R.id.media_bar_button_camera, ToolbarActionType.OTHER,
            new HashSet<ITextFormat>(Collections.singletonList(AztecTextFormat.FORMAT_NONE))),
    LIBRARY(R.id.media_bar_button_library, ToolbarActionType.OTHER,
            new HashSet<ITextFormat>(Collections.singletonList(AztecTextFormat.FORMAT_NONE)));

    private final int mButtonId;
    private final ToolbarActionType mActionType;
    private final Set<ITextFormat> mTextFormats;

    MediaToolbarAction(int buttonId, ToolbarActionType actionType, Set<ITextFormat> textFormats) {
        this.mButtonId = buttonId;
        this.mActionType = actionType;
        this.mTextFormats = textFormats;
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

    @Override
    public boolean isStylingAction() {
        return false;
    }

    public interface MediaToolbarButtonClickListener {
        void onMediaToolbarButtonClicked(MediaToolbarAction button);
    }
}
