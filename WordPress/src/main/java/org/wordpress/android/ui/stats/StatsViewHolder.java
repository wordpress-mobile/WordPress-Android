package org.wordpress.android.ui.stats;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.stats.models.SingleItemModel;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.UrlUtils;
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
    public final LinearLayout rowContent;

    public StatsViewHolder(View view) {
        rowContent = (LinearLayout) view.findViewById(R.id.layout_content);
        entryTextView = (TextView) view.findViewById(R.id.stats_list_cell_entry);
        totalsTextView = (TextView) view.findViewById(R.id.stats_list_cell_total);
        chevronImageView = (ImageView) view.findViewById(R.id.stats_list_cell_chevron);

        networkImageView = (WPNetworkImageView) view.findViewById(R.id.stats_list_cell_image);

        imgMore = (ImageView) view.findViewById(R.id.image_more);
    }

    /*
     * used by stats fragments to set the entry text, making it a clickable link if a url is passed
     */
    public void setEntryTextOrLink(final String linkURL, String linkName) {
        if (entryTextView == null) {
            return;
        }

        entryTextView.setText(linkName);
        if (TextUtils.isEmpty(linkURL)) {
            entryTextView.setTextColor(entryTextView.getContext().getResources().getColor(R.color.stats_text_color));
            rowContent.setClickable(false);
            return;
        }

        rowContent.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String url = linkURL;
                        AppLog.d(AppLog.T.UTILS, "Tapped on the Link: " + url);
                        if (url.startsWith("https://wordpress.com/my-stats")
                                || url.startsWith("http://wordpress.com/my-stats")) {
                            // make sure to load the no-chrome version of Stats over https
                            url = UrlUtils.makeHttps(url);
                            if (url.contains("?")) {
                                // add the no chrome parameters if not available
                                if (!url.contains("?no-chrome") && !url.contains("&no-chrome")) {
                                    url += "&no-chrome";
                                }
                            } else {
                                url += "?no-chrome";
                            }
                            AppLog.d(AppLog.T.UTILS, "Opening the Authenticated in-app browser : " + url);
                            // Let's try the global wpcom credentials
                            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(view.getContext());
                            String statsAuthenticatedUser = settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
                            String statsAuthenticatedPassword = WordPressDB.decryptPassword(
                                    settings.getString(WordPress.WPCOM_PASSWORD_PREFERENCE, null)
                            );
                            if (org.apache.commons.lang.StringUtils.isEmpty(statsAuthenticatedPassword)
                                    || org.apache.commons.lang.StringUtils.isEmpty(statsAuthenticatedUser)) {
                                // Still empty. Do not eat the event, but let's open the default Web Browser.

                            }
                            WPWebViewActivity.openUrlByUsingWPCOMCredentials(view.getContext(),
                                    url, statsAuthenticatedUser, statsAuthenticatedPassword);

                        } else if (url.startsWith("https") || url.startsWith("http")) {
                            AppLog.d(AppLog.T.UTILS, "Opening the in-app browser: " + url);
                            WPWebViewActivity.openURL(view.getContext(), url);
                        }

                    }
                }
        );

        entryTextView.setTextColor(entryTextView.getContext().getResources().getColor(R.color.stats_link_text_color));
    }

    public void setEntryText(String text) {
        entryTextView.setText(text);
        rowContent.setClickable(false);
    }

    public void setEntryText(String text, int color) {
        entryTextView.setTextColor(color);
        setEntryText(text);
    }

    /*
     * used by stats fragments to set the entry text, opening it with reader if possible
     */
    public void setEntryTextOpenInReader(SingleItemModel currentItem) {
        if (entryTextView == null) {
            return;
        }

        String name = currentItem.getTitle();
        final String url = currentItem.getUrl();
        final long blogID = Long.parseLong(currentItem.getBlogID());
        final long itemID = Long.parseLong(currentItem.getItemID());
        entryTextView.setText(name);
        rowContent.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // If the post/page has ID == 0 is the home page, and we need to load the blog preview,
                        // otherwise 404 is returned if we try to show the post in the reader
                        if (itemID == 0) {
                            ReaderActivityLauncher.showReaderBlogPreview(
                                    view.getContext(),
                                    blogID,
                                    url
                            );
                        } else {
                            ReaderActivityLauncher.showReaderPostDetail(
                                    view.getContext(),
                                    blogID,
                                    itemID
                            );
                        }
                    }
                });
        entryTextView.setTextColor(entryTextView.getContext().getResources().getColor(R.color.stats_link_text_color));
    }
}
