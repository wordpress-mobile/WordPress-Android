package org.wordpress.android.ui.stats;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
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
import org.wordpress.android.ui.stats.Stats.Category;

public class StatsCursorAdapter extends CursorAdapter {

    private Category mCategory;

    public StatsCursorAdapter(Context context, Cursor c, boolean autoRequery, Stats.Category category) {
        super(context, c, autoRequery);
        mCategory = category;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        
        String entryType = cursor.getString(cursor.getColumnIndex("entryType"));
        String entry = cursor.getString(cursor.getColumnIndex("entry"));
        int total = cursor.getInt(cursor.getColumnIndex("total"));
        String imageUrl = cursor.getString(cursor.getColumnIndex("imageUrl"));
        String url = cursor.getString(cursor.getColumnIndex("url"));

        // entries
        TextView entryTextView = (TextView) view.findViewById(R.id.stats_list_cell_entry);
        if (url != null && url.length() > 0) {
            Spanned link = Html.fromHtml("<a href=\"" + url + "\">" + entry + "</a>");
            entryTextView.setText(link);
            entryTextView.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            entryTextView.setText(entry);
        }
        
        // totals
        TextView totalsTextView = (TextView) view.findViewById(R.id.stats_list_cell_total);
        totalsTextView.setText(total + "");
        
        NetworkImageView imageView = (NetworkImageView) view.findViewById(R.id.stats_list_cell_image);
        imageView.setImageUrl(imageUrl, WordPress.imageLoader);
        
        switch (mCategory) {
            case POSTS_AND_PAGES:
            case VIDEO_PLAYS:
            case MOST_COMMENTED:
            case SEARCH_ENGINE_TERMS:
                imageView.setVisibility(View.GONE);
                break;
            default:
                imageView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup root) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.stats_list_cell, root, false);
    }

}
