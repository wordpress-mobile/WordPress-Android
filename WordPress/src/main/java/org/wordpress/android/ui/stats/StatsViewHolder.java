package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.text.Html;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.stats.models.SingleItemModel;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * View holder for stats_list_cell layout
 */
public class StatsViewHolder {
    public final TextView entryTextView;
    public final TextView totalsTextView;
    public final WPNetworkImageView networkImageView;
    public final ImageView chevronImageView;
    public final ImageView imgMore;

    public StatsViewHolder(View view) {
        entryTextView = (TextView) view.findViewById(R.id.stats_list_cell_entry);
        entryTextView.setMovementMethod(StatsWPLinkMovementMethod.getInstance());
        totalsTextView = (TextView) view.findViewById(R.id.stats_list_cell_total);
        chevronImageView = (ImageView) view.findViewById(R.id.stats_list_cell_chevron);

        networkImageView = (WPNetworkImageView) view.findViewById(R.id.stats_list_cell_image);

        imgMore = (ImageView) view.findViewById(R.id.image_more);
    }

    /*
     * used by stats fragments to set the entry text, making it a clickable link if a url is passed
     */
    public void setEntryTextOrLink(String linkUrl, String linkName) {
        if (entryTextView == null) {
            return;
        }
        boolean isLink = false;
        if (TextUtils.isEmpty(linkUrl)) {
            entryTextView.setText(linkName);
            isLink = (linkName != null && linkName.startsWith("http"));
        } else if (TextUtils.isEmpty(linkName)) {
            entryTextView.setText(linkUrl);
            isLink = (linkUrl != null && linkUrl.startsWith("http"));
        } else {
            entryTextView.setText(Html.fromHtml("<a href=\"" + linkUrl + "\">" + linkName + "</a>"));
        }

        if (isLink) {
            entryTextView.setMovementMethod(StatsWPLinkMovementMethod.getInstance());
            Linkify.addLinks(entryTextView, Linkify.WEB_URLS);
        }

        // Remove the highlight color. It's already specified in the XML, but Linkify and friends re-add it at run-time.
        entryTextView.setHighlightColor(entryTextView.getResources().getColor(R.color.transparent));
        StatsUIHelper.removeUnderlines((Spannable) entryTextView.getText());
    }


    /*
     * used by stats fragments to set the entry text, opening it with reader if possible
     */
    public void setEntryTextOpenInreader(final Activity ctx, SingleItemModel currentItem) {
        if (entryTextView == null) {
            return;
        }

        String name = currentItem.getTitle();
        final String url = currentItem.getUrl();
        final long blogID = Long.parseLong(currentItem.getBlogID());
        final long itemID = Long.parseLong(currentItem.getItemID());
        entryTextView.setText(name);
        entryTextView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // If the post/page has ID == 0 is the home page, and we need to load the blog preview,
                        // otherwise 404 is returned if we try to show the post in the reader
                        if (itemID == 0) {
                            ReaderActivityLauncher.showReaderBlogPreview(
                                    ctx,
                                    blogID,
                                    url
                            );
                        } else {
                            ReaderActivityLauncher.showReaderPostDetail(
                                    ctx,
                                    blogID,
                                    itemID
                            );
                        }
                    }
                });
        entryTextView.setTextColor(ctx.getResources().getColor(R.color.wordpress_blue));
    }


    /*
     * used by stats fragments to show a downloadable icon or default image

    public void showNetworkImage(String imageUrl) {
        if (networkImageView == null) {
            return;
        }
        if (imageUrl != null && imageUrl.startsWith("http")) {
            networkImageView.setImageUrl(imageUrl, WPNetworkImageView.ImageType.SITE_AVATAR);
        } else {
            networkImageView.setImageResource(R.drawable.stats_blank_image);
        }
        networkImageView.setVisibility(View.VISIBLE);
    } */
}
