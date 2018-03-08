package org.wordpress.android.editor;


import org.jetbrains.annotations.NotNull;
import org.wordpress.aztec.AztecTextFormat;
import org.wordpress.aztec.ITextFormat;
import org.wordpress.aztec.toolbar.IToolbarAction;
import org.wordpress.aztec.toolbar.ToolbarActionType;

public enum MediaToolbarAction implements IToolbarAction {
    GALLERY(R.id.media_bar_button_gallery, ToolbarActionType.OTHER, AztecTextFormat.FORMAT_NONE),
    CAMERA(R.id.media_bar_button_camera, ToolbarActionType.OTHER, AztecTextFormat.FORMAT_NONE),
    LIBRARY(R.id.media_bar_button_library, ToolbarActionType.OTHER, AztecTextFormat.FORMAT_NONE);

    private final int mButtonId;
    private final ToolbarActionType mActionType;
    private final ITextFormat mTextFormat;

    MediaToolbarAction(int buttonId, ToolbarActionType actionType, ITextFormat textFormat) {
        this.mButtonId = buttonId;
        this.mActionType = actionType;
        this.mTextFormat = textFormat;
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
    public ITextFormat getTextFormat() {
        return mTextFormat;
    }

    @Override
    public boolean isStylingAction() {
        return false;
    }

    public interface MediaToolbarButtonClickListener {
        void onMediaToolbarButtonClicked(MediaToolbarAction button);
    }
}
