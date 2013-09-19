/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wordpress.android.widgets.StaggeredGridView;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.WrapperListAdapter;

public class HeaderFooterListAdapter implements WrapperListAdapter {
    private ListAdapter mAdapter;
    View mHeaderView;
    View mFooterView;
    boolean mAreAllFixedViewsSelectable;
    private int headerSize = -1;
    private int footerSize = -1;

    public static int TYPE_COUNT_WITHOUT_REFRESHABLE_LIST = 2;
    public static int HEADER_TYPE = 0;
    public static int FOOTER_TYPE = HEADER_TYPE + 1;
    public static int REFRESHABLE_LIST_TYPE = FOOTER_TYPE + 1;

    public HeaderFooterListAdapter(View headerView, View footerView, ListAdapter adapter) {
        mAdapter = adapter;

        if (headerView == null) {
            headerSize = 0;
        } else {
            headerSize = 1;
            mHeaderView = headerView;
        }

        if (footerView == null) {
            footerSize = 0;
        } else {
            footerSize = 1;
            mFooterView = footerView;
        }

        mAreAllFixedViewsSelectable = true;
    }

    @Override
    public ListAdapter getWrappedAdapter() {
        return mAdapter;
    }

    @Override
    public boolean areAllItemsEnabled() {
        if (mAdapter != null) {
            return mAreAllFixedViewsSelectable && mAdapter.areAllItemsEnabled();
        } else {
            return true;
        }
    }

    @Override
    public boolean isEnabled(int position) {
        if (position < headerSize) {
            return true;
        }

        int adjPosition = position - headerSize;
        if (mAdapter != null) {
            if (adjPosition < mAdapter.getCount()) {
                return mAdapter.isEnabled(adjPosition);
            }
        }

        return false;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        if (mAdapter != null) {
            mAdapter.registerDataSetObserver(observer);
        }
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(observer);
        }
    }

    @Override
    public int getCount() {
        if (mAdapter != null) {
            return headerSize + footerSize + mAdapter.getCount();
        } else {
            return headerSize + footerSize;
        }
    }

    @Override
    public Object getItem(int position) {
        if (position < headerSize) {
            return mHeaderView;
        }

        final int adjPosition = position - headerSize;
        int adapterCount = 0;
        if (mAdapter != null) {
            adapterCount = mAdapter.getCount();
            if (adjPosition < adapterCount) {
                return mAdapter.getItem(adjPosition);
            }
        }

        return mFooterView;
    }

    @Override
    public long getItemId(int position) {
        if (mAdapter != null && position >= headerSize) {
            int adjPosition = position - headerSize;
            int adapterCount = mAdapter.getCount();
            if (adjPosition < adapterCount) {
                return mAdapter.getItemId(adjPosition);
            }
        }
        return -1;
    }

    @Override
    public boolean hasStableIds() {
        if (mAdapter != null) {
            return mAdapter.hasStableIds();
        }
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position < headerSize) {
            return mHeaderView;
        }

        final int adjPosition = position - headerSize;
        int adapterCount = 0;
        if (mAdapter != null) {
            adapterCount = mAdapter.getCount();
            if (adjPosition < adapterCount) {
                return mAdapter.getView(adjPosition, convertView, parent);
            }
        }

        return mFooterView;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < headerSize) {
            return HEADER_TYPE;
        }

        if (mAdapter != null && position >= headerSize) {
            int adjPosition = position - headerSize;
            int adapterCount = mAdapter.getCount();
            if (adjPosition < adapterCount) {
//                return mAdapter.getItemViewType(adjPosition);
                return REFRESHABLE_LIST_TYPE;
            }
        }

        return FOOTER_TYPE;
    }

    @Override
    public int getViewTypeCount() {
        if (mAdapter != null) {
            return mAdapter.getViewTypeCount() + TYPE_COUNT_WITHOUT_REFRESHABLE_LIST;
        }
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return mAdapter == null || mAdapter.isEmpty();
    }
}
