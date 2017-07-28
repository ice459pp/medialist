package com.shareba.medialist.adapter;

import android.content.Context;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.shareba.medialist.item.MediaItem;
import com.shareba.medialist.R;
import com.shareba.medialist.item.VideoItem;
import java.io.File;


public class MediaListAdapter extends CustomBaseAdapter<MediaItem> {
    private final static String TAG = "MediaListAdapter";
    private Context mContext;
    private LayoutInflater layoutInflater;
    private MediaListAdapterCallbacks mCallbacks;
    private float screenWidth;

    public interface MediaListAdapterCallbacks {
        void onMediaClick(int position, View view);
        void onCheckboxChange(int position);
    }

    public MediaListAdapter(Context context) {
        mContext = context;
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        screenWidth = displayMetrics.widthPixels;

        if (layoutInflater == null) {
            layoutInflater = LayoutInflater.from(context);
        }
    }

    public void setCallbacks(MediaListAdapterCallbacks callbacks) {
        this.mCallbacks = callbacks;
    }

    @Override
    public Object getItem(int position) {
        return get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final MediaViewHolder viewHolder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.adapter_media, null);
            viewHolder = new MediaViewHolder();
            viewHolder.ivMedia = (ImageView) convertView.findViewById(R.id.iv_media);
            viewHolder.ivPlayer = (ImageView) convertView.findViewById(R.id.iv_player);
            viewHolder.cbMedia = (CheckBox) convertView.findViewById(R.id.cb_media);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (MediaViewHolder) convertView.getTag();
        }

        int length = (int) (screenWidth / 3);
        viewHolder.ivMedia.getLayoutParams().width = length;
        viewHolder.ivMedia.getLayoutParams().height = length;

        // set media click listener
        viewHolder.ivMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCallbacks != null) {
                    mCallbacks.onMediaClick(position, view);
                }
            }
        });

        // set checkbox change listener
        viewHolder.cbMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCallbacks != null) {
                    mCallbacks.onCheckboxChange(position);
                }
            }
        });

        MediaItem mediaItem = get(position);
        File file = new File(mediaItem.getPath());
        Uri uri = Uri.fromFile(file);

        Glide.with(mContext)
                .load(uri)
                .placeholder(R.color.adapter_media_bg)
                .override(200, 200)
                .crossFade()
                .centerCrop()
                .into(viewHolder.ivMedia);

        if (mediaItem instanceof VideoItem) {
            viewHolder.ivPlayer.setVisibility(View.VISIBLE);
        } else {
            viewHolder.ivPlayer.setVisibility(View.GONE);
        }

        if (mediaItem.getChecked()) {
            viewHolder.cbMedia.setChecked(true);
        } else {
            viewHolder.cbMedia.setChecked(false);
        }

        return convertView;
    }

    private class MediaViewHolder {
        ImageView ivMedia;
        ImageView ivPlayer;
        CheckBox cbMedia;
    }

}
