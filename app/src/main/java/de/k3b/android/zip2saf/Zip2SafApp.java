package de.k3b.android.zip2saf;

import android.app.Application;
import android.util.Log;

import de.k3b.zip2saf.data.MountInfoRepository;

public class Zip2SafApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MountInfoRepository.init(getString(R.string.title_mountinfo_new_item));
        AndroidMountInfoRepositoryHelper.loadRepository(this);

        // log info when resources where not released
        // from https://wh0.github.io/2020/08/12/closeguard.html
        try {
            Class.forName("dalvik.system.CloseGuard")
                    .getMethod("setEnabled", boolean.class)
                    .invoke(null, true);
        } catch (ReflectiveOperationException e) {
            Log.i(ZipReadStorageProvider.TAG, "not supported: dalvik.system.CloseGuard.setEnabled(true); ");
        }

    }
}
