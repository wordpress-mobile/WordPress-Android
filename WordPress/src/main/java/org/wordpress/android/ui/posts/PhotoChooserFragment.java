package org.wordpress.android.ui.posts;

import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;

public class PhotoChooserFragment extends Fragment {

    public interface OnPhotoChosenListener {
        public void onPhotoChosen(Uri imageUri);
    }

    private GridLayoutManager mLayoutManager;
    private RecyclerView mRecycler;

    public static PhotoChooserFragment newInstance() {
        Bundle args = new Bundle();
        PhotoChooserFragment fragment = new PhotoChooserFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.photo_chooser_fragment, container, false);

        mLayoutManager = new GridLayoutManager(getActivity(), 4);

        mRecycler = (RecyclerView) view.findViewById(R.id.recycler);
        mRecycler.setHasFixedSize(true);
        mRecycler.setLayoutManager(mLayoutManager);

        loadGallery();

        return view;
    }


    private OnPhotoChosenListener mListener = new OnPhotoChosenListener() {
        @Override
        public void onPhotoChosen(Uri imageUri) {
            if (getActivity() instanceof EditPostActivity) {
                EditPostActivity activity = (EditPostActivity) getActivity();
                activity.hidePhotoChooser();
                activity.addMedia(imageUri);
            }
        }
    };
    private void loadGallery() {
        PhotoChooserAdapter adapter = new PhotoChooserAdapter(getActivity(), mListener);
        mRecycler.setAdapter(adapter);
        adapter.loadGallery();
    }
}
