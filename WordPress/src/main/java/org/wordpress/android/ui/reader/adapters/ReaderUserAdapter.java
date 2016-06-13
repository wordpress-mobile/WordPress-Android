package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderUser;
import org.wordpress.android.models.ReaderUserList;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderInterfaces.DataLoadedListener;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * owner must call setUsers() with the list of
 * users to display
 */
public class ReaderUserAdapter  extends RecyclerView.Adapter<ReaderUserAdapter.UserViewHolder> {
    private final ReaderUserList mUsers = new ReaderUserList();
    private DataLoadedListener mDataLoadedListener;
    private final int mAvatarSz;

    public ReaderUserAdapter(Context context) {
        super();
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_small);
        setHasStableIds(true);
    }

    public void setDataLoadedListener(DataLoadedListener listener) {
        mDataLoadedListener = listener;
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    private boolean isEmpty() {
        return (getItemCount() == 0);
    }

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.reader_listitem_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UserViewHolder holder, int position) {
        final ReaderUser user = mUsers.get(position);

        holder.txtName.setText(user.getDisplayName());
        if (user.hasUrl()) {
            holder.txtUrl.setVisibility(View.VISIBLE);
            holder.txtUrl.setText(user.getUrlDomain());
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (user.hasBlogId()) {
                        ReaderActivityLauncher.showReaderBlogPreview(
                                v.getContext(),
                                user.blogId);
                    }
                }
            });
        } else {
            holder.txtUrl.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
        }

        if (user.hasAvatarUrl()) {
            holder.imgAvatar.setImageUrl(
                    GravatarUtils.fixGravatarUrl(user.getAvatarUrl(), mAvatarSz),
                    WPNetworkImageView.ImageType.AVATAR);
        } else {
            holder.imgAvatar.showDefaultGravatarImage();
        }
    }

    @Override
    public long getItemId(int position) {
        return mUsers.get(position).userId;
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtName;
        private final TextView txtUrl;
        private final WPNetworkImageView imgAvatar;

        public UserViewHolder(View view) {
            super(view);
            txtName = (TextView) view.findViewById(R.id.text_name);
            txtUrl = (TextView) view.findViewById(R.id.text_url);
            imgAvatar = (WPNetworkImageView) view.findViewById(R.id.image_avatar);
        }
    }

    public void setUsers(final ReaderUserList users) {
        mUsers.clear();
        if (users != null && users.size() > 0) {
            mUsers.addAll(users);
        }
        notifyDataSetChanged();
        if (mDataLoadedListener != null) {
            mDataLoadedListener.onDataLoaded(isEmpty());
        }
    }
}
