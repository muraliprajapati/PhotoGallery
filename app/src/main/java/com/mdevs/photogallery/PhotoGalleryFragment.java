package com.mdevs.photogallery;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;

/**
 * Created by Murali on 22-05-2015.
 */
public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = "PhotoGalleryFragment";

    GridView mGridView;
    ArrayList<GalleryItem> mItems;
    ThumbnailDownloder<ImageView> mThumbnailDownloder;
    Handler handler;
    ImageView imageView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        //Intent intent = new Intent(getActivity(), PollService.class);
        //getActivity().startService(intent);
        //PollService.setServiceAlarm(getActivity(), true);

        handler = new Handler();
        mThumbnailDownloder = new ThumbnailDownloder<ImageView>(handler);
        mThumbnailDownloder.setListener(new ThumbnailDownloder.Listener<ImageView>() {
            @Override
            public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
                if (isVisible()) {
                    imageView.setImageBitmap(thumbnail);
                }
            }
        });
        mThumbnailDownloder.start();
        mThumbnailDownloder.getLooper();
        Log.i(TAG, "Background thread started");
        if (networkState()) updateItems();
        else showError();

    }

    private void showError() {
        FragmentManager manager = getActivity().getFragmentManager();
        Dialog dialog = new Dialog();
        dialog.show(manager, "dialog");
    }

    public void updateItems() {
        if (networkState()) {
            new FetchItemsTask().execute();
        } else showError();


    }

    public boolean networkState() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloder.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mGridView = (GridView) v.findViewById(R.id.gridView);
        setupAdapter();
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloder.clearQueue();
    }

    private void setupAdapter() {
        if (getActivity() == null || mGridView == null) return;
        if (mItems != null) {
            //ArrayAdapter<GalleryItem> adapter = new ArrayAdapter<GalleryItem>(getActivity(),android.R.layout.simple_gallery_item,mItems);
            GalleryAdapter<GalleryItem> adapter = new GalleryAdapter<GalleryItem>(getActivity(), mItems);
            mGridView.setAdapter(adapter);


        } else {
            mGridView.setAdapter(null);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_photo_gallery, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_search:
                getActivity().onSearchRequested();
                return true;
            case R.id.menu_item_clear:
                PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .edit()
                        .putString(FlickrFetchr.PREF_SEARCH_QUERY, null)
                        .commit();
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class GalleryAdapter<GalleryItems> extends ArrayAdapter<GalleryItem> {

        public GalleryAdapter(Context context, ArrayList<GalleryItem> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.gallery_item, parent, false);
            }
            ImageView imageView = (ImageView) convertView.findViewById(R.id.gallery_item_imageView);
            imageView.setImageResource(R.drawable.user);

            GalleryItem item = getItem(position);
            mThumbnailDownloder.queueThumbnail(imageView, item.getmUrl());
            return convertView;
        }
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, ArrayList<GalleryItem>> {
        @Override
        protected ArrayList<GalleryItem> doInBackground(Void... params) {
            Activity activity = getActivity();
            if (activity == null)
                return new ArrayList<GalleryItem>();
            String query = PreferenceManager.getDefaultSharedPreferences(activity)
                    .getString(FlickrFetchr.PREF_SEARCH_QUERY, null);

            if (query != null) {
                try {
                    return new FlickrFetchr().search(query);
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    return new FlickrFetchr().fetchItems();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<GalleryItem> galleryItems) {
            mItems = galleryItems;
            setupAdapter();
        }
    }
}
