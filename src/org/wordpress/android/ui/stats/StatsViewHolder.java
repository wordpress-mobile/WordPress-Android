package org.wordpress.android.ui.stats;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPLinkMovementMethod;

/**
 * View holder for stats_list_cell layout
 */
class StatsViewHolder {
    public final TextView entryTextView;
    public final TextView totalsTextView;
    public final NetworkImageView networkImageView;
    public final ImageView chevronImageView;

    public StatsViewHolder(View view) {
        entryTextView = (TextView) view.findViewById(R.id.stats_list_cell_entry);
        totalsTextView = (TextView) view.findViewById(R.id.stats_list_cell_total);
        chevronImageView = (ImageView) view.findViewById(R.id.stats_list_cell_chevron);

        networkImageView = (NetworkImageView) view.findViewById(R.id.stats_list_cell_image);
        networkImageView.setErrorImageResId(R.drawable.stats_blank_image);
        networkImageView.setDefaultImageResId(R.drawable.stats_blank_image);

        entryTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    TextView currentTextView = (TextView) v;
                    URLSpan[] urls = currentTextView.getUrls();
                    // note that there will be only one URLSpan (the one that was tapped)
                    if (urls.length > 0) {
                        String url = StringUtils.notNullStr(urls[0].getURL());
                        if (Uri.parse(url).getScheme() == null) {
                            url = "http://" + url.trim();
                        }

                        url = url.trim();
                        try {
                            Intent statsWebViewIntent = new Intent(v.getContext(), StatsWebViewActivity.class);
                            statsWebViewIntent.putExtra(StatsWebViewActivity.STATS_URL, url);
                            v.getContext().startActivity(statsWebViewIntent);
                        } catch (ActivityNotFoundException e) {
                            AppLog.e(AppLog.T.STATS, e);
                            ToastUtils.showToast(v.getContext(),
                                    v.getContext().getString(R.string.reader_toast_err_url_intent, url),
                                    ToastUtils.Duration.LONG);
                        }
                        return true;
                    }
                }
                return false;
            }
        });
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
            entryTextView.setMovementMethod(WPLinkMovementMethod.getInstance());
        }
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
    }
}
