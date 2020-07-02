package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.ui.reader.models.ReaderTagHeaderInfo;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.util.HashMap;

/**
 * topmost view in post adapter when showing tag preview - displays tag name and follow button
 */
public class ReaderTagHeaderView extends RelativeLayout {
    private ReaderFollowButton mFollowButton;
    private ImageView mImageView;
    private TextView mTxtAttribution;
    private ReaderTag mCurrentTag;

    private static final ReaderTagHeaderInfoList TAG_INFO_CACHE = new ReaderTagHeaderInfoList();

    public ReaderTagHeaderView(Context context) {
        super(context);
        initView(context);
    }

    public ReaderTagHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ReaderTagHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        View view = inflate(context, R.layout.reader_tag_header_view, this);
        mFollowButton = (ReaderFollowButton) view.findViewById(R.id.follow_button);
        mImageView = view.findViewById(R.id.image_tag_header);
        mTxtAttribution = view.findViewById(R.id.text_attribution);
    }

    public void setCurrentTag(final ReaderTag tag) {
        if (tag == null) {
            return;
        }

        boolean isTagChanged = !ReaderTag.isSameTag(tag, mCurrentTag);

        if (isTagChanged) {
            mTxtAttribution.setText(null);
            ImageManager.getInstance().cancelRequestAndClearImageView(mImageView);
            mCurrentTag = tag;
        }

        TextView txtTagName = findViewById(R.id.text_tag);
        txtTagName.setText(tag.getLabel());

        // use cached info if it's available, otherwise request it if the tag has changed
        if (TAG_INFO_CACHE.hasInfoForTag(tag)) {
            setTagHeaderInfo(TAG_INFO_CACHE.getInfoForTag(tag));
        } else if (isTagChanged) {
            getTagHeaderInfo();
        }

        /*if (ReaderUtils.isLoggedOutReader()) {
            mFollowButton.setVisibility(View.GONE);
        } else {*/
        mFollowButton.setVisibility(View.VISIBLE);
        mFollowButton.setIsFollowed(ReaderTagTable.isFollowedTagName(tag.getTagSlug()));
        mFollowButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFollowStatus();
            }
        });
//        }
    }

    private void setTagHeaderInfo(final ReaderTagHeaderInfo info) {
        int imageWidth = mImageView.getWidth();
        int imageHeight = getContext().getResources().getDimensionPixelSize(R.dimen.reader_tag_header_image_height);
        String photonUrl = PhotonUtils.getPhotonImageUrl(info.getImageUrl(), imageWidth, imageHeight);
        ImageManager.getInstance().load(mImageView, ImageType.PHOTO, photonUrl, ScaleType.CENTER_CROP);

        // show attribution line - author name when available, otherwise blog name or nothing
        if (info.hasAuthorName()) {
            mTxtAttribution.setText(getContext().getString(R.string.reader_photo_by, info.getAuthorName()));
        } else if (info.hasBlogName()) {
            mTxtAttribution.setText(getContext().getString(R.string.reader_photo_by, info.getBlogName()));
        }

        // show the source post when the attribution line is clicked
        if (info.hasSourcePost()) {
            mTxtAttribution.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    ReaderActivityLauncher
                            .showReaderPostDetail(view.getContext(), info.getSourceBlogId(), info.getSourcePostId());
                }
            });
        } else {
            mTxtAttribution.setOnClickListener(null);
        }
    }

    /*
     * performs a GET request for the info we display here
     */
    private void getTagHeaderInfo() {
        if (mCurrentTag == null) {
            return;
        }

        String tagNameForApi = ReaderUtils.sanitizeWithDashes(mCurrentTag.getTagSlug());
        String path = "read/tags/" + tagNameForApi + "/images?number=1";

        WordPress.getRestClientUtilsV1_2().get(path, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject == null) {
                    return;
                }

                JSONArray jsonArray = jsonObject.optJSONArray("images");
                if (jsonArray == null) {
                    return;
                }

                JSONObject jsonImage = jsonArray.optJSONObject(0);
                if (jsonImage == null) {
                    return;
                }

                // current endpoint doesn't include the protocol
                String url = JSONUtils.getString(jsonImage, "url");
                if (!url.startsWith("http")) {
                    url = "https://" + url;
                }

                ReaderTagHeaderInfo info = new ReaderTagHeaderInfo();
                info.setImageUrl(url);
                info.setAuthorName(JSONUtils.getString(jsonImage, "author"));
                info.setBlogName(JSONUtils.getString(jsonImage, "blog_title"));
                info.setSourceBlogId(jsonImage.optLong("blog_id"));
                info.setSourcePostId(jsonImage.optLong("post_id"));

                // add to cached list then display it
                TAG_INFO_CACHE.setInfoForTag(mCurrentTag, info);
                setTagHeaderInfo(info);
            }
        }, null);
    }

    /*
     * cache of tag header info
     */
    private static class ReaderTagHeaderInfoList extends HashMap<String, ReaderTagHeaderInfo> {
        public ReaderTagHeaderInfo getInfoForTag(ReaderTag tag) {
            return this.get(getKeyForTag(tag));
        }

        public boolean hasInfoForTag(ReaderTag tag) {
            return this.containsKey(getKeyForTag(tag));
        }

        public void setInfoForTag(ReaderTag tag, ReaderTagHeaderInfo info) {
            this.put(getKeyForTag(tag), info);
        }

        private String getKeyForTag(ReaderTag tag) {
            return tag.getTagSlug();
        }
    }

    private void toggleFollowStatus() {
        if (mCurrentTag == null || !NetworkUtils.checkConnection(getContext())) {
            return;
        }

        final boolean isAskingToFollow = !ReaderTagTable.isFollowedTagName(mCurrentTag.getTagSlug());

        ReaderActions.ActionListener listener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (getContext() == null) {
                    return;
                }
                mFollowButton.setEnabled(true);
                if (!succeeded) {
                    int errResId = isAskingToFollow ? R.string.reader_toast_err_add_tag : R.string.reader_toast_err_remove_tag;
                    ToastUtils.showToast(getContext(), errResId);
                    mFollowButton.setIsFollowed(!isAskingToFollow);
                }
            }
        };

        mFollowButton.setEnabled(false);

        boolean success;
        if (isAskingToFollow) {
            success = ReaderTagActions.addTag(mCurrentTag, listener);
        } else {
            success = ReaderTagActions.deleteTag(mCurrentTag, listener);
        }

        if (success) {
            mFollowButton.setIsFollowedAnimated(isAskingToFollow);
        }
    }
}
