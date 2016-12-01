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
        if (tag == null) return;

        mCurrentTag = tag;

        TextView txtTagName = (TextView) findViewById(R.id.text_tag);
        txtTagName.setText(ReaderUtils.makeHashTag(tag.getTagSlug()));

        mTxtAttribution.setText(null);
        mImageView.setImageDrawable(null);
        getImageAndAttribution();
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
