package com.shareba.medialist.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.shareba.medialist.R;


public class FullscreenImageActivity extends AppCompatActivity {
    public static final String ARG_MEDIA_URL = "fullscreen:image:media:url";
    private ImageView mPhotoView;
    private String mediaUrl;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_fullscreen_image);

        mPhotoView = (ImageView) findViewById(R.id.photo_view);
//        mPhotoView.setZoomable(false);
        supportPostponeEnterTransition();

        if (getIntent() == null) {
            finish();
        }

        mediaUrl = getIntent().getStringExtra(ARG_MEDIA_URL);
        if (TextUtils.isEmpty(mediaUrl)) {
            finish();
        }

        Glide.with(this)
                .load(mediaUrl)
                .fitCenter()
                .dontAnimate()
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        supportStartPostponedEnterTransition();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        supportStartPostponedEnterTransition();
                        return false;
                    }
                })
                .into(mPhotoView);


    }
}
