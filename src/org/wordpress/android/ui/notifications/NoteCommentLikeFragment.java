/**
 * Behaves much list a ListFragment
 */
package org.wordpress.android.ui.notifications;

import android.support.v4.app.ListFragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.models.Note;
import org.wordpress.android.util.JSONUtil;

public class NoteCommentLikeFragment extends ListFragment implements NotificationFragment {
    private Note mNote;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.notifications_follow_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle bundle){
        super.onActivityCreated(bundle);

        ListView list = getListView();
        list.setDivider(getResources().getDrawable(R.drawable.list_divider));
        list.setDividerHeight(1);
        list.setHeaderDividersEnabled(false);

        // No note? No service.
        if (getNote() == null)
            return;

        JSONArray bodyItems = getNote().queryJSON("body.items", new JSONArray());
        JSONObject bodyObject =  getNote().queryJSON("body", new JSONObject());

        // header subject will be the note subject ("These people like your comment"), header
        // snippet will be a snippet of the comment
        final String headerSubject = getHeaderText(bodyItems);
        final String headerSnippet = getCommentSnippet(bodyItems);
        final String headerLink = (bodyObject != null ? JSONUtil.getString(bodyObject, "header_link") : "");

        final DetailHeader noteHeader = (DetailHeader) getView().findViewById(R.id.header);

        // full header text is the subject + quoted snippet
        if (TextUtils.isEmpty(headerSnippet)) {
            noteHeader.setText(headerSubject);
        } else {
            noteHeader.setText(headerSubject + " \"" + headerSnippet + "\"");
        }

        noteHeader.setNote(getNote(), headerLink);

        if (getActivity() instanceof OnPostClickListener) {
            noteHeader.setOnPostClickListener(((OnPostClickListener)getActivity()));
        }
        if (getActivity() instanceof OnCommentClickListener) {
            noteHeader.setOnCommentClickListener(((OnCommentClickListener)getActivity()));
        }

        setListAdapter(new NoteFollowAdapter(getActivity(), getNote(), true));
    }

    @Override
    public void setNote(Note note){
        mNote = note;
    }

    @Override
    public Note getNote(){
        return mNote;
    }

    private String getHeaderText(JSONArray bodyItems) {
        if (bodyItems == null)
            return "";
        JSONObject noteItem = JSONUtil.queryJSON(bodyItems, String.format("[%d]", 0), new JSONObject());
        return JSONUtil.getStringDecoded(noteItem, "header_text");
    }

    private String getCommentSnippet(JSONArray bodyItems) {
        if (bodyItems == null)
            return "";
        JSONObject noteItem = JSONUtil.queryJSON(bodyItems, String.format("[%d]", 0), new JSONObject());
        return JSONUtil.getStringDecoded(noteItem, "html");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }
}