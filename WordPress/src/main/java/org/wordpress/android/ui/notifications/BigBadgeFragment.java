package org.wordpress.android.ui.notifications;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.ui.stats.StatsWPLinkMovementMethod;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.JSONUtil;

public class BigBadgeFragment extends Fragment implements NotificationFragment {
    private Note mNote;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        View view = inflater.inflate(R.layout.notifications_big_badge, parent, false);
        NetworkImageView badgeImageView = (NetworkImageView) view.findViewById(R.id.badge);

        TextView bodyTextView = (TextView) view.findViewById(R.id.body);
        bodyTextView.setMovementMethod(StatsWPLinkMovementMethod.getInstance());

        if (getNote() != null) {
            String noteHTML = JSONUtil.queryJSON(getNote().toJSONObject(), "body.html", "");
            if (noteHTML.equals("")) {
                noteHTML = getNote().getSubject();
            }
            Spanned html = HtmlUtils.fromHtml(noteHTML);
            bodyTextView.setText(html);

            // Get the badge
            String iconURL = getNote().getIconURL();
            if (!iconURL.equals("")) {
                badgeImageView.setImageUrl(iconURL, WordPress.imageLoader);
            }

            // if this is a stats-related note, show stats link and enable tapping badge
            // to view stats - but only if the note is for a blog that's visible
            if (isStatsNote()) {
                final int remoteBlogId = getNote().getMetaValueAsInt("blog_id", -1);
                if (WordPress.wpDB.isDotComAccountVisible(remoteBlogId)) {
                    TextView txtStats = (TextView) view.findViewById(R.id.text_stats_link);
                    txtStats.setVisibility(View.VISIBLE);
                    View.OnClickListener statsListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showStatsActivity(remoteBlogId);
                        }
                    };
                    txtStats.setOnClickListener(statsListener);
                    badgeImageView.setOnClickListener(statsListener);
                }
            }
        }

        return view;
    }

    public void setNote(Note note) {
        mNote = note;
    }
    public Note getNote() {
        return mNote;
    }

    /*
     * returns true if this is a stats-related notification - currently handles these types:
     *   followed_milestone_achievement
     *   post_milestone_achievement
     *   like_milestone_achievement
     *   traffic_surge
     *   best_followed_day_feat
     *   best_liked_day_feat
     *   most_liked_day
     *   most_followed_day
     */
    boolean isStatsNote() {
        if (getNote() == null) {
            return false;
        }

        String type = getNote().getType();
        if (type == null) {
            return false;
        }

        return (type.contains("_milestone_")
             || type.startsWith("traffic_")
             || type.startsWith("best_")
             || type.startsWith("most_"));
    }

    /*
     * show stats for the passed blog
     */
    private void showStatsActivity(int remoteBlogId) {
        if (getActivity() == null || isRemoving()) {
            return;
        }

        int localBlogId = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogId(remoteBlogId);
        Intent intent = new Intent(getActivity(), StatsActivity.class);
        intent.putExtra(StatsActivity.ARG_NO_MENU_DRAWER, true);
        intent.putExtra(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, localBlogId);
        getActivity().startActivity(intent);
    }
}
