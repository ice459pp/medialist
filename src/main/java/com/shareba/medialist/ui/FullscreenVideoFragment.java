package com.shareba.medialist.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.shareba.exoplayer2.ExoPlayerHelper;
import com.shareba.exoplayer2.ExoPlayerView;
import com.shareba.medialist.R;


public class FullscreenVideoFragment extends AppCompatDialogFragment {
    private static final String ARG_MEDIA_URL = "fullscreen:video:media:url";

    private String mediaUrl;
    ExoPlayerView playerView;

    public static FullscreenVideoFragment newInstance(String mediaUrl) {
        FullscreenVideoFragment fragment = new FullscreenVideoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MEDIA_URL, mediaUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getTheme() {
        return R.style.AppTheme_Fullscreen_Player;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mediaUrl = getArguments().getString(ARG_MEDIA_URL);
        }

        if (mediaUrl == null) {
            dismissAllowingStateLoss();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_media_fullscreen_video, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        playerView = (ExoPlayerView) view.findViewById(R.id.video);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // set fullscreen
        Window window = getDialog().getWindow();
        if (window != null) {
            View decorView = window.getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            Context context = getContext();
            DataSource.Factory dataSourceFactory =
                    new DefaultDataSourceFactory(context, Util.getUserAgent(context, context.getString(R.string.app_name)));

            MediaSource mediaSource = ExoPlayerHelper.buildMediaSource(getContext(), Uri.parse(mediaUrl),
                    dataSourceFactory, playerView.getHandler(), null);

            playerView.setAutoRestart(true);
            playerView.setResumePosition(0);
            playerView.setMediaSource(mediaSource, true);
        } catch (ParserException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SimpleExoPlayer player = playerView.getPlayer();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        SimpleExoPlayer player = playerView.getPlayer();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        SimpleExoPlayer player = playerView.getPlayer();
        if (player != null) {
            player.release();
        }
        mediaUrl = null;
    }
}
