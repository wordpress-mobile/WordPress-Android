package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * topmost view in post adapter when showing tag preview - displays tag name and follow button
 */
public class ReaderTagHeaderView extends LinearLayout {

    private ReaderFollowButton mFollowButton;
    private WPNetworkImageView mImageView;
    private TextView mTxtAttribution;
    private ReaderTag mCurrentTag;

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
        mImageView = (WPNetworkImageView) view.findViewById(R.id.image_tag_header);
        mTxtAttribution = (TextView) view.findViewById(R.id.text_attribution);
    }

    public void setCurrentTag(final ReaderTag tag) {
        if (tag == null) return;

        mCurrentTag = tag;

        TextView txtTagName = (TextView) findViewById(R.id.text_tag);
        txtTagName.setText(ReaderUtils.makeHashTag(tag.getTagSlug()));

        mTxtAttribution.setText(null);
        mImageView.setImageDrawable(null);

        if (ReaderUtils.isLoggedOutReader()) {
            mFollowButton.setVisibility(View.GONE);
        } else {
            mFollowButton.setVisibility(View.VISIBLE);
            mFollowButton.setIsFollowed(ReaderTagTable.isFollowedTagName(tag.getTagSlug()));
            mFollowButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollowStatus();
                }
            });
            getImageAndAttribution();
        }
    }

    /*
    {
	"body": {
		"images": [
			{
				"attachment_id": 3594,
				"author": "Eileen On",
				"blog_id": 75481429,
				"blog_title": "Eileen On",
				"blog_url": "http://eileenon.wordpress.com",
				"feed_id": 24565206,
				"height": 3387,
				"post_id": 3593,
				"post_title": "Picture Postcard - Knoxville Tennessee",
				"post_url": "https://eileenon.wordpress.com/?p=3593",
				"railcar": {
					"fetch_algo": "read:tag:image/1",
					"fetch_lang": "en",
					"fetch_position": 0,
					"fetch_query": "knoxville",
					"railcar": "UQ!o&Ik&l!IQ",
					"rec_blog_id": 75481429,
					"rec_post_id": 3593,
					"rec_url": "eileenon.files.wordpress.com/2016/10/postcard-tennessee-knoxville.jpg"
				},
				"url": "eileenon.files.wordpress.com/2016/10/postcard-tennessee-knoxville.jpg",
				"width": 2406
			}
		]
	},
	"code": 200,
	"headers": [
		{
			"name": "Content-Type",
			"value": "application/json"
		}
	]
}
     */
    private void getImageAndAttribution() {
        if (mCurrentTag == null) return;

        String tagNameForApi = ReaderUtils.sanitizeWithDashes(mCurrentTag.getTagSlug());
        String path = "read/tags/" + tagNameForApi + "/images?number=1";

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                JSONObject jsonImages = JSONUtils.getJSONChild(jsonObject, "body/images");
                if (jsonImages == null) return;

                String imageUrl = JSONUtils.getString(jsonImages, "url");
                mImageView.setImageUrl(imageUrl, WPNetworkImageView.ImageType.PHOTO_ROUNDED);

                String author = JSONUtils.getString(jsonImages, "author");
                String blogTitle = JSONUtils.getString(jsonImages, "blog_title");
                boolean hasAuthor = !author.isEmpty();
                boolean hasTitle = !blogTitle.isEmpty();
                String attribution;
                if (hasAuthor && hasTitle && !author.equalsIgnoreCase(blogTitle)) {
                    attribution = getContext().getString(R.string.reader_photo_by, author + ", " + blogTitle);
                } else if (hasAuthor) {
                    attribution = getContext().getString(R.string.reader_photo_by, author);
                } else if (hasTitle) {
                    attribution = getContext().getString(R.string.reader_photo_by, blogTitle);
                } else {
                    attribution = null;
                }
                mTxtAttribution.setText(attribution);
            }
        };

        WordPress.getRestClientUtilsV1_1().post(path, listener, null);
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
