package org.wordpress.android.ui.posts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.util.AniUtils;

/**
 * Displayed after user selects multiple items from the WP media library to insert into
 * a post - provides a choice between inserting them individually or as a gallery
 */
public class InsertMediaDialog extends AppCompatDialogFragment {
    public enum InsertType {
        INDIVIDUALLY,
        GALLERY
    }

    public enum GalleryType {
        DEFAULT,
        TILED,
        SQUARES,
        CIRCLES,
        SLIDESHOW;

        // overridden to return the actual name used in the gallery shortcode
        @Override
        public String toString() {
            switch (this) {
                case CIRCLES:
                    return "circle";
                case SLIDESHOW:
                    return "slideshow";
                case SQUARES:
                    return "square";
                case TILED:
                    return "rectangular";
                default:
                    return "";
            }
        }
    }

    public interface InsertMediaCallback {
        void onCompleted(@NonNull InsertMediaDialog dialog);
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

    private InsertMediaCallback mCallback;
    private SiteModel mSite;
    private GalleryType mGalleryType;
    private InsertType mInsertType;
    private int mNumColumns;


    public static InsertMediaDialog newInstance(@NonNull InsertMediaCallback callback, @NonNull SiteModel site) {
        InsertMediaDialog dialog = new InsertMediaDialog();
        dialog.setCallback(callback);
        Bundle args = new Bundle();
        args.putSerializable(WordPress.SITE, site);
        dialog.setArguments(args);
        return dialog;
    }

    private void setCallback(@NonNull InsertMediaCallback callback) {
        mCallback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.media_insert_title);

        View view = inflater.inflate(R.layout.insert_media_dialog, container, false);

        mInsertRadioGroup = (RadioGroup) view.findViewById(R.id.radio_group_insert_type);
        mGalleryRadioGroup = (RadioGroup) view.findViewById(R.id.radio_group_gallery_type);
        mNumColumnsContainer = (ViewGroup) view.findViewById(R.id.num_columns_container);
        mNumColumnsSeekBar = (SeekBar) mNumColumnsContainer.findViewById(R.id.seekbar_num_columns);

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

        // self-hosted sites don't support gallery types
        boolean enableGalleryType = mSite != null && mSite.isUsingWpComRestApi();
        if (enableGalleryType) {
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
        } else {
            mGalleryRadioGroup.setVisibility(View.GONE);
            mNumColumnsContainer.setVisibility(View.VISIBLE);
            mGalleryType = GalleryType.DEFAULT;
        }

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

        Button btnCancel = (Button) view.findViewById(R.id.button_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().cancel();
            }
        });

        Button btnOk = (Button) view.findViewById(R.id.button_ok);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onCompleted(InsertMediaDialog.this);
                getDialog().dismiss();
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
            setInsertType(InsertType.GALLERY);
            setGalleryType(GalleryType.DEFAULT);
            setNumColumns(DEFAULT_COLUMN_COUNT, false);
        }
    }

    public InsertType getInsertType() {
        return mInsertType;
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

        ViewGroup container = (ViewGroup) getView().findViewById(R.id.container_gallery_settings);
        boolean enableGalleryTypes = insertType == InsertType.GALLERY;
        if (enableGalleryTypes && container.getVisibility() != View.VISIBLE) {
            AniUtils.fadeIn(container, AniUtils.Duration.SHORT);
        } else if (!enableGalleryTypes && container.getVisibility() == View.VISIBLE) {
            AniUtils.fadeOut(container, AniUtils.Duration.SHORT, View.INVISIBLE);
        }
    }

    public GalleryType getGalleryType() {
        return mGalleryType;
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

    public int getNumColumns() {
        return mNumColumns;
    }

    private void setNumColumns(int numColumns, boolean fromSeekBar) {
        // seekbar is zero-based, so increment the column count if this was called from it
        if (fromSeekBar) {
            mNumColumns = numColumns + 1;
        } else {
            mNumColumns = numColumns;
            mNumColumnsSeekBar.setProgress(numColumns);
        }

        TextView textValue = (TextView) getView().findViewById(R.id.text_num_columns_label);
        if (mNumColumns == 1) {
            textValue.setText(getString(R.string.media_gallery_column_count_single));
        } else {
            textValue.setText(String.format(getString(R.string.media_gallery_column_count_multi), mNumColumns));
        }
    }
}
