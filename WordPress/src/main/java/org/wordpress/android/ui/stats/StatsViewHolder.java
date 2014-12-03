package org.wordpress.android.ui.stats;

import android.text.Html;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

/**
 * View holder for stats_list_cell layout
 */
public class StatsViewHolder {
    public final TextView entryTextView;
    public final TextView totalsTextView;
    public final NetworkImageView networkImageView;
    public final ImageView chevronImageView;
    public final ImageView imgMore;

    public StatsViewHolder(View view) {
        entryTextView = (TextView) view.findViewById(R.id.stats_list_cell_entry);
        entryTextView.setMovementMethod(StatsWPLinkMovementMethod.getInstance());
        totalsTextView = (TextView) view.findViewById(R.id.stats_list_cell_total);
        chevronImageView = (ImageView) view.findViewById(R.id.stats_list_cell_chevron);

        networkImageView = (NetworkImageView) view.findViewById(R.id.stats_list_cell_image);
        networkImageView.setErrorImageResId(R.drawable.stats_blank_image);
        networkImageView.setDefaultImageResId(R.drawable.stats_blank_image);

        imgMore = (ImageView) view.findViewById(R.id.image_more);
    }

    /*
     * used by stats fragments to set the entry text, making it a clickable link if a url is passed
     */
    public void setEntryTextOrLink(String linkUrl, String linkName) {
        if (entryTextView == null) {
            return;
        }

        if (TextUtils.isEmpty(linkUrl)) {
            entryTextView.setText(linkName);
            if (linkName != null && linkName.startsWith("http")) {
                Linkify.addLinks(entryTextView, Linkify.WEB_URLS);
            }
        } else if (TextUtils.isEmpty(linkName)) {
            entryTextView.setText(linkUrl);
            if (linkUrl != null && linkUrl.startsWith("http")) {
                Linkify.addLinks(entryTextView, Linkify.WEB_URLS);
            }
        } else {
            entryTextView.setText(Html.fromHtml("<a href=\"" + linkUrl + "\">" + linkName + "</a>"));
        }

        StatsUIHelper.removeUnderlines((Spannable) entryTextView.getText());
    }

    /*
     * used by stats fragments to show a downloadable icon or default image
     */
    public void showNetworkImage(String imageUrl) {
        if (networkImageView == null) {
            return;
        }
        if (imageUrl != null && imageUrl.startsWith("http")) {
            networkImageView.setImageUrl(imageUrl, WordPress.imageLoader);
        } else {
            networkImageView.setImageResource(R.drawable.stats_blank_image);
        }
        networkImageView.setVisibility(View.VISIBLE);
    }
}
