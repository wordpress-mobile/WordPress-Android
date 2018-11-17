package org.wordpress.android.editor;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.text.Editable;
import android.text.Spanned;
import android.view.MotionEvent;
import android.view.View;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;
import org.wordpress.aztec.IHistoryListener;

public class GutenbergEditorFragment extends EditorFragmentAbstract implements
        View.OnTouchListener,
        EditorMediaUploadListener,
        IHistoryListener {

    public static GutenbergEditorFragment newInstance(String title, String content, boolean isExpanded) {
        return null;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        return false;
    }

    @Override
    public void appendGallery(MediaGallery mediaGallery) {
    }

    @Override
    public void setUrlForVideoPressId(final String videoId, final String videoUrl, final String posterUrl) {
    }

    @Override
    public boolean isUploadingMedia() {
        return false;
    }

    @Override
    public boolean hasFailedMediaUploads() {
        return false;
    }

    @Override
    public void removeAllFailedMediaUploads() {
    }

    @Override
    public void removeMedia(String mediaId) {
    }

    @Override
    public CharSequence getTitle() {
        return null;
    }

    @Override
    public CharSequence getContent(CharSequence originalContent) {
        return null;
    }

    @Override
    public void setTitle(CharSequence text) {
    }

    @Override
    public void setContent(CharSequence text) {
    }

    @Override
    public LiveData<Editable> getTitleOrContentChanged() {
        return new MutableLiveData<>();
    }

    @Override
    public void appendMediaFile(final MediaFile mediaFile, final String mediaUrl, ImageLoader imageLoader) {
    }

    @Override
    public Spanned getSpannedContent() {
        return null;
    }

    @Override
    public void setTitlePlaceholder(CharSequence placeholderText) {
    }

    @Override
    public void setContentPlaceholder(CharSequence placeholderText) {
    }

    @Override
    public void onMediaUploadReattached(String localId, float currentProgress) {
    }

    @Override
    public void onMediaUploadRetry(String localId, MediaType mediaType) {
    }

    @Override
    public void onMediaUploadSucceeded(final String localMediaId, final MediaFile mediaFile) {
    }

    @Override
    public void onMediaUploadProgress(final String localMediaId, final float progress) {
    }

    @Override
    public void onMediaUploadFailed(final String localMediaId, final MediaType
            mediaType, final String errorMessage) {
    }

//    @Override
//    public void onVideoInfoRequested(final AztecAttributes attrs) {
//        // VideoPress special case here
//        if (attrs.hasAttribute(VideoPressExtensionsKt.getATTRIBUTE_VIDEOPRESS_HIDDEN_ID())) {
//            mEditorFragmentListener.onVideoPressInfoRequested(attrs.getValue(
//                    VideoPressExtensionsKt.getATTRIBUTE_VIDEOPRESS_HIDDEN_ID()));
//        }
//    }

    @Override
    public void onGalleryMediaUploadSucceeded(final long galleryId, long remoteMediaId, int remaining) {
    }

    @Override
    public void onRedoEnabled() {
    }

    @Override
    public void onUndoEnabled() {
    }

    @Override
    public boolean isActionInProgress() {
        return false;
    }

}
