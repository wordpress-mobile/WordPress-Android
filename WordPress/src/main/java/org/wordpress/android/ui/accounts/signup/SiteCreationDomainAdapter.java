package org.wordpress.android.ui.accounts.signup;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse;
import org.wordpress.android.util.helpers.Debouncer;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class SiteCreationDomainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_INPUT = 1;
    private static final int VIEW_TYPE_ITEM = 2;

    public interface OnAdapterListener {
        void onKeywordsChange(String keywords);
        void onSelectionChange(String domain);
    }

    private boolean mIsLoading;
    private String mKeywords;
    private List<DomainSuggestionResponse> mSuggestions;
    private OnAdapterListener mOnAdapterListener;

    private DomainSuggestionResponse mSelectedDomain;

    private Debouncer mDebouncer = new Debouncer();

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public final View progress;
        public final TextView label;

        HeaderViewHolder(View itemView) {
            super(itemView);
            this.progress = itemView.findViewById(R.id.progress_container);
            this.label = (TextView) itemView.findViewById(R.id.progress_label);
        }
    }

    public static class InputViewHolder extends RecyclerView.ViewHolder {
        public final EditText input;
        public final View progressBar;

        public InputViewHolder(View itemView) {
            super(itemView);
            this.input = itemView.findViewById(R.id.input);
            this.progressBar = itemView.findViewById(R.id.progress_bar);
        }
    }

    public static class DomainViewHolder extends RecyclerView.ViewHolder {
        public final RadioButton radioButton;
        public final TextView textView;

        public DomainViewHolder(View itemView) {
            super(itemView);
            radioButton = (RadioButton) itemView.findViewById(R.id.radio_button);
            textView = (TextView) itemView.findViewById(R.id.text);
        }
    }

    public SiteCreationDomainAdapter(Context context, OnAdapterListener onAdapterListener) {
        super();
        ((WordPress) context.getApplicationContext()).component().inject(this);

        // Stable IDs so the edittext doesn't lose focus on refresh
        setHasStableIds(true);

        mOnAdapterListener = onAdapterListener;
    }

    public void setData(boolean isLoading, String keywords, List<DomainSuggestionResponse> suggestions) {
        if (isLoading != mIsLoading) {
            notifyItemChanged(1);
        }

        mIsLoading = isLoading;
        mKeywords = keywords;
        mSuggestions = suggestions;

        if (mSuggestions != null) {
            mSelectedDomain = mSuggestions.get(0);
            mOnAdapterListener.onSelectionChange(mSelectedDomain.domain_name);
        }

        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.site_creation_domain_header,
                    parent, false);
            return new HeaderViewHolder(itemView);
        } else if (viewType == VIEW_TYPE_INPUT) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.site_creation_domain_input, parent,
                    false);
            return new InputViewHolder(itemView);
        } else {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.site_creation_domain_item, parent,
                    false);
            return new DomainViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);

        if (viewType == VIEW_TYPE_HEADER) {
//            final HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
        } else if (viewType == VIEW_TYPE_INPUT) {
            final InputViewHolder inputViewHolder = (InputViewHolder) holder;
            inputViewHolder.progressBar.setVisibility(mIsLoading ? View.VISIBLE : View.GONE);
            if (inputViewHolder.input.getTag() == null) {
                inputViewHolder.input.setText(mKeywords);
                inputViewHolder.input.setTag(Boolean.TRUE);
                inputViewHolder.input.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void afterTextChanged(final Editable editable) {
                        mDebouncer.debounce(Void.class, new Runnable() {
                            @Override
                            public void run() {
                                mOnAdapterListener.onKeywordsChange(editable.toString());
                            }
                        }, 400, TimeUnit.MILLISECONDS);
                    }
                });
            }
        } else {
            final DomainSuggestionResponse suggestion = getItem(position);
            final DomainViewHolder domainViewHolder = (DomainViewHolder) holder;
            domainViewHolder.radioButton.setChecked(suggestion.equals(mSelectedDomain));
            domainViewHolder.textView.setText(suggestion.domain_name);

            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!suggestion.equals(mSelectedDomain)) {
                        mSelectedDomain = suggestion;
                        notifyDataSetChanged();
                        mOnAdapterListener.onSelectionChange(mSelectedDomain.domain_name);
                    }
                }
            };

            holder.itemView.setOnClickListener(clickListener);
            domainViewHolder.radioButton.setOnClickListener(clickListener);
        }
    }

    @Override
    public int getItemCount() {
        return 2 + (mSuggestions == null ? 0 : mSuggestions.size());
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_HEADER;
        } else if (position == 1) {
            return VIEW_TYPE_INPUT;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    @Override
    public long getItemId(int position) {
        return position; // just return the position itself. Items are all unique anyway.
    }

    private DomainSuggestionResponse getItem(int position) {
        return mSuggestions.get(position - 2);
    }
}
