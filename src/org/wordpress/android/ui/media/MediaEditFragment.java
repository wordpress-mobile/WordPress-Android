package org.wordpress.android.ui.media;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;

public class MediaEditFragment extends Fragment {

    private static final String ARGS_MEDIA_ID = "media_id";
    
    private NetworkImageView mImageView;
    private EditText mTitleView;
    private EditText mCaptionView;
    private EditText mDescriptionView;
    private MediaEditFragmentCallback mCallback;
    
    public interface MediaEditFragmentCallback {
        public void onResumeMediaEditFragment();
        public void onPauseMediaEditFragment();
    }
    
    public static MediaEditFragment newInstance(String mediaId) {
        MediaEditFragment fragment = new MediaEditFragment();
        
        Bundle args = new Bundle();
        args.putString(ARGS_MEDIA_ID, mediaId);
        fragment.setArguments(args);

        return fragment;
    }    
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        try {
            mCallback = (MediaEditFragmentCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement MediaEditFragmentCallback");
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mCallback.onResumeMediaEditFragment();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        mCallback.onPauseMediaEditFragment();
    }
    
    private String getMediaId() {
        if (getArguments() != null)
            return getArguments().getString(ARGS_MEDIA_ID);
        else
            return null;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.media_edit_fragment, container, false);

        mTitleView = (EditText) view.findViewById(R.id.media_edit_fragment_title);
        mImageView = (NetworkImageView) view.findViewById(R.id.media_edit_fragment_image);
        mCaptionView = (EditText) view.findViewById(R.id.media_edit_fragment_caption);
        mDescriptionView = (EditText) view.findViewById(R.id.media_edit_fragment_description);

        loadMedia(getMediaId());

        return view;
    }
    
    public void loadMedia(String mediaId) {
        String id = mediaId;
        Blog blog = WordPress.getCurrentBlog();
        
        if (blog != null) {
            String blogId = String.valueOf(blog.getBlogId());
            
            Cursor cursor;
            
            // if the id is null, get the first media item in the database
            if (id == null) {
                cursor = WordPress.wpDB.getFirstMediaFileForBlog(blogId);
            } else {
                cursor = WordPress.wpDB.getMediaFile(blogId, id);
            }
            
            refreshViews(cursor);
            cursor.close();
        }
    }
    
    private void refreshViews(Cursor cursor) {
        if (!cursor.moveToFirst())
            return;
        
        mTitleView.setText(cursor.getString(cursor.getColumnIndex("title")));
        mCaptionView.setText(cursor.getString(cursor.getColumnIndex("caption")));
        mDescriptionView.setText(cursor.getString(cursor.getColumnIndex("description")));
        
        String imageUrl = cursor.getString(cursor.getColumnIndex("fileURL"));
        if (MediaUtils.isValidImage(imageUrl)) {
            
            int width = cursor.getInt(cursor.getColumnIndex("width"));
            int height = cursor.getInt(cursor.getColumnIndex("height"));
            
            float screenWidth;

            View parentView = (View) mImageView.getParent();
            
            //differentiating between tablet and phone
            if (this.isInLayout()) {
                screenWidth =  parentView.getMeasuredWidth();
            } else {
                screenWidth = getActivity().getResources().getDisplayMetrics().widthPixels;
            }
            float screenHeight = getActivity().getResources().getDisplayMetrics().heightPixels;
            
            mImageView.setImageUrl(imageUrl + "?w=" + screenWidth, WordPress.imageLoader);
            mImageView.setVisibility(View.VISIBLE);
            
            if (width > screenWidth) {
                height = (int) (height / (width/screenWidth));
            } else if (height > screenHeight) {
                width = (int) (width / (height/screenHeight));
            }
            mImageView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, height));
            
        } else {
            mImageView.setVisibility(View.GONE);
        }
    }
}
