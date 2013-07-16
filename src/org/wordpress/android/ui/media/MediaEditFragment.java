
package org.wordpress.android.ui.media;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;

import org.xmlrpc.android.ApiHelper;

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

    private boolean mUpdatingMedia = false;

    public interface MediaEditFragmentCallback {
        public void onResume(Fragment fragment);
        public void onPause(Fragment fragment);
        public void onEditCompleted(boolean result);
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
            throw new ClassCastException(activity.toString()
                    + " must implement MediaEditFragmentCallback");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mCallback.onResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mCallback.onPause(this);
    }

    public String getMediaId() {
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

    public void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
    }
    
    public void editMedia() {
        hideKeyboard();
        
        final String mediaId = this.getMediaId();
        final String title = mTitleView.getText().toString();
        final String description = mDescriptionView.getText().toString();
        final Blog currentBlog = WordPress.getCurrentBlog();

        ApiHelper.EditMediaItemTask task = new ApiHelper.EditMediaItemTask(mediaId, title,
                description,
                new ApiHelper.EditMediaItemTask.Callback() {

                    @Override
                    public void onSuccess() {
                        String blogId = String.valueOf(currentBlog.getBlogId());
                        WordPress.wpDB.updateMediaFile(blogId, mediaId, title, description);

                        Toast.makeText(getActivity(), "SUCCESS!!", Toast.LENGTH_LONG)
                                .show();

                        mUpdatingMedia = false;
                        
                        mCallback.onEditCompleted(true);
                    }

                    @Override
                    public void onFailure() {
                        Toast.makeText(getActivity(), "Failure!!", Toast.LENGTH_LONG)
                                .show();

                        mUpdatingMedia = false;
                        mCallback.onEditCompleted(false);
                    }
                });

        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(currentBlog);

        if (!mUpdatingMedia) {
            mUpdatingMedia = true;
            task.execute(apiArgs);
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

            // differentiating between tablet and phone
            if (this.isInLayout()) {
                screenWidth = parentView.getMeasuredWidth();
            } else {
                screenWidth = getActivity().getResources().getDisplayMetrics().widthPixels;
            }
            float screenHeight = getActivity().getResources().getDisplayMetrics().heightPixels;

            mImageView.setImageUrl(imageUrl + "?w=" + screenWidth, WordPress.imageLoader);
            mImageView.setVisibility(View.VISIBLE);

            if (width > screenWidth) {
                height = (int) (height / (width / screenWidth));
            } else if (height > screenHeight) {
                width = (int) (width / (height / screenHeight));
            }
            mImageView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    height));

        } else {
            mImageView.setVisibility(View.GONE);
        }
    }
}
