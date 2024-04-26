package org.wordpress.android.ui.notifications.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import org.wordpress.android.R
import org.wordpress.android.ui.notifications.blocks.NoteBlock

class NoteBlockAdapter(context: Context, private var noteBlocks: List<NoteBlock>) :
    ArrayAdapter<NoteBlock>(context, 0, noteBlocks) {
    override fun hasStableIds(): Boolean = true

    override fun getCount(): Int = noteBlocks.size

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val noteBlock = noteBlocks[position]

        // Check the tag for this recycled view, if it matches we can reuse it
        return if (convertView == null || noteBlock.blockType != convertView.getTag(R.id.note_block_tag_id)) {
            val view = LayoutInflater.from(parent.context).inflate(noteBlock.layoutResourceId, parent, false)
            view.tag = noteBlock.getViewHolder(view)
            view.setTag(R.id.note_block_tag_id, noteBlock.blockType)
            noteBlock.configureView(view)
        } else {
            // Update the block type for this view
            convertView.setTag(R.id.note_block_tag_id, noteBlock.blockType)
            noteBlock.configureView(convertView)
        }
    }

    fun setNoteList(noteList: List<NoteBlock>?) {
        if (noteList == null) {
            return
        }
        noteBlocks = noteList
        notifyDataSetChanged()
    }
}
