package org.wordpress.android.ui.media;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.xtremelabs.imageutils.BitmapListener;
import com.xtremelabs.imageutils.ImageLoader;
import com.xtremelabs.imageutils.ImageReturnedFrom;

import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.GetMediaTask.Callback;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;

public class MediaItemListFragment extends ListFragment {

    private ApiHelper.GetMediaTask mGetMediaTask;
    private MediaCursorAdapter mAdapter;
    private Cursor mCursor;
    
    private ImageLoader mImageLoader;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mImageLoader = ImageLoader.buildImageLoaderForSupportFragment(this);
        return super.onCreateView(inflater, container, savedInstanceState);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mImageLoader.destroy();
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        
        Log.d("WordPress", "MediaItemListFragment onResume()");
        
        if(WordPress.getCurrentBlog() != null)
            refreshMedia();
        
        loadCursor();
        
        mAdapter = new MediaCursorAdapter(getActivity(), mCursor, 0);
        setListAdapter(mAdapter);
        
    }

    private void loadCursor() {
        Blog blog = WordPress.getCurrentBlog();
        if(blog != null) {
            String blogId = String.valueOf(blog.getBlogId());
            mCursor = WordPress.wpDB.getMediaFilesForBlog(blogId);
        }
    }
    
    private void refreshMedia() {
        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(WordPress.getCurrentBlog());
        
        mGetMediaTask = new ApiHelper.GetMediaTask(mCallback);
        mGetMediaTask.execute(apiArgs);
    }

    
    class MediaCursorAdapter extends CursorAdapter {

        public MediaCursorAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        @Override
        public void bindView(final View view, Context context, Cursor cursor) {
            TextView title = (TextView) view.findViewById(R.id.media_listitem_title);
            title.setText(cursor.getString(cursor.getColumnIndex("title")));
            
            String fileURL = cursor.getString(cursor.getColumnIndex("fileURL"));
            ImageView thumbnail = (ImageView) view.findViewById(R.id.media_listitem_thumbnail);
            if(isValidImage(fileURL))
                loadThumbnail(thumbnail, context, cursor);
            else
                thumbnail.setImageDrawable(null);
                
        }

        private void loadThumbnail(final ImageView thumbnail, Context context, Cursor cursor) {
            
            
            Blog blog = WordPress.getCurrentBlog(); 
            if(blog != null) {
                String blogId = String.valueOf(blog.getBlogId());
                File blogDir = context.getDir(blogId, Context.MODE_PRIVATE);
                final String imgPath = blogDir.getAbsolutePath() + File.separator + cursor.getString(cursor.getColumnIndex("uuid"));
                
                Uri imageUri;
                
                final File file = new File(imgPath);
                if(file.exists()) {
                    Log.d("WordPress", "MCA: File " + imgPath + " exists");
                    // show image
                    imageUri = Uri.fromFile(file);
                    mImageLoader.loadImage(thumbnail, imageUri.toString());
                } else {
                    Log.d("WordPress", "MCA: File " + imgPath + " does not exist");
                    imageUri = Uri.parse(cursor.getString(cursor.getColumnIndex("thumbnailURL")));
                    

                    mImageLoader.loadImage(imageUri.toString(), new BitmapListener() {
                        
                        @Override
                        public void onImageLoadError(String error) {
                            
                        }
                        
                        @Override
                        public void onImageAvailable(Bitmap bitmap, ImageReturnedFrom arg1) {
                            thumbnail.setImageBitmap(bitmap);
                            saveBitmapToFile(file, bitmap);
                            
                        }
                       
                    });
                }
                
            }
        }

        private void saveBitmapToFile(File file, Bitmap bitmap) {
            try {
               OutputStream out = new FileOutputStream(file);
               bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
               out.flush();
               out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup root) {
            
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.media_listitem, root, false);
            
            return view;
        }
        
    }
    
    private boolean isValidImage(String url) {
        if(url.endsWith(".png") || url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".gif"))
            return true;
        return false;
    }

    private Callback mCallback = new Callback() {
        
        @Override
        public void onSuccess() {
            loadCursor();
            
            mAdapter.changeCursor(mCursor);
        }
    };    
    
}
