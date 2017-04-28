package org.wordpress.android.ui.media;

import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.AniUtils;

/**
 * The fragment containing the settings for the media gallery
 */
public class GallerySettingsDialog extends AppCompatDialogFragment {
    private static final int DEFAULT_COLUMN_COUNT = 3;
    private static final int MAX_COLUMN_COUNT = 9;

    private static final String STATE_NUM_COLUMNS = "STATE_NUM_COLUMNS";
    private static final String STATE_GALLERY_TYPE_ORD = "GALLERY_TYPE_ORD";

    private ViewGroup mNumColumnsContainer;
    private RadioGroup mGalleryRadioGroup;
    private SeekBar mNumColumnsSeekBar;

    private GalleryType mGalleryType;
    private int mNumColumns;

    private enum GalleryType {
        DEFAULT,
        TILED,
        SQUARES,
        CIRCLES,
        SLIDESHOW
    }

    public static GallerySettingsDialog newInstance() {
        GallerySettingsDialog dialog = new GallerySettingsDialog();
        dialog.setStyle(AppCompatDialogFragment.STYLE_NO_TITLE, R.style.Theme_AppCompat_Light);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.media_gallery_settings_dialog, container, false);

        mNumColumnsContainer = (ViewGroup) view.findViewById(R.id.num_columns_container);
        mNumColumnsSeekBar = (SeekBar) view.findViewById(R.id.seekbar_num_columns);
        mNumColumnsSeekBar.setMax(MAX_COLUMN_COUNT - 1);
        mNumColumnsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setNumColumns(progress + 1);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // noop
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // noop
            }
        });

        mGalleryRadioGroup = (RadioGroup) view.findViewById(R.id.radio_group_gallery_type);
        mGalleryRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                GalleryType galleryType;
                switch (checkedId) {
                    case R.id.radio_circles:
                        galleryType = GalleryType.CIRCLES;
                        break;
                    case R.id.radio_slideshow:
                        galleryType = GalleryType.SLIDESHOW;
                        break;
                    case R.id.radio_squares:
                        galleryType = GalleryType.SQUARES;
                        break;
                    case R.id.radio_tiled:
                        galleryType = GalleryType.TILED;
                        break;
                    default:
                        galleryType = GalleryType.DEFAULT;
                        break;
                }
                setGalleryType(galleryType);
            }
        });

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_NUM_COLUMNS, mNumColumns);
        outState.putInt(STATE_GALLERY_TYPE_ORD, mGalleryType.ordinal());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            setNumColumns(savedInstanceState.getInt(STATE_NUM_COLUMNS));
            int galleryTypeOrdinal = savedInstanceState.getInt(STATE_GALLERY_TYPE_ORD);
            setGalleryType(GalleryType.values()[galleryTypeOrdinal]);
        } else {
            setGalleryType(GalleryType.DEFAULT);
            setNumColumns(DEFAULT_COLUMN_COUNT);
        }
    }

    private void setGalleryType(@NonNull GalleryType galleryType) {
        if (galleryType == mGalleryType) {
            return;
        }

        mGalleryType = galleryType;

        boolean showNumColumns = (galleryType == GalleryType.DEFAULT);
        if (showNumColumns && mNumColumnsContainer.getVisibility() != View.VISIBLE) {
            AniUtils.fadeIn(mNumColumnsContainer, AniUtils.Duration.SHORT);
        } else if (!showNumColumns && mNumColumnsContainer.getVisibility() == View.VISIBLE) {
            AniUtils.fadeOut(mNumColumnsContainer, AniUtils.Duration.SHORT);
        }

        @IdRes int resId;
        @DrawableRes int drawableId;
        switch (galleryType) {
            case CIRCLES:
                resId = R.id.radio_circles;
                drawableId = R.drawable.gallery_icon_circles;
                break;
            case SLIDESHOW:
                resId = R.id.radio_slideshow;
                drawableId = R.drawable.gallery_icon_slideshow;
                break;
            case SQUARES:
                resId = R.id.radio_squares;
                drawableId = R.drawable.gallery_icon_squares;
                break;
            case TILED:
                resId = R.id.radio_tiled;
                drawableId = R.drawable.gallery_icon_tiled;
                break;
            default:
                resId = R.id.radio_thumbnail_grid;
                drawableId = R.drawable.gallery_icon_thumbnailgrid;
                break;
        }

        RadioButton radio = (RadioButton) mGalleryRadioGroup.findViewById(resId);
        if (!radio.isChecked()) {
            radio.setChecked(true);
        }

        ImageView imageView = (ImageView) getView().findViewById(R.id.image_gallery_type);
        imageView.setImageResource(drawableId);
    }

    public void setNumColumns(int numColumns) {
        mNumColumns = numColumns;
        if (isAdded()) {
            mNumColumnsSeekBar.setProgress(numColumns - 1);
            TextView textValue = (TextView) getView().findViewById(R.id.text_num_columns_value);
            textValue.setText(Integer.toString(numColumns));
        }
    }

    public int getNumColumns() {
        return mNumColumns;
    }
}
