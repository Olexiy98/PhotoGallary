package com.example.olexi.photogallery.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.olexi.photogallery.R;
import com.example.olexi.photogallery.activity.PhotoPageActivity;
import com.example.olexi.photogallery.http.FlickrFetchr;
import com.example.olexi.photogallery.models.GalleryItem;
import com.example.olexi.photogallery.preference.QueryPreferences;
import com.example.olexi.photogallery.service.PollService;
import com.example.olexi.photogallery.thread.ThumbnailDownloader;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends VisibleFragment {

    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<ActivityHolder> mThumbnailDownloader;

    public static PhotoGalleryFragment newInstance() {
        Bundle args = new Bundle();
        PhotoGalleryFragment fragment = new PhotoGalleryFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        if(checkInternetConnection()){
            updateItems();
        }else {
            Toast.makeText(getActivity(),"not internet connection", Toast.LENGTH_SHORT).show();
        }

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<ActivityHolder>() {
            @Override
            public void onThumbnailDownloaded(ActivityHolder target, Bitmap thumbnail) {
                Log.i(TAG, "getResources ");
                if(isAdded()){
                    Drawable drawable = new BitmapDrawable(getResources(),thumbnail);
                    target.bindView(drawable);
                }
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery,container,false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));
        setupAdapter();
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_fragment_photo_gallery, menu);
        MenuItem searchItem =  menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(mSearchQueryTextListener);
        searchView.setOnSearchClickListener(mSearchClickListener);

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if(PollService.isServiceAlarmOn(getActivity())){
            toggleItem.setTitle(R.string.stop_polling);
        }else {
            toggleItem.setTitle(R.string.start_polling);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroyView() {
        mThumbnailDownloader.clearQueue();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        mThumbnailDownloader.quit();
        super.onDestroy();
    }

    private void setupAdapter(){
        if(isAdded()){
            mRecyclerView.setAdapter(new ActivityAdapter(mItems));
        }
    }

    private boolean checkInternetConnection(){
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void updateItems(){
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute();
    }

    private SearchView.OnQueryTextListener mSearchQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            QueryPreferences.setStoredQuery(getActivity(), query);
            updateItems();
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            return false;
        }
    };

    private View.OnClickListener mSearchClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String query = QueryPreferences.getStoredQuery(getActivity());
            ((SearchView) v).setQuery(query, false);
        }
    };

    private class FetchItemsTask extends AsyncTask<Void,Void,List<GalleryItem>>{

        String query;

        public FetchItemsTask(String query){
            this.query = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
            if(query == null){
                return new FlickrFetchr().fetchRecentPhotos();
            }else {
                return new FlickrFetchr().searchPhotos(query);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            super.onPostExecute(items);
            mItems = items;
            setupAdapter();
        }
    }

    private class ActivityHolder extends RecyclerView.ViewHolder
    implements View.OnClickListener{

        private ImageView mImageView;
        private GalleryItem mGalleryItem;

        public ActivityHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.iv_photo_gallery);
            mImageView.setOnClickListener(this);
        }

        public void bindView(Drawable drawable){
           mImageView.setImageDrawable(drawable);
        }

        public void bindGalleryItem(GalleryItem galleryItem){
            mGalleryItem = galleryItem;
        }

        @Override
        public void onClick(View v) {
            Intent intent = PhotoPageActivity.newIntent(getActivity(),mGalleryItem.getPhotoPageUri());
            startActivity(intent);
        }
    }

    private class ActivityAdapter extends RecyclerView.Adapter<ActivityHolder>{

        List<GalleryItem> items;

        public ActivityAdapter(List<GalleryItem> items){
            this.items = items;
        }

        @NonNull
        @Override
        public ActivityHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.gallery_item,parent,false);
            return new ActivityHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ActivityHolder holder, int position) {
            GalleryItem item = items.get(position);
            Drawable placeHolder = getResources().getDrawable(android.R.drawable.ic_media_ff);
            holder.bindView(placeHolder);
            holder.bindGalleryItem(item);
            mThumbnailDownloader.queueThumbnail(holder,item.getUrl());
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}
