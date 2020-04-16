package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.TermModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TagsRecyclerViewAdapter extends RecyclerView.Adapter<TagsRecyclerViewAdapter.TagViewHolder> {
    private List<TermModel> mAllTags;
    private List<TermModel> mFilteredTags;
    private Context mContext;
    private TagSelectedListener mTagSelectedListener;

    TagsRecyclerViewAdapter(Context context, TagSelectedListener tagSelectedListener) {
        mContext = context;
        mTagSelectedListener = tagSelectedListener;
        mFilteredTags = new ArrayList<>();
    }

    @Override
    public TagViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.tags_list_row, parent, false);
        return new TagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final TagViewHolder holder, int position) {
        // Guard against mFilteredTags getting altered in another thread
        if (mFilteredTags.size() <= position) {
            return;
        }
        String tag = StringEscapeUtils.unescapeHtml4(mFilteredTags.get(position).getName());
        holder.mNameTextView.setText(tag);
    }

    @Override
    public int getItemCount() {
        return mFilteredTags.size();
    }

    void setAllTags(List<TermModel> allTags) {
        mAllTags = allTags;
    }

    public void filter(final String text) {
        final List<TermModel> allTags = mAllTags;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<TermModel> filteredTags = new ArrayList<>();
                if (TextUtils.isEmpty(text)) {
                    filteredTags.addAll(allTags);
                } else {
                    for (TermModel tag : allTags) {
                        if (tag.getName().toLowerCase(Locale.getDefault())
                               .contains(text.toLowerCase(Locale.getDefault()))) {
                            filteredTags.add(tag);
                        }
                    }
                }

                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mFilteredTags = filteredTags;
                        notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

    class TagViewHolder extends RecyclerView.ViewHolder {
        private final TextView mNameTextView;

        TagViewHolder(View view) {
            super(view);
            mNameTextView = (TextView) view.findViewById(R.id.tag_name);
            RelativeLayout layout = (RelativeLayout) view.findViewById(R.id.tags_list_row_container);
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mTagSelectedListener.onTagSelected(mNameTextView.getText().toString());
                }
            });
        }
    }
}
