package com.example.olexi.photogallery.activity;

import android.support.v4.app.Fragment;

import com.example.olexi.photogallery.fragment.PhotoGalleryFragment;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    @Override
    public Fragment createFragment() {
        return PhotoGalleryFragment.newInstance();
    }
}
