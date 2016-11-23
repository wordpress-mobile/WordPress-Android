package org.wordpress.android.ui.media;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ScrollView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.ExpandableHeightGridView;

import java.util.ArrayList;

/**
 * The fragment containing the settings for the media gallery
 */
public class MediaGallerySettingsFragment extends Fragment implements OnCheckedChangeListener {
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

    private View mNumColumnsContainer;
    private View mHeader;

    private CheckBox mRandomOrderCheckbox;

    private boolean mAllowCheckChanged;

    private TextView mTitleView;

    private ScrollView mScrollView;

    private CustomGridAdapter mGridAdapter;

    private MediaGallerySettingsCallback mCallback;


    private enum GalleryType {
        DEFAULT(""),
        TILED("rectangular"),
        SQUARES("square"),
        CIRCLES("circle"),
        SLIDESHOW("slideshow");

        private final String mTag;

        private GalleryType(String tag) {
            mTag = tag;
        }

        public String getTag() {
            return mTag;
        }

        public static GalleryType getTypeFromTag(String tag) {
            if (tag.equals("rectangular"))
                return TILED;
            else if (tag.equals("square"))
                return SQUARES;
            else if (tag.equals("circle"))
                return CIRCLES;
            else if (tag.equals("slideshow"))
                return SLIDESHOW;
            else
                return DEFAULT;
        }

    }

