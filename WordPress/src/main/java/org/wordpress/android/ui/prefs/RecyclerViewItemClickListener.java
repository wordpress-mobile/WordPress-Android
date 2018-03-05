package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class RecyclerViewItemClickListener implements RecyclerView.OnItemTouchListener {
    private final GestureDetector mGestureDetector;
    private final OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
        void onLongItemClick(View view, int position);
    }

    public RecyclerViewItemClickListener(Context context, final RecyclerView recyclerView,
                                         OnItemClickListener listener) {
        mListener = listener;

        mGestureDetector = new GestureDetector(
                context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapUp(MotionEvent motionEvent) {
                        return true;
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        View child = recyclerView.findChildViewUnder(e.getX(), e.getY());

                        if (child != null && mListener != null) {
                            mListener.onLongItemClick(child, recyclerView.getChildAdapterPosition(child));
                        }
                    }
                }
        );
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent motionEvent) {
        View childView = view.findChildViewUnder(motionEvent.getX(), motionEvent.getY());

        if (childView != null && mListener != null && mGestureDetector.onTouchEvent(motionEvent)) {
            mListener.onItemClick(childView, view.getChildAdapterPosition(childView));
            return true;
        }

        return false;
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    @Override
    public void onTouchEvent(RecyclerView view, MotionEvent motionEvent) {
    }
}
