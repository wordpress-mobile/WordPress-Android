package org.wordpress.android.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.util.JSONUtil;

public class BigBadgeFragment extends Fragment implements NotificationFragment {
    private Note mNote;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state){
        View view = inflater.inflate(R.layout.notifications_big_badge, parent, false);
        NetworkImageView badgeImageView = (NetworkImageView) view.findViewById(R.id.badge);
        
        TextView bodyTextView = (TextView) view.findViewById(R.id.body);
        bodyTextView.setMovementMethod(LinkMovementMethod.getInstance());

        if (getNote() != null) {
            String noteHTML = JSONUtil.queryJSON(getNote().toJSONObject(), "body.html", "");
            if (noteHTML.equals(""))
                noteHTML = getNote().getSubject();
            Spanned html = Html.fromHtml(noteHTML);
            bodyTextView.setText(html);

            // Get the badge
            String iconURL = getNote().getIconURL();
            if (!iconURL.equals(""))
                badgeImageView.setImageUrl(iconURL, WordPress.imageLoader);

            // if this is a stats-related note, show stats link and enable tapping badge to view stats
            TextView txtStats = (TextView) view.findViewById(R.id.text_stats_link);
            if (isStatsNote()) {
                txtStats.setVisibility(View.VISIBLE);
                View.OnClickListener statsListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showStatsActivity();
                    }
                };
                txtStats.setOnClickListener(statsListener);
                badgeImageView.setOnClickListener(statsListener);
            } else {
                txtStats.setVisibility(View.GONE);
            }
        }
        
        return view;
    }

    public void setNote(Note note){
        mNote = note;
    }
    public Note getNote(){
        return mNote;
    }

    /*
     * returns true if this is a stats-related notification - currently handles these types:
     *
     *      followed_milestone_achievement
     *      post_milestone_achievement
     *      like_milestone_achievement
     *      traffic_surge
     *      best_followed_day_feat
     *      best_liked_day_feat
     *      most_liked_day
     *      most_followed_day
     *
     *  https://wpcom.trac.automattic.com/browser/trunk/wp-content/mu-plugins/notes/notes-rest-common.js#L64
     */
    public boolean isStatsNote() {
        if (getNote() == null)
            return false;

        String type = getNote().getType();
        if (type == null)
            return false;

        return (type.contains("_milestone_")
             || type.startsWith("traffic_")
             || type.startsWith("best_")
             || type.startsWith("most_"));
    }

    // TODO: this will show stats for the currently selected blog, but doesn't handle the case
    // where the note is for a different blog
    private void showStatsActivity() {
        if (getActivity() == null || isRemoving())
            return;
        Intent intent = new Intent(getActivity(), StatsActivity.class);
        intent.putExtra(StatsActivity.ARG_NO_MENU_DRAWER, true);
        getActivity().startActivity(intent);
    }
}