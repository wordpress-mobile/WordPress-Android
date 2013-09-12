package org.wordpress.android.ui.media;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.ImageHelper.BitmapWorkerCallback;
import org.wordpress.android.util.ImageHelper.BitmapWorkerTask;

/**
 * A fragment display a media item's details. 
 * Only appears on phone.
 */
public class MediaItemFragment extends SherlockFragment {

    private static final String ARGS_MEDIA_ID = "media_id";

    public static final String TAG = MediaItemFragment.class.getName();

    private View mView;
    
    private ImageView mImageView;
    private TextView mTitleView;
    private TextView mCaptionView;
    private TextView mDescriptionView;
    private TextView mDateView;
    private TextView mFileNameView;
    private TextView mFileTypeView;
    private TextView mDimensionsView;
    private MediaItemFragmentCallback mCallback;

    private boolean mIsLocal;
    
    public interface MediaItemFragmentCallback {
        public void onResume(Fragment fragment);
        public void onPause(Fragment fragment);
        public void onDeleteMedia(final List<String> ids);
    }
    
    public static MediaItemFragment newInstance(String mediaId) {
        MediaItemFragment fragment = new MediaItemFragment();
        
        Bundle args = new Bundle();
        args.putString(ARGS_MEDIA_ID, mediaId);
        fragment.setArguments(args);

        return fragment;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        try {
            mCallback = (MediaItemFragmentCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement MediaItemFragmentCallback");
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

        mView = inflater.inflate(R.layout.media_listitem_details, container, false);

        mTitleView = (TextView) mView.findViewById(R.id.media_listitem_details_title);
        mCaptionView = (TextView) mView.findViewById(R.id.media_listitem_details_caption);
        mDescriptionView = (TextView) mView.findViewById(R.id.media_listitem_details_description);
        mDateView = (TextView) mView.findViewById(R.id.media_listitem_details_date);
        mFileNameView = (TextView) mView.findViewById(R.id.media_listitem_details_file_name);
        mFileTypeView = (TextView) mView.findViewById(R.id.media_listitem_details_file_type);
        mDimensionsView = (TextView) mView.findViewById(R.id.media_listitem_details_dimensions);
        
        loadMedia(getMediaId());

        return mView;
    }

    /** Loads the first media item for the current blog from the database **/
    public void loadDefaultMedia() {
        loadMedia(null); 
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

        // check whether or not to show the edit button
        String state = cursor.getString(cursor.getColumnIndex("uploadState"));
        mIsLocal = MediaUtils.isLocalFile(state);
        if (mIsLocal)
            getSherlockActivity().invalidateOptionsMenu();
        
        // title
        mTitleView.setText(cursor.getString(cursor.getColumnIndex("title")));
        
        // caption
        String caption = cursor.getString(cursor.getColumnIndex("caption"));
        if (caption == null || caption.length() == 0) {
            mCaptionView.setVisibility(View.GONE);
        } else {
            mCaptionView.setText(caption);
            mCaptionView.setVisibility(View.VISIBLE);
        }
        
        // description
        String desc = cursor.getString(cursor.getColumnIndex("description"));
        if (desc == null || desc.length() == 0) {
            mDescriptionView.setVisibility(View.GONE);
        } else {
            mDescriptionView.setText(desc);
            mDescriptionView.setVisibility(View.VISIBLE);
        }

        // added / upload date
        String date = MediaUtils.getDate(cursor.getLong(cursor.getColumnIndex("date_created_gmt")));
        if (mIsLocal)
            mDateView.setText("Added on: " + date);
        else
            mDateView.setText("Uploaded on: " + date);
        
        // file name
        String fileName = cursor.getString(cursor.getColumnIndex("fileName"));
        mFileNameView.setText("File name: " + fileName);
        
        // get the file extension from the fileURL
        String fileURL = cursor.getString(cursor.getColumnIndex("fileURL"));
        if (fileURL != null) {
            String fileType = fileURL.replaceAll(".*\\.(\\w+)$", "$1").toUpperCase(); 
            mFileTypeView.setText("File type: " + fileType);
            mFileTypeView.setVisibility(View.VISIBLE);
        } else {
            mFileTypeView.setVisibility(View.GONE);
        }
        
        String imageUri = cursor.getString(cursor.getColumnIndex("fileURL"));
        if (imageUri == null)
            imageUri = cursor.getString(cursor.getColumnIndex("filePath"));
        
        inflateImageView();
        
        // image and dimensions
        if (MediaUtils.isValidImage(imageUri)) {
            
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
            
            if (width > 0 && height > 0) {
                String dimensions = width + "x" + height;
                mDimensionsView.setText("Dimensions: " + dimensions);
                mDimensionsView.setVisibility(View.VISIBLE);
            } else {
                mDimensionsView.setVisibility(View.GONE);
            }
            
            if (width > screenWidth) {
                height = (int) (height / (width/screenWidth));
                width = (int) screenWidth;
            } else if (height > screenHeight) {
                width = (int) (width / (height/screenHeight));
                height = (int) screenHeight;
            }
            
            if (mIsLocal) {
                final String filePath = cursor.getString(cursor.getColumnIndex("filePath"));
                loadLocalImage(mImageView, filePath, width, height);
            } else {
                ((NetworkImageView) mImageView).setImageUrl(imageUri + "?w=" + screenWidth, WordPress.imageLoader);
            }
            mImageView.setVisibility(View.VISIBLE);
            
            mImageView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, height));
            
        } else {
            mImageView.setVisibility(View.GONE);
            mDimensionsView.setVisibility(View.GONE);
        }
    }

    private void inflateImageView() {
        ViewStub viewStub = (ViewStub) mView.findViewById(R.id.media_listitem_details_stub);
        if (viewStub != null) {
            if (mIsLocal)
                viewStub.setLayoutResource(R.layout.media_grid_image_local);
            else
                viewStub.setLayoutResource(R.layout.media_grid_image_network);
            viewStub.inflate();
        }
        
        mImageView = (ImageView) mView.findViewById(R.id.media_listitem_details_image);
    }
    
    private synchronized void loadLocalImage(ImageView imageView, String filePath, int width, int height) {

        if (MediaUtils.isValidImage(filePath)) {
            imageView.setTag(filePath);
            
            Bitmap bitmap = WordPress.localImageCache.get(filePath); 
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                BitmapWorkerTask task = new BitmapWorkerTask(imageView, width, height, new BitmapWorkerCallback() {
                    
                    @Override
                    public void onBitmapReady(String path, ImageView imageView, Bitmap bitmap) {
                        imageView.setImageBitmap(bitmap);
                        WordPress.localImageCache.put(path, bitmap);
                    }
                });
                task.execute(filePath);
            }
        }        
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.media_details, menu);
    }
    
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_refresh).setVisible(false);
        menu.findItem(R.id.menu_new_media).setVisible(false);
        menu.findItem(R.id.menu_search).setVisible(false);
        
        if (mIsLocal)
            menu.findItem(R.id.menu_edit_media).setVisible(false);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.menu_delete) {
            Builder builder = new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.confirm_delete_media)
                    .setCancelable(true)
                    .setPositiveButton(R.string.delete, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                                ArrayList<String> ids = new ArrayList<String>(1);
                                ids.add(getMediaId());
                                mCallback.onDeleteMedia(ids);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null);
            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
}
