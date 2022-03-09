package de.k3b.android.zip2saf;

import android.app.Application;

import de.k3b.zip2saf.data.MountInfoRepository;

public class Zip2SafApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MountInfoRepository.init(getString(R.string.title_mountinfo_new_item));
        AndroidMountInfoRepositoryHelper.loadRepository(this);
    }
}
