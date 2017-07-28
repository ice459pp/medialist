package com.shareba.medialist.ui;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.shareba.medialist.R;

import java.io.File;

public class FullscreenImageFragment extends Fragment {
    private final static String TAG = FullscreenImageFragment.class.getSimpleName();
    private static final String ARG_MEDIA_URL = "fullscreen:image:media:url";
    private String mediaUrl;
    private ImageView mPhotoView;

    public static FullscreenImageFragment newInstance(String mediaUrl) {
        FullscreenImageFragment fragment = new FullscreenImageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MEDIA_URL, mediaUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        postponeEnterTransition();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setSharedElementEnterTransition(TransitionInflater.from(getContext()).inflateTransition(android.R.transition.fade));
            setEnterTransition(TransitionInflater.from(getContext()).inflateTransition(android.R.transition.fade));
        }

        if (getArguments() != null) {
            mediaUrl = getArguments().getString(ARG_MEDIA_URL);
        }

        if (TextUtils.isEmpty(mediaUrl)) {
            Log.e(TAG, "mediaUrl is null or empty");
            return;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_media_fullscreen_image, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mPhotoView = (ImageView) view.findViewById(R.id.photo_view);
        File file = new File(mediaUrl);
        if (file.exists()) {
            Uri uri = Uri.fromFile(file);
            Glide.with(this)
                    .load(uri)
                    .fitCenter()
                    .listener(new RequestListener<Uri, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, Uri model, Target<GlideDrawable> target, boolean isFirstResource) {
                            startPostponedEnterTransition();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, Uri model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            startPostponedEnterTransition();
                            return false;
                        }
                    }).into(mPhotoView);
            // get the bitmap width and height using traditional way.
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inJustDecodeBounds = true;
//            BitmapFactory.decodeFile(new File(uri.getPath()).getAbsolutePath(), options);
//            int imageWidth = options.outWidth;
//            int imageHeight = options.outHeight;


        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // set fullscreen
        Window window = getActivity().getWindow();
        if (window != null) {
            View decorView = window.getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mediaUrl = null;
    }
}
