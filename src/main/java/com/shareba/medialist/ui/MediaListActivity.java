package com.shareba.medialist.ui;


import android.Manifest;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.shareba.medialist.R;
import com.shareba.medialist.adapter.MediaListAdapter;
import com.shareba.medialist.item.ImageItem;
import com.shareba.medialist.item.MediaItem;
import com.shareba.medialist.item.VideoItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;


public class MediaListActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,
        LoaderManager.LoaderCallbacks<Cursor> {
    public final static String MEDIA_LIST_DATA = "media:list:data";
    public final static int MEDIA_LIST_REQUEST = 2232;
    private final static String TAG = "MediaListActivity";
    private final static int PERMISSION_REQUEST = 10000;
    private final static int ID_LOADER_MEDIA_LIST = 1;
    private final static int SELECT_LIMIT = 10;
    private final static int QUERY_LIMIT = 5000;
    private final String[] mProjection = {
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.TITLE,
    };

    private final String mWhere = MediaStore.Files.FileColumns.MEDIA_TYPE + " = ? OR "
            + MediaStore.Files.FileColumns.MEDIA_TYPE + " = ?";

    private final String[] mArgs = {
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
    };

    private final String[] mPermission = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private final String mOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC LIMIT " + QUERY_LIMIT;

    private ProgressBar progressBar;
    private TextView tvError;
    private GridView gridView;
    private MediaListAdapter mAdapter;
    private List<MediaItem> mList;
    private List<MediaItem> mSelected;
    private LoaderManager mLoaderManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medialist);
        if (getIntent() == null) {
            finish();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_media_list);
        }

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);
        tvError = (TextView) findViewById(R.id.tv_error);
        tvError.setVisibility(View.INVISIBLE);
        gridView = (GridView) findViewById(R.id.grid_view);
        gridView.setVisibility(View.GONE);
        gridView.setNumColumns(3);
        gridView.setHorizontalSpacing(2);
        gridView.setVerticalSpacing(2);

        if (mSelected == null) {
            mSelected = new ArrayList<>();
        }
        if (mList == null) {
            mList = new ArrayList<>();
        }

        // setup mAdapter
        if (mAdapter == null) {
            mAdapter = new MediaListAdapter(this);
            mAdapter.setCallbacks(new MediaListAdapter.MediaListAdapterCallbacks() {
                @Override
                public void onMediaClick(int position, View view) {
                    MediaItem mediaItem = (MediaItem) mAdapter.getItem(position);
                    if (mediaItem instanceof VideoItem) {
                        VideoItem videoItem = (VideoItem) mediaItem;
                        showFullscreenVideo(videoItem);
                    }

                    if (mediaItem instanceof ImageItem) {
                        ImageItem imageItem = (ImageItem) mediaItem;
                        showFullscreenImage(imageItem);
                    }
                }

                @Override
                public void onCheckboxChange(int position) {
                    if (mSelected.size() > SELECT_LIMIT) {
                        Log.d(TAG, "there's a limit upload number");
                        return;
                    }
                    MediaItem mediaItem = mAdapter.get(position);
                    if (mSelected.contains(mediaItem)) {
                        mediaItem.setChecked(false);
                        mSelected.remove(mediaItem);
                    } else {
                        mediaItem.setChecked(true);
                        mSelected.add(mediaItem);
                    }
                }
            });
        }

        gridView.setAdapter(mAdapter);
        checkPermission();
    }

    private void showFullscreenVideo(VideoItem videoItem) {
        FullscreenVideoFragment fragment =
                FullscreenVideoFragment.newInstance(videoItem.getPath());
        fragment.setTargetFragment(fragment, 2000);
        FragmentManager manager = getSupportFragmentManager();
        fragment.show(manager, TAG);
    }

    private void showFullscreenImage(ImageItem imageItem) {
        FullscreenImageFragment fragment = FullscreenImageFragment.newInstance(imageItem.getPath());
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showFullscreenImageActivity(ImageItem imageItem, View view) {
        ImageView imageView = (ImageView) view.findViewById(R.id.iv_media);
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(MediaListActivity.this, imageView,
                        ViewCompat.getTransitionName(imageView));
        Intent intent = new Intent(MediaListActivity.this, FullscreenImageActivity.class);
        intent.putExtra(FullscreenImageActivity.ARG_MEDIA_URL, imageItem.getPath());
        startActivity(intent, options.toBundle());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.media_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_media_list) {
            collectMediaPath();
            return true;
        }

        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void collectMediaPath() {
        ArrayList<String> mediaPath = new ArrayList<>();
        for (MediaItem mediaItem : mSelected) {
            mediaPath.add(mediaItem.getPath());
        }
        Intent intent = new Intent();
        intent.putStringArrayListExtra(MEDIA_LIST_DATA, mediaPath);
        setResult(AppCompatActivity.RESULT_OK, intent);
        finish();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void loadMediaList() {
        if (mLoaderManager == null) {
            mLoaderManager = getSupportLoaderManager();
        }
        mLoaderManager.initLoader(ID_LOADER_MEDIA_LIST, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // mQueryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        // mQueryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Uri queryUri = MediaStore.Files.getContentUri("external");
        return new CursorLoader(this, queryUri, mProjection, mWhere, mArgs, mOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null) {
            Log.d(TAG, "fetch error");
            return;
        }

        while (cursor.moveToNext()) {
            /**
             * private final String[] mProjection = {
             MediaStore.Files.FileColumns._ID,
             MediaStore.Files.FileColumns.DATA,
             MediaStore.Files.FileColumns.MEDIA_TYPE,
             MediaStore.Files.FileColumns.MIME_TYPE,
             MediaStore.Files.FileColumns.TITLE
             };
             */
            long id = cursor.getLong(cursor.getColumnIndex(mProjection[0]));
            String path = cursor.getString(cursor.getColumnIndex(mProjection[1]));
            int mediaType = cursor.getInt(cursor.getColumnIndex(mProjection[2]));
            String mimeType = cursor.getString(cursor.getColumnIndex(mProjection[3]));
            String title = cursor.getString(cursor.getColumnIndex(mProjection[4]));

            try {
                File file = new File(path);
                if (file.exists()) {
                    switch (mediaType) {
                        case MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE:
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
        mAdapter.addAll(mList);
        progressBar.setVisibility(View.GONE);
        gridView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Loader<Cursor> loader = mLoaderManager.getLoader(ID_LOADER_MEDIA_LIST);
        if (loader != null) {
            loader.cancelLoad();
        }
        mLoaderManager.destroyLoader(ID_LOADER_MEDIA_LIST);
        mLoaderManager = null;
    }

    private void checkPermission() {
        if (EasyPermissions.hasPermissions(this, mPermission)) {
            loadMediaList();
        } else {
            // Ask for one mPermission
            EasyPermissions.requestPermissions(this, getString(R.string.title_permission_ask_for), PERMISSION_REQUEST, mPermission);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "granted");
        loadMediaList();
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

}
