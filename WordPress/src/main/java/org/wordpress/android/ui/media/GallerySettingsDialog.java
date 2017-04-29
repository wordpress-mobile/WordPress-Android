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

    public enum InsertType {
        INDIVIDUALLY,
        GALLERY
    }

    public enum GalleryType {
        DEFAULT,
        TILED,
        SQUARES,
        CIRCLES,
        SLIDESHOW
    }

    private static final int DEFAULT_COLUMN_COUNT = 3;
    private static final int MAX_COLUMN_COUNT = 9;

    private static final String STATE_INSERT_TYPE = "STATE_INSERT_TYPE";
    private static final String STATE_NUM_COLUMNS = "STATE_NUM_COLUMNS";
    private static final String STATE_GALLERY_TYPE_ORD = "GALLERY_TYPE_ORD";

    private RadioGroup mInsertRadioGroup;
    private RadioGroup mGalleryRadioGroup;
    private ViewGroup mNumColumnsContainer;
    private SeekBar mNumColumnsSeekBar;

    private GalleryType mGalleryType;
    private InsertType mInsertType;
    private int mNumColumns;

    public static GallerySettingsDialog newInstance() {
        GallerySettingsDialog dialog = new GallerySettingsDialog();
        dialog.setStyle(AppCompatDialogFragment.STYLE_NORMAL, R.style.Theme_AppCompat_Light_Dialog_Alert);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.media_insert_title);
        View view = inflater.inflate(R.layout.media_gallery_settings_dialog, container, false);

        mInsertRadioGroup = (RadioGroup) view.findViewById(R.id.radio_group_insert_type);
        mInsertRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                if (checkedId == R.id.radio_insert_as_gallery) {
                    setInsertType(InsertType.GALLERY);
                } else {
                    setInsertType(InsertType.INDIVIDUALLY);
                }
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

        mNumColumnsContainer = (ViewGroup) view.findViewById(R.id.num_columns_container);
        mNumColumnsSeekBar = (SeekBar) mNumColumnsContainer.findViewById(R.id.seekbar_num_columns);
        mNumColumnsSeekBar.setMax(MAX_COLUMN_COUNT - 1);
        mNumColumnsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setNumColumns(progress, true);
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

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_INSERT_TYPE, mInsertType.ordinal());
        outState.putInt(STATE_GALLERY_TYPE_ORD, mGalleryType.ordinal());
        outState.putInt(STATE_NUM_COLUMNS, mNumColumns);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            int insertTypeOrdinal = savedInstanceState.getInt(STATE_INSERT_TYPE);
            setInsertType(InsertType.values()[insertTypeOrdinal]);
            int galleryTypeOrdinal = savedInstanceState.getInt(STATE_GALLERY_TYPE_ORD);
            setGalleryType(GalleryType.values()[galleryTypeOrdinal]);
            setNumColumns(savedInstanceState.getInt(STATE_NUM_COLUMNS), false);
        } else {
            setInsertType(InsertType.INDIVIDUALLY);
            setGalleryType(GalleryType.DEFAULT);
            setNumColumns(DEFAULT_COLUMN_COUNT, false);
        }
    }

    private void setInsertType(@NonNull InsertType insertType) {
        if (insertType == mInsertType) {
            return;
        }

        mInsertType = insertType;

        @IdRes int radioId = insertType == InsertType.GALLERY
                ? R.id.radio_insert_as_gallery : R.id.radio_insert_individually;
        RadioButton radio = (RadioButton) getView().findViewById(radioId);
        if (!radio.isChecked()) {
            radio.setChecked(true);
        }

        View container = getView().findViewById(R.id.container_gallery_settings);
        boolean showGalleryTypes = insertType == InsertType.GALLERY;
        if (showGalleryTypes && container.getVisibility() != View.VISIBLE) {
            AniUtils.fadeIn(container, AniUtils.Duration.SHORT);
        } else if (!showGalleryTypes && container.getVisibility() == View.VISIBLE) {
            AniUtils.fadeOut(container, AniUtils.Duration.SHORT, View.INVISIBLE);
        }
    }

    private void setGalleryType(@NonNull GalleryType galleryType) {
        if (galleryType == mGalleryType) {
            return;
        }

        mGalleryType = galleryType;

        // column count applies only to thumbnail grid
        boolean showNumColumns = (galleryType == GalleryType.DEFAULT);
        if (showNumColumns && mNumColumnsContainer.getVisibility() != View.VISIBLE) {
            AniUtils.fadeIn(mNumColumnsContainer, AniUtils.Duration.SHORT);
        } else if (!showNumColumns && mNumColumnsContainer.getVisibility() == View.VISIBLE) {
            AniUtils.fadeOut(mNumColumnsContainer, AniUtils.Duration.SHORT, View.INVISIBLE);
        }

        @IdRes final int resId;
        @DrawableRes final int drawableId;
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

        // scale out the gallery type image, then set the new image and scale it back in
        final ImageView imageView = (ImageView) getView().findViewById(R.id.image_gallery_type);
        AniUtils.scaleOut(imageView, View.VISIBLE, AniUtils.Duration.SHORT, new AniUtils.AnimationEndListener() {
            @Override
            public void onAnimationEnd() {
                imageView.setImageResource(drawableId);
                AniUtils.scaleIn(imageView, AniUtils.Duration.SHORT);
            }
        });
    }

    private void setNumColumns(int numColumns, boolean fromSeekBar) {
        // seekbar is zero-based, so increment the column count if this was called from it
        if (fromSeekBar) {
            mNumColumns = numColumns + 1;
        } else {
            mNumColumns = numColumns;
            mNumColumnsSeekBar.setProgress(numColumns);
        }

        TextView textValue = (TextView) getView().findViewById(R.id.text_num_columns_value);
        textValue.setText(Integer.toString(mNumColumns));
    }
}
