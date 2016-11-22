package org.wordpress.android.ui.reader;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.MediaController;
import android.widget.VideoView;

import org.wordpress.android.R;

/**
 *
 */

public class ReaderVideoViewerActivity extends AppCompatActivity {

    private String mVideoUrl;
    private VideoView mVideoView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_video_player);
        mVideoView = (VideoView) findViewById(R.id.video_view);

        if (savedInstanceState == null) {
            mVideoUrl = getIntent().getStringExtra(ReaderConstants.ARG_VIDEO_URL);
        } else {
            mVideoUrl = savedInstanceState.getString(ReaderConstants.ARG_VIDEO_URL);
        }

        MediaController mediacontroller = new MediaController(this);
        mediacontroller.setAnchorView(mVideoView);
        mVideoView.setMediaController(mediacontroller);
        mVideoView.setVideoURI(Uri.parse(mVideoUrl));
        mVideoView.requestFocus();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(ReaderConstants.ARG_VIDEO_URL, mVideoUrl);
        super.onSaveInstanceState(outState);
    }
}
