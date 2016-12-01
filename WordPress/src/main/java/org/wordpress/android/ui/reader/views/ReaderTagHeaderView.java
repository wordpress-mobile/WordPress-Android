package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * topmost view in post adapter when showing tag preview - displays tag name and follow button
 */
public class ReaderTagHeaderView extends RelativeLayout {

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
        mImageView = (WPNetworkImageView) view.findViewById(R.id.image_tag_header);
        mTxtAttribution = (TextView) view.findViewById(R.id.text_attribution);
    }

    public void setCurrentTag(final ReaderTag tag) {
        if (tag == null || ReaderTag.isSameTag(tag, mCurrentTag)) return;

        mCurrentTag = tag;

        TextView txtTagName = (TextView) findViewById(R.id.text_tag);
        txtTagName.setText(ReaderUtils.makeHashTag(tag.getTagSlug()));

        mTxtAttribution.setText(null);
        mImageView.resetImage();
        getImageAndAttribution();
    }

    private void getImageAndAttribution() {
        if (mCurrentTag == null) return;

        String tagNameForApi = ReaderUtils.sanitizeWithDashes(mCurrentTag.getTagSlug());
        String path = "read/tags/" + tagNameForApi + "/images?number=1";

        WordPress.getRestClientUtilsV1_2().get(path, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject == null) return;

                JSONArray jsonArray = jsonObject.optJSONArray("images");
                if (jsonArray == null) return;

                JSONObject jsonImage = jsonArray.optJSONObject(0);
                if (jsonImage == null) return;

                String imageUrl = JSONUtils.getString(jsonImage, "url");
                // current endpoint doesn't include the protocol
                if (!imageUrl.startsWith("http")) {
                    imageUrl = "https://" + imageUrl;
                }

                int imageWidth = mImageView.getWidth();
                int imageHeight = getContext().getResources().getDimensionPixelSize(R.dimen.reader_thumbnail_strip_image_height);
                String photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, imageWidth, imageHeight);
                mImageView.setImageUrl(photonUrl, WPNetworkImageView.ImageType.GONE_UNTIL_AVAILABLE);

                String author = JSONUtils.getString(jsonImage, "author");
                String blogTitle = JSONUtils.getString(jsonImage, "blog_title");
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
        }, null);
    }
}
