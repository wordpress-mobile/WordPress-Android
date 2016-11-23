package org.wordpress.android.ui.stats;

import android.content.Context;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.apache.commons.lang.StringEscapeUtils;
import org.wordpress.android.R;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.stats.models.PostModel;
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
    public final ImageView linkImageView;
    public final ImageView imgMore;
    public final LinearLayout rowContent;

    public StatsViewHolder(View view) {
        rowContent = (LinearLayout) view.findViewById(R.id.layout_content);
        entryTextView = (TextView) view.findViewById(R.id.stats_list_cell_entry);
        totalsTextView = (TextView) view.findViewById(R.id.stats_list_cell_total);
        chevronImageView = (ImageView) view.findViewById(R.id.stats_list_cell_chevron);
        linkImageView = (ImageView) view.findViewById(R.id.stats_list_cell_link);
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
                            String statsAuthenticatedUser = AccountHelper.getDefaultAccount().getUserName();
                            if (org.apache.commons.lang.StringUtils.isEmpty(statsAuthenticatedUser)) {
                                // Still empty. Do not eat the event, but let's open the default Web Browser.

                            }
                            WPWebViewActivity.openUrlByUsingWPCOMCredentials(view.getContext(),
                                    url, statsAuthenticatedUser);

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
     * Used by stats fragments to set the entry text, opening the stats details page.
     */
    public void setEntryTextOpenDetailsPage(final PostModel currentItem) {
        if (entryTextView == null) {
            return;
        }

        String name =  StringEscapeUtils.unescapeHtml(currentItem.getTitle());
        entryTextView.setText(name);
        rowContent.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ActivityLauncher.viewStatsSinglePostDetails(view.getContext(), currentItem);
                    }
                });
        entryTextView.setTextColor(entryTextView.getContext().getResources().getColor(R.color.stats_link_text_color));
    }

    /*
     * Used by stats fragments to create the more btn context menu with the "View" option in it.
     * Opening it with reader if possible.
     *
     */
    public void setMoreButtonOpenInReader(final PostModel currentItem) {
        if (imgMore == null) {
            return;
        }

        imgMore.setVisibility(View.VISIBLE);
        imgMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Context ctx = view.getContext();
                PopupMenu popup = new PopupMenu(ctx, view);
                MenuItem menuItem = popup.getMenu().add(ctx.getString(R.string.stats_view));
                menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        StatsUtils.openPostInReaderOrInAppWebview(ctx, currentItem);
                        return true;
                    }
                });
                popup.show();
            }
        });
    }


    public void showChevronIcon() {
        linkImageView.setVisibility(View.GONE);
        chevronImageView.setVisibility(View.VISIBLE);
    }

    public void showLinkIcon() {
        linkImageView.setVisibility(View.VISIBLE);
        chevronImageView.setVisibility(View.GONE);
    }
}
