package org.wordpress.android.ui.media;

import java.util.ArrayList;

import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.GridView;

import com.actionbarsherlock.app.SherlockFragment;

import org.wordpress.android.R;

public class MediaGallerySettingsFragment extends SherlockFragment implements OnCheckedChangeListener {

    private static final int DEFAULT_THUMBNAIL_COLUMN_COUNT = 3;

    private static final String STATE_NUM_COLUMNS = "STATE_NUM_COLUMNS";
    private static final String STATE_GALLERY_TYPE_ORD = "GALLERY_TYPE_ORD";
    private static final String STATE_RANDOM_ORDER = "STATE_RANDOM_ORDER";
    
    private CheckBox mThumbnailCheckbox;
    private CheckBox mSquaresCheckbox;
    private CheckBox mTiledCheckbox;
    private CheckBox mCirclesCheckbox;
    private CheckBox mSlideshowCheckbox;
    
    private GalleryType mType;
    private int mNumColumns;
    private boolean mIsRandomOrder;
    
    private GridView mNumColumnsGrid;
    private View mNumColumnsContainer;

    private CheckBox mRandomOrderCheckbox;

    private boolean mAllowCheckChanged;

    private enum GalleryType {
        DEFAULT(""),
        TILED("rectangular"),
        SQUARES("square"),
        CIRCLES("circle"),
        SLIDESHOW("slideshow");
        
        private String mTag;

        private GalleryType(String tag) {
            mTag = tag;
        }
        
        public String getTag() {
            return mTag;
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mAllowCheckChanged = true;
        mType = GalleryType.DEFAULT;
        mNumColumns = DEFAULT_THUMBNAIL_COLUMN_COUNT;
        mIsRandomOrder = false;
        
        restoreState(savedInstanceState);
        
        View view = inflater.inflate(R.layout.media_gallery_settings_fragment, container, false);
        
        mNumColumnsContainer = view.findViewById(R.id.media_gallery_settings_num_columns_container);
        int visible = (mType == GalleryType.DEFAULT) ? View.VISIBLE : View.GONE;
        mNumColumnsContainer.setVisibility(visible);
        
        mNumColumnsGrid = (GridView) view.findViewById(R.id.media_gallery_num_columns_grid);
        ArrayList<String> list = new ArrayList<String>(9);
        for (int i = 1; i <= 9; i++) {
            list.add(i + "");
        }
        mNumColumnsGrid.setAdapter(new CustomGridAdapter(mNumColumns));
        
        
        mThumbnailCheckbox = (CheckBox) view.findViewById(R.id.media_gallery_type_thumbnail_grid);
        mThumbnailCheckbox.setChecked(mType == GalleryType.DEFAULT);
        mThumbnailCheckbox.setOnCheckedChangeListener(this);
        
        mTiledCheckbox = (CheckBox) view.findViewById(R.id.media_gallery_type_tiled);
        mTiledCheckbox.setChecked(mType == GalleryType.TILED);
        mTiledCheckbox.setOnCheckedChangeListener(this);
        
        mSquaresCheckbox = (CheckBox) view.findViewById(R.id.media_gallery_type_squares);
        mSquaresCheckbox.setChecked(mType == GalleryType.SQUARES);
        mSquaresCheckbox.setOnCheckedChangeListener(this);
        
        mCirclesCheckbox = (CheckBox) view.findViewById(R.id.media_gallery_type_circles);
        mCirclesCheckbox.setChecked(mType == GalleryType.CIRCLES);
        mCirclesCheckbox.setOnCheckedChangeListener(this);
        
        mSlideshowCheckbox = (CheckBox) view.findViewById(R.id.media_gallery_type_slideshow);
        mSlideshowCheckbox.setChecked(mType == GalleryType.SLIDESHOW);
        mSlideshowCheckbox.setOnCheckedChangeListener(this);
        
        mRandomOrderCheckbox = (CheckBox) view.findViewById(R.id.media_gallery_random_checkbox);
        mRandomOrderCheckbox.setChecked(mIsRandomOrder);
        mRandomOrderCheckbox.setOnCheckedChangeListener(this);
        
        return view;
    }
    
    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null)
            return;
        
