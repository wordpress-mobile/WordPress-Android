package org.wordpress.android.ui.media;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;

import org.wordpress.android.R;
import org.wordpress.android.ui.ExpandableHeightGridView;

import java.util.ArrayList;

/**
 * The fragment containing the settings for the media gallery
 */
public class GallerySettingsDialog extends DialogFragment {
    private static final int DEFAULT_THUMBNAIL_COLUMN_COUNT = 3;

    private static final String STATE_NUM_COLUMNS = "STATE_NUM_COLUMNS";
    private static final String STATE_GALLERY_TYPE_ORD = "GALLERY_TYPE_ORD";

    private View mNumColumnsContainer;
    private ScrollView mScrollView;
    private RadioGroup mGalleryRadioGroup;

    private GalleryType mType;
    private int mNumColumns;

    private boolean mAllowCheckChanged;

    private CustomGridAdapter mGridAdapter;

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

    public static GallerySettingsDialog newInstance() {
        GallerySettingsDialog dialog = new GallerySettingsDialog();
        dialog.setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mAllowCheckChanged = true;
        mType = GalleryType.DEFAULT;
        mNumColumns = DEFAULT_THUMBNAIL_COLUMN_COUNT;

        View view = inflater.inflate(R.layout.media_gallery_settings_dialog, container, false);

        mScrollView = (ScrollView) view.findViewById(R.id.media_gallery_settings_content_container);
        mGalleryRadioGroup = (RadioGroup) view.findViewById(R.id.radio_group_gallery_type);

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

        restoreState(savedInstanceState);
        setType(mType.getTag());

        return view;
    }

    private RadioButton getRadioButtonForType(@NonNull GalleryType type) {
        if (!isAdded()) {
            return null;
        }

        @IdRes int resId;
        switch (type) {
            case CIRCLES:
                resId = R.id.radio_circles;
                break;
            case SLIDESHOW:
                resId = R.id.radio_slideshow;
                break;
            case SQUARES:
                resId = R.id.radio_squares;
                break;
            case TILED:
                resId = R.id.radio_tiled;
                break;
            default:
                resId = R.id.radio_thumbnail_grid;
                break;
        }
        return (RadioButton) mGalleryRadioGroup.findViewById(resId);
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null)
            return;

        mNumColumns = savedInstanceState.getInt(STATE_NUM_COLUMNS);
        int galleryTypeOrdinal = savedInstanceState.getInt(STATE_GALLERY_TYPE_ORD);
        mType = GalleryType.values()[galleryTypeOrdinal];
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_NUM_COLUMNS, mNumColumns);
        outState.putInt(STATE_GALLERY_TYPE_ORD, mType.ordinal());
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
            for (int i = 0; i < 9; i++) {
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

    public void setType(String type) {
        mType = GalleryType.getTypeFromTag(type);
        RadioButton radio = getRadioButtonForType(mType);
        if (radio != null) {
            radio.setChecked(true);
            boolean showNumColumns = (mType == GalleryType.DEFAULT);
            mNumColumnsContainer.setVisibility(showNumColumns ? View.VISIBLE : View.GONE);
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
