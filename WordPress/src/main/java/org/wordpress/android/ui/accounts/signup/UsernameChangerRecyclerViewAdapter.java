package org.wordpress.android.ui.accounts.signup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.R;

import java.util.List;

public class UsernameChangerRecyclerViewAdapter
        extends RecyclerView.Adapter<UsernameChangerRecyclerViewAdapter.ViewHolder> {
    private Context mContext;

    protected List<String> mItems;
    protected OnUsernameSelectedListener mListener;
    protected int mSelectedItem = -1;

    public UsernameChangerRecyclerViewAdapter(Context context, List<String> items) {
        mContext = context;
        mItems = items;
    }

    @Override
    public void onBindViewHolder(UsernameChangerRecyclerViewAdapter.ViewHolder viewHolder, int position) {
        viewHolder.mRadio.setChecked(position == mSelectedItem);
        viewHolder.mText.setText(mItems.get(position));
    }

    @Override
    public UsernameChangerRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        final View view =
                LayoutInflater.from(mContext).inflate(R.layout.single_choice_recycler_view_item, viewGroup, false);
        return new UsernameChangerRecyclerViewAdapter.ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public int getSelectedItem() {
        return mSelectedItem;
    }

    public void setOnUsernameSelectedListener(OnUsernameSelectedListener listener) {
        mListener = listener;
    }

    public void setSelectedItem(int position) {
        mSelectedItem = position;
        notifyItemRangeChanged(0, mItems.size());
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public RadioButton mRadio;
        public TextView mText;

        public ViewHolder(final View inflate) {
            super(inflate);
            mRadio = inflate.findViewById(R.id.radio);
            mText = inflate.findViewById(R.id.text);

            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mSelectedItem = getBindingAdapterPosition();
                    notifyItemRangeChanged(0, mItems.size());

                    if (mListener != null) {
                        mListener.onUsernameSelected(mItems.get(mSelectedItem));
                    }
                }
            };

            itemView.setOnClickListener(listener);
            mRadio.setOnClickListener(listener);
        }
    }

    interface OnUsernameSelectedListener {
        void onUsernameSelected(String username);
    }
}