    public interface MediaGallerySettingsCallback {
        public void onReverseClicked();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (MediaGallerySettingsCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement MediaGallerySettingsCallback");
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

        mHeader = view.findViewById(R.id.media_gallery_settings_header);
        mScrollView = (ScrollView) view.findViewById(R.id.media_gallery_settings_content_container);
        mTitleView = (TextView) view.findViewById(R.id.media_gallery_settings_title);

        mNumColumnsContainer = view.findViewById(R.id.media_gallery_settings_num_columns_container);
        int visible = (mType == GalleryType.DEFAULT) ? View.VISIBLE : View.GONE;
        mNumColumnsContainer.setVisibility(visible);

        ExpandableHeightGridView numColumnsGrid = (ExpandableHeightGridView) view.findViewById(R.id.media_gallery_num_columns_grid);
        numColumnsGrid.setExpanded(true);
        ArrayList<String> list = new ArrayList<String>(9);
        for (int i = 1; i <= 9; i++) {
            list.add(i + "");
        }

        mGridAdapter = new CustomGridAdapter(mNumColumns);
        numColumnsGrid.setAdapter(mGridAdapter);

        mThumbnailCheckbox = (CheckBox) view.findViewById(R.id.media_gallery_type_thumbnail_grid);
        mTiledCheckbox = (CheckBox) view.findViewById(R.id.media_gallery_type_tiled);
        mSquaresCheckbox = (CheckBox) view.findViewById(R.id.media_gallery_type_squares);
        mCirclesCheckbox = (CheckBox) view.findViewById(R.id.media_gallery_type_circles);
        mSlideshowCheckbox = (CheckBox) view.findViewById(R.id.media_gallery_type_slideshow);

        setType(mType.getTag());

        mThumbnailCheckbox.setOnCheckedChangeListener(this);
        mTiledCheckbox.setOnCheckedChangeListener(this);
        mSquaresCheckbox.setOnCheckedChangeListener(this);
        mCirclesCheckbox.setOnCheckedChangeListener(this);

        mSlideshowCheckbox.setOnCheckedChangeListener(this);

        mRandomOrderCheckbox = (CheckBox) view.findViewById(R.id.media_gallery_random_checkbox);
        mRandomOrderCheckbox.setChecked(mIsRandomOrder);
        mRandomOrderCheckbox.setOnCheckedChangeListener(this);

        Button reverseButton = (Button) view.findViewById(R.id.media_gallery_settings_reverse_button);
        reverseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onReverseClicked();
            }
        });

        return view;
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null)
            return;

        mNumColumns = savedInstanceState.getInt(STATE_NUM_COLUMNS);
        int galleryTypeOrdinal = savedInstanceState.getInt(STATE_GALLERY_TYPE_ORD);
        mType = GalleryType.values()[galleryTypeOrdinal];
        mIsRandomOrder = savedInstanceState.getBoolean(STATE_RANDOM_ORDER);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_NUM_COLUMNS, mNumColumns);
        outState.putBoolean(STATE_RANDOM_ORDER, mIsRandomOrder);
        outState.putInt(STATE_GALLERY_TYPE_ORD, mType.ordinal());
    }

    @Override
    public void onCheckedChanged(CompoundButton button, boolean checked) {
        if (!mAllowCheckChanged)
            return;

        // the checkboxes for types are mutually exclusive, so when one is set,
        // the others must be unset. Disable the checkChange listener during this time,
        // and re-enable it once done.
        mAllowCheckChanged = false;

        int numColumnsContainerVisible = View.GONE;
        int i = button.getId();
        if (i == R.id.media_gallery_type_thumbnail_grid) {
            numColumnsContainerVisible = View.VISIBLE;

            mType = GalleryType.DEFAULT;
            mThumbnailCheckbox.setChecked(true);
            mSquaresCheckbox.setChecked(false);
            mTiledCheckbox.setChecked(false);
            mCirclesCheckbox.setChecked(false);
            mSlideshowCheckbox.setChecked(false);
        } else if (i == R.id.media_gallery_type_tiled) {
            mType = GalleryType.TILED;
            mThumbnailCheckbox.setChecked(false);
            mTiledCheckbox.setChecked(true);
            mSquaresCheckbox.setChecked(false);
            mCirclesCheckbox.setChecked(false);
            mSlideshowCheckbox.setChecked(false);
        } else if (i == R.id.media_gallery_type_squares) {
            mType = GalleryType.SQUARES;
            mThumbnailCheckbox.setChecked(false);
            mTiledCheckbox.setChecked(false);
            mSquaresCheckbox.setChecked(true);
            mCirclesCheckbox.setChecked(false);
            mSlideshowCheckbox.setChecked(false);
        } else if (i == R.id.media_gallery_type_circles) {
            mType = GalleryType.CIRCLES;
            mThumbnailCheckbox.setChecked(false);
            mSquaresCheckbox.setChecked(false);
            mTiledCheckbox.setChecked(false);
            mCirclesCheckbox.setChecked(true);
            mSlideshowCheckbox.setChecked(false);
        } else if (i == R.id.media_gallery_type_slideshow) {
            mType = GalleryType.SLIDESHOW;
            mThumbnailCheckbox.setChecked(false);
            mSquaresCheckbox.setChecked(false);
            mTiledCheckbox.setChecked(false);
            mCirclesCheckbox.setChecked(false);
            mSlideshowCheckbox.setChecked(true);
        } else if (i == R.id.media_gallery_random_checkbox) {
            numColumnsContainerVisible = mNumColumnsContainer.getVisibility();
            mIsRandomOrder = checked;
        }

        mNumColumnsContainer.setVisibility(numColumnsContainerVisible);

        mAllowCheckChanged = true;
    }

    private class CustomGridAdapter extends BaseAdapter implements OnCheckedChangeListener {
        private boolean mAllowCheckChanged;
        private final SparseBooleanArray mCheckedPositions;

        public CustomGridAdapter(int selection) {
            mAllowCheckChanged = true;
            mCheckedPositions = new SparseBooleanArray(9);
            setSelection(selection);
        }

        // when a number of columns is checked, the numbers less than
        // the one chose are also set to checked on the ui.
        // e.g. when 3 is checked, 1 and 2 are as well.
        private void setSelection(int selection) {
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

    public View getDragView() {
        return mHeader;
    }

    public void onPanelExpanded() {
        mTitleView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.gallery_arrow_dropdown_open, 0);
        mScrollView.scrollTo(0, 0);
    }

    public void onPanelCollapsed() {
        mTitleView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.gallery_arrow_dropdown_closed, 0);
    }

    public void setRandom(boolean random) {
        mIsRandomOrder = random;
        mRandomOrderCheckbox.setChecked(mIsRandomOrder);
    }

    public boolean isRandom() {
        return mIsRandomOrder;
    }

    public void setType(String type) {
        mType = GalleryType.getTypeFromTag(type);
        switch (mType) {
            case CIRCLES:
                mCirclesCheckbox.setChecked(true);
                break;
            case DEFAULT:
                mThumbnailCheckbox.setChecked(true);
                break;
            case SLIDESHOW:
                mSlideshowCheckbox.setChecked(true);
                break;
            case SQUARES:
                mSquaresCheckbox.setChecked(true);
                break;
            case TILED:
                mTiledCheckbox.setChecked(true);
                break;
        }
    }

    public String getType() {
        return mType.getTag();
    }

    public void setNumColumns(int numColumns) {
        mNumColumns = numColumns;
        mGridAdapter.setSelection(numColumns);
        mGridAdapter.notifyDataSetChanged();
    }

    public int getNumColumns() {
        return mNumColumns;
    }
}
