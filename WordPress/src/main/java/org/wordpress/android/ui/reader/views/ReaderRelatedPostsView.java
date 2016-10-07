package org.wordpress.android.ui.reader.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions.RelatedPostsType;
import org.wordpress.android.ui.reader.models.ReaderRelatedPost;
import org.wordpress.android.ui.reader.models.ReaderRelatedPostList;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * used by the detail view to display related posts, which can be either local (related posts
 * from the same site as the source posts) or global (related posts from across wp.com)
 */
public class ReaderRelatedPostsView extends LinearLayout {

    public ReaderRelatedPostsView(Context context) {
        super(context);
        initView(context);
    }

    public ReaderRelatedPostsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ReaderRelatedPostsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ReaderRelatedPostsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    private void initView(Context context) {
        inflate(context, R.layout.reader_related_posts_view, this);
    }

    public void showRelatedPosts(ReaderRelatedPostList relatedPosts,
                                 RelatedPostsType relatedPostsType,
                                 String siteName) {

        if (relatedPosts.size() == 0) {
            setVisibility(View.GONE);
            return;
        }

        ViewGroup container = (ViewGroup) findViewById(R.id.container_related_posts);
        container.removeAllViews();

        int avatarSize = DisplayUtils.dpToPx(getContext(), getResources().getDimensionPixelSize(R.dimen.avatar_sz_extra_small));
        boolean isGlobal = relatedPostsType == ReaderPostActions.RelatedPostsType.GLOBAL;

        // add a separate view for each related post
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int index = 0; index < relatedPosts.size(); index++) {
            final ReaderRelatedPost relatedPost = relatedPosts.get(index);

            View postView = inflater.inflate(R.layout.reader_related_post, container, false);
            TextView txtTitle = (TextView) postView.findViewById(R.id.text_related_post_title);
            TextView txtExcerpt = (TextView) postView.findViewById(R.id.text_related_post_excerpt);
            View siteHeader = postView.findViewById(R.id.layout_related_post_site_header);

            txtTitle.setText(relatedPost.getTitle());

            if (relatedPost.hasExcerpt()) {
                txtExcerpt.setText(relatedPost.getExcerpt());
                txtExcerpt.setVisibility(View.VISIBLE);
            } else {
                txtExcerpt.setVisibility(View.GONE);
            }

            // site header only appears for global posts
            if (isGlobal) {
                WPNetworkImageView imgAvatar = (WPNetworkImageView) siteHeader.findViewById(R.id.image_avatar);
                TextView txtSiteName = (TextView) siteHeader.findViewById(R.id.text_site_name);
                TextView txtAuthorName = (TextView) siteHeader.findViewById(R.id.text_author_name);
                txtSiteName.setText(relatedPost.getSiteName());
                txtAuthorName.setText(relatedPost.getAuthorName());
                if (relatedPost.hasAuthorAvatarUrl()) {
                    String avatarUrl = PhotonUtils.getPhotonImageUrl(relatedPost.getAuthorAvatarUrl(), avatarSize, avatarSize);
                    imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
                } else {
                    imgAvatar.showDefaultGravatarImage();
                }
                siteHeader.setVisibility(View.VISIBLE);
            } else {
                siteHeader.setVisibility(View.GONE);
            }

            // tapping this view should open the related post detail
            postView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO
                    //showRelatedPostDetail(relatedPost.getSiteId(), relatedPost.getPostId());
                }
            });

            container.addView(postView);
        }

        // make sure the label for these related posts is showing
        TextView label = (TextView) findViewById(R.id.text_related_posts_label);
        if (isGlobal) {
            label.setText(getContext().getString(R.string.reader_label_global_related_posts));
        } else {
            label.setText(String.format(getContext().getString(R.string.reader_label_local_related_posts), siteName));
        }
    }

}
