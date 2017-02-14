package org.wordpress.android.ui.posts.photochooser;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import org.wordpress.android.R;

import uk.co.senab.photoview.PhotoViewAttacher;

public class PhotoChooserPreviewActivity extends AppCompatActivity {

    public static final String ARG_MEDIA_URI = "media_uri";

    private Uri mMediaUri;
    private ImageView mImageView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.photo_chooser_preview_activity);
        mImageView = (ImageView) findViewById(R.id.image_preview);

        String uriString;
        if (savedInstanceState != null) {
            uriString = savedInstanceState.getString(ARG_MEDIA_URI);
        } else {
            uriString = getIntent().getStringExtra(ARG_MEDIA_URI);
        }

        // TODO: handle video preview
        mMediaUri = Uri.parse(uriString);
        mImageView.setImageURI(mMediaUri);

        PhotoViewAttacher attacher = new PhotoViewAttacher(mImageView);
        attacher.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
            @Override
            public void onPhotoTap(View view, float v, float v2) {
                finish();
            }
        });
        attacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float v, float v2) {
                finish();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_MEDIA_URI, mMediaUri.toString());
    }
}
