package com.example.olexi.photogallery.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.example.olexi.photogallery.R;
import com.example.olexi.photogallery.fragment.PhotoPageFragment;
import com.example.olexi.photogallery.tools.OnBackPressed;

public class PhotoPageActivity extends SingleFragmentActivity {

    public static Intent newIntent(Context context, Uri uri){
        Intent intent = new Intent(context,PhotoPageActivity.class);
        intent.setData(uri);
        return intent;
    }

    @Override
    public Fragment createFragment() {
        return PhotoPageFragment.newInstance(getIntent().getData());
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if(!(fragment instanceof OnBackPressed) || !((OnBackPressed)fragment).onBack())
            super.onBackPressed();
    }
}
