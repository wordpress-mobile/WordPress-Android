package org.wordpress.android.ui.notifications.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.notifications.blocks.NoteBlock;

import java.util.List;

public class NoteBlockAdapter extends ArrayAdapter<NoteBlock> {

    private final LayoutInflater mLayoutInflater;
    private final int mBackgroundColor;

    private List<NoteBlock> mNoteBlockList;

    public NoteBlockAdapter(Context context, List<NoteBlock> noteBlocks, int backgroundColor) {
        super(context, 0, noteBlocks);

        mNoteBlockList = noteBlocks;
        mLayoutInflater = LayoutInflater.from(context);
        mBackgroundColor = backgroundColor;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        return mNoteBlockList == null ? 0 : mNoteBlockList.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        NoteBlock noteBlock = mNoteBlockList.get(position);

        // Check the tag for this recycled view, if it matches we can reuse it
        if (convertView == null || noteBlock.getBlockType() != convertView.getTag(R.id.note_block_tag_id)) {
            convertView = mLayoutInflater.inflate(noteBlock.getLayoutResourceId(), parent, false);
            convertView.setTag(noteBlock.getViewHolder(convertView));
        }

        // Update the block type for this view
        convertView.setTag(R.id.note_block_tag_id, noteBlock.getBlockType());

        noteBlock.setBackgroundColor(mBackgroundColor);

        return noteBlock.configureView(convertView);
    }

    public void setNoteList(List<NoteBlock> noteList) {
        if (noteList == null) return;

        mNoteBlockList = noteList;
        notifyDataSetChanged();
    }
}