        mNumColumns = savedInstanceState.getInt(STATE_NUM_COLUMNS);
        int galleryTypeOrdinal = savedInstanceState.getInt(STATE_GALLERY_TYPE_ORD);
        mType = GalleryType.values()[galleryTypeOrdinal];
        Log.d("ASDF", "restored ordinal: " + mType.ordinal());
        mIsRandomOrder = savedInstanceState.getBoolean(STATE_RANDOM_ORDER);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_NUM_COLUMNS, mNumColumns);
        outState.putBoolean(STATE_RANDOM_ORDER, mIsRandomOrder);
        Log.d("ASDF", "saving ordinal: " + mType.ordinal());
        outState.putInt(STATE_GALLERY_TYPE_ORD, mType.ordinal());
    }

    @Override
    public void onCheckedChanged(CompoundButton button, boolean checked) {
        
        if (!mAllowCheckChanged)
            return;
        
        mAllowCheckChanged = false;
        
        int numColumnsContainerVisible = View.GONE;
        switch (button.getId()) {
            case R.id.media_gallery_type_thumbnail_grid:
                numColumnsContainerVisible = View.VISIBLE;
                
                mType = GalleryType.DEFAULT;
                mThumbnailCheckbox.setChecked(true);
                mSquaresCheckbox.setChecked(false);
                mTiledCheckbox.setChecked(false);
                mCirclesCheckbox.setChecked(false);
                mSlideshowCheckbox.setChecked(false);
                break;
            case R.id.media_gallery_type_tiled:
                mType = GalleryType.TILED;
                mThumbnailCheckbox.setChecked(false);
                mTiledCheckbox.setChecked(true);
                mSquaresCheckbox.setChecked(false);
                mCirclesCheckbox.setChecked(false);
                mSlideshowCheckbox.setChecked(false);
                break;
            case R.id.media_gallery_type_squares:
                mType = GalleryType.SQUARES;
                mThumbnailCheckbox.setChecked(false);
                mTiledCheckbox.setChecked(false);
                mSquaresCheckbox.setChecked(true);
                mCirclesCheckbox.setChecked(false);
                mSlideshowCheckbox.setChecked(false);
                break;
            case R.id.media_gallery_type_circles:
                mType = GalleryType.CIRCLES;
                mThumbnailCheckbox.setChecked(false);
                mSquaresCheckbox.setChecked(false);
                mTiledCheckbox.setChecked(false);
                mCirclesCheckbox.setChecked(true);
                mSlideshowCheckbox.setChecked(false);
                break;
            case R.id.media_gallery_type_slideshow:
                mType = GalleryType.SLIDESHOW;
                mThumbnailCheckbox.setChecked(false);
                mSquaresCheckbox.setChecked(false);
                mTiledCheckbox.setChecked(false);
                mCirclesCheckbox.setChecked(false);
                mSlideshowCheckbox.setChecked(true);
                break;
            case R.id.media_gallery_random_checkbox:
                numColumnsContainerVisible = mNumColumnsContainer.getVisibility();
                mIsRandomOrder = checked;
                break;
        }
        
        Log.d("ASDF", "type changed, ordinal: " + mType.ordinal());
        
        mNumColumnsContainer.setVisibility(numColumnsContainerVisible);
        
        mAllowCheckChanged = true;
    }

    private class CustomGridAdapter extends BaseAdapter implements OnCheckedChangeListener {

        private boolean mAllowCheckChanged;
        private SparseBooleanArray mCheckedPositions;
        
        public CustomGridAdapter(int selection) {
            mAllowCheckChanged = true;
            mCheckedPositions = new SparseBooleanArray(9);
            for (int i = 0; i < 9; i++){
                if (i + 1 <= selection)
                    mCheckedPositions.put(i, true);
                else
                    mCheckedPositions.put(i, false);
            }
        }
        
        @Override
        public int getCount() {
            return 9;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            CheckBox checkbox = (CheckBox) inflater.inflate(R.layout.media_gallery_column_checkbox, parent, false);
            checkbox.setChecked(mCheckedPositions.get(position));
            checkbox.setTag(position);
            checkbox.setText(String.valueOf(position + 1));
            checkbox.setOnCheckedChangeListener(this);
            
            return checkbox;
        }

        @Override
        public void onCheckedChanged(CompoundButton button, boolean checked) {
            
            if (mAllowCheckChanged) {
            
                mAllowCheckChanged = false;
                
                int position = (Integer) button.getTag();
                mNumColumns = position + 1;
                
                int count = mCheckedPositions.size();
                for (int i = 0; i < count; i++) {
                    if (i <= position)
                        mCheckedPositions.put(i, true);
                    else
                        mCheckedPositions.put(i, false);
                }
                notifyDataSetChanged();
                mAllowCheckChanged = true;
            }
        }
        
    }
    
}
