package org.wordpress.android.ui.media;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;

public class MediaItemFragment extends Fragment {

    private static final String ARGS_MEDIA_ID = "media_id";
    
    private TextView mTitleView;
    private NetworkImageView mImageView;
    private TextView mCaptionView;
    private TextView mDescriptionView;
    private TextView mDateView;
    private TextView mFileNameView;
    private TextView mFileTypeView;
    private TextView mDimensionsView;

    public static MediaItemFragment newInstance(String mediaId) {
        MediaItemFragment fragment = new MediaItemFragment();
        
        Bundle args = new Bundle();
        args.putString(ARGS_MEDIA_ID, mediaId);
        fragment.setArguments(args);

        return fragment;
    }

    private String getMediaId() {
        if (getArguments() != null)
            return getArguments().getString(ARGS_MEDIA_ID);
        else
            return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.media_listitem_details, container, false);

        mTitleView = (TextView) view.findViewById(R.id.media_listitem_details_title);
        mImageView = (NetworkImageView) view.findViewById(R.id.media_listitem_details_image);
        mCaptionView = (TextView) view.findViewById(R.id.media_listitem_details_caption);
        mDescriptionView = (TextView) view.findViewById(R.id.media_listitem_details_description);
        mDateView = (TextView) view.findViewById(R.id.media_listitem_details_date);
        mFileNameView = (TextView) view.findViewById(R.id.media_listitem_details_file_name);
        mFileTypeView = (TextView) view.findViewById(R.id.media_listitem_details_file_type);
        mDimensionsView = (TextView) view.findViewById(R.id.media_listitem_details_dimensions);

        loadMedia(getMediaId());

        return view;
    }

    public void loadMedia(String mediaId) {
        if (mediaId == null)
            return;

        Blog blog = WordPress.getCurrentBlog();
        if (blog != null) {
            String blogId = String.valueOf(blog.getBlogId());
            Cursor cursor = WordPress.wpDB.getMediaFile(blogId, mediaId);
            refreshViews(cursor);
        }
    }

    private void refreshViews(Cursor cursor) {
        if (!cursor.moveToFirst())
            return;
        
        mTitleView.setText(cursor.getString(cursor.getColumnIndex("title")));
        
        mCaptionView.setText(cursor.getString(cursor.getColumnIndex("caption")));
        mDescriptionView.setText(cursor.getString(cursor.getColumnIndex("description")));
        
        String date = MediaUtils.getDate(cursor.getLong(cursor.getColumnIndex("date_created_gmt")));
        mDateView.setText("Uploaded on: " + date);
        
        String fileName = cursor.getString(cursor.getColumnIndex("fileName"));
        mFileNameView.setText("File name: " + fileName);
        
        // get the file extension from the fileURL
        String fileURL = cursor.getString(cursor.getColumnIndex("fileURL"));
        String fileType = fileURL.replaceAll(".*\\.(\\w+)$", "$1").toUpperCase(); 
        mFileTypeView.setText("File type: " + fileType);
        

        String imageUrl = cursor.getString(cursor.getColumnIndex("fileURL"));
        if (MediaUtils.isValidImage(imageUrl)) {
            mImageView.setImageUrl(imageUrl, WordPress.imageLoader);
            mImageView.setVisibility(View.VISIBLE);

            String dimensions = cursor.getString(cursor.getColumnIndex("width")) + "x" + cursor.getString(cursor.getColumnIndex("height"));
            mDimensionsView.setText("Dimensions: " + dimensions);
            mDimensionsView.setVisibility(View.VISIBLE);
        } else {
            mImageView.setVisibility(View.GONE);
            mDimensionsView.setVisibility(View.GONE);
        }
    }
    
}
