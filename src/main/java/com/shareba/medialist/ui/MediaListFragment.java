package com.shareba.medialist.ui;


import android.Manifest;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.shareba.medialist.adapter.MediaListAdapter;
import com.shareba.medialist.item.ImageItem;
import com.shareba.medialist.item.MediaItem;
import com.shareba.medialist.R;
import com.shareba.medialist.item.VideoItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MediaListFragment extends Fragment {

    private final static String TAG = "MediaListFragment";
    private final static int FETCH_STARTED = 1;
    private final static int FETCH_COMPLETED = 2;
    private final static int ERROR = 3;

    private final String[] projection = {
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.TITLE,
    };

    private final String where = MediaStore.Files.FileColumns.MEDIA_TYPE + " = ? OR "
            + MediaStore.Files.FileColumns.MEDIA_TYPE + " = ?";

    private final String[] args = {
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
    };

    private final String[] permission = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private final String order = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC LIMIT 2000";

    private ProgressBar progressBar;
    private TextView tvError;
    private GridView gridView;
    private Thread mThread;
    private Handler mHandler;
    private ContentObserver mObserver;
    private Uri mQueryUri;
    private MediaListAdapter mAdapter;
    private List<MediaItem> mList;
    private FragmentActivity mActivity;

    public static MediaListFragment newInstance() {
        MediaListFragment fragment = new MediaListFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_media_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mQueryUri = MediaStore.Files.getContentUri("external");
        mActivity = getActivity();
        init(view);
    }

    private void init(View view) {
        if (view == null) {
            return;
        }
        progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        tvError = (TextView) view.findViewById(R.id.tv_error);
        tvError.setVisibility(View.INVISIBLE);
        gridView = (GridView) view.findViewById(R.id.grid_view);
        gridView.setNumColumns(3);
        gridView.setHorizontalSpacing(2);
        gridView.setVerticalSpacing(2);
//        mQueryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
//        mQueryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        if (mList == null) {
            mList = new ArrayList<>();
        }

        // setup mAdapter
        if (mAdapter == null) {
            mAdapter = new MediaListAdapter(mActivity);
        }
        gridView.setAdapter(mAdapter);
        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MediaItem mediaItem = (MediaItem) mAdapter.getItem(position);
                if (mediaItem instanceof VideoItem) {
                    VideoItem videoItem = (VideoItem) mediaItem;
                    showFullscreenVideo(videoItem);
                }

                if (mediaItem instanceof ImageItem) {
                    ImageItem imageItem = (ImageItem) mediaItem;
                    FullscreenImageFragment fragment =
                            FullscreenImageFragment.newInstance(imageItem.getPath());

                    ImageView imageView = (ImageView) view.findViewById(R.id.iv_media);
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .addSharedElement(imageView, getString(R.string.media_transition))
                            .replace(android.R.id.content, fragment)
                            .addToBackStack(null)
                            .commit();
//                    showFullscreenImage(view, imageItem);
                }
            }
        });

        loadMediaList();
    }

//    private void showFullscreenImage(View view, ImageItem imageItem) {
//        FullscreenImageFragment fragment =
//                FullscreenImageFragment.newInstance(imageItem.getPath());
//        fragment.setTargetFragment(fragment, 2000);
//        FragmentManager manager = getChildFragmentManager();
//        manager.beginTransaction().addSharedElement(view, ViewCompat.getTransitionName(view)).commit();
//        fragment.show(manager, TAG);
//    }

    private void showFullscreenVideo(VideoItem videoItem) {
        FullscreenVideoFragment fragment =
                FullscreenVideoFragment.newInstance(videoItem.getPath());
        fragment.setTargetFragment(fragment, 2000);
        FragmentManager manager = getChildFragmentManager();
        fragment.show(manager, TAG);
    }

    private void sendMessage(int what) {
        if (mHandler == null) {
            return;
        }

        Message message = mHandler.obtainMessage();
        message.what = what;
        message.sendToTarget();
    }

    @Override
    public void onStart() {
        super.onStart();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {

                    case FETCH_STARTED:
                        Log.d(TAG, "fetch started");
                        mList.clear();
                        progressBar.setVisibility(View.VISIBLE);
                        gridView.setVisibility(View.INVISIBLE);
                        break;

                    case FETCH_COMPLETED:
                        Log.d(TAG, "fetch completed");
                        mAdapter.addAll(mList);
                        progressBar.setVisibility(View.GONE);
                        gridView.setVisibility(View.VISIBLE);
                        // set mAdapter
                        break;

                    case ERROR:
                        Log.d(TAG, "fetch error");
                        progressBar.setVisibility(View.GONE);
                        // show error message
                        break;

                    default:
                        super.handleMessage(msg);
                }
            }
        };

        mObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                loadMediaList();
            }
        };

        mActivity.getContentResolver().registerContentObserver(mQueryUri, false, mObserver);
    }

    private void loadMediaList() {
        startThread(new ImageLoaderRunnable());
    }

    private class ImageLoaderRunnable implements Runnable {

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            sendMessage(FETCH_STARTED);

            Cursor cursor = mActivity.getContentResolver().query(mQueryUri, projection, where, args, order);

            if (cursor == null) {
                sendMessage(ERROR);
                return;
            }

            Log.d(TAG, "cursor size: " + cursor.getCount());
            while (cursor.moveToNext()) {
                if (Thread.interrupted()) {
                    return;
                }

                /**
                 * private final String[] projection = {
                 MediaStore.Files.FileColumns._ID,
                 MediaStore.Files.FileColumns.DATA,
                 MediaStore.Files.FileColumns.MEDIA_TYPE,
                 MediaStore.Files.FileColumns.MIME_TYPE,
                 MediaStore.Files.FileColumns.TITLE
                 };
                 */
                long id = cursor.getLong(cursor.getColumnIndex(projection[0]));
                String path = cursor.getString(cursor.getColumnIndex(projection[1]));
                int mediaType = cursor.getInt(cursor.getColumnIndex(projection[2]));
                String mimeType = cursor.getString(cursor.getColumnIndex(projection[3]));
                String title = cursor.getString(cursor.getColumnIndex(projection[4]));

                File file = null;
                try {
                    file = new File(path);
                    if (file.exists()) {
                        Log.d(TAG, "mediaType:file: " + mediaType + ": " + path);
                        switch (mediaType) {
                            case MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE:
//                                ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MICRO_KIND);
                                mList.add(new ImageItem(title, path));
                                break;

                            case MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO:
                                mList.add(new VideoItem(title, path));
                                break;

                            default:
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }

            cursor.close();
            sendMessage(FETCH_COMPLETED);

        }
    }

    @Override
    public void onStop() {
        super.onStop();

        stopThread();
        mActivity.getContentResolver().unregisterContentObserver(mObserver);
        mObserver = null;
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }

    private void startThread(Runnable runnable) {
        stopThread();
        mThread = new Thread(runnable);
        mThread.start();
    }

    private void stopThread() {
        if (mThread == null || !mThread.isAlive()) {
            return;
        }

        mThread.interrupt();
        try {
            mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
