/*
    Copyright (C) 2020 - 2022 k3b

    This file is part of de.k3b.android.zip2saf (https://github.com/k3b/Zip2Saf/)

    This program is free software: you can redistribute it and/or modify it
    under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
    FOR A PARTICULAR PURPOSE. See the GNU General Public License
    for more details.

    You should have received a copy of the GNU General Public License along with
    this program. If not, see <http://www.gnu.org/licenses/>
    */


package de.k3b.android.zip2saf.widget;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import de.k3b.android.zip2saf.R;

/**
 * Manage permission in lifecycle for Android-6 ff {@link Activity}
 * * read from external-storage
 * <p>
 * implemented in {@link #onCreate(Bundle)}
 * when done executes in {@link #onCreateEx(Bundle)} in inherited class
 * <p>
 * how to use:
 * * In all activities replace ".... extends {@link Activity} with extends {@link FilePermissionActivity}
 * * rename {@link #onCreate(Bundle)} to {@link #onCreateEx(Bundle)}
 * * make shure that in onCreateEx() that there is no call to super.onCreate()
 */
public abstract class FilePermissionActivity extends Activity {
    public static final String TAG = "k3b.FilePermAct";

    private static final int REQUEST_ID_READ_EXTERNAL_STORAGE = 2000;
    private static final String PERMISSION_READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;

    private static final int RESULT_NO_PERMISSIONS = -22;


    // workflow onCreate() => requestPermission(PERMISSION_READ_EXTERNAL_STORAGE) => onRequestPermissionsResult() => abstract onCreateEx()
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && ActivityCompat.checkSelfPermission(this, PERMISSION_READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission(PERMISSION_READ_EXTERNAL_STORAGE, REQUEST_ID_READ_EXTERNAL_STORAGE);
        } else {
            afterPermissionsGranted(null);
        }
    }

    protected void afterPermissionsGranted(Bundle savedInstanceState) {
        onCreateEx(savedInstanceState);
    }

    protected abstract void onCreateEx(Bundle savedInstanceState);

    private void requestPermission(final String permission, final int requestCode) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ID_READ_EXTERNAL_STORAGE: {
                final boolean success = (grantResults != null)
                        && (grantResults.length > 0)
                        && (grantResults[0] == PackageManager.PERMISSION_GRANTED);
                if (success) {
                    afterPermissionsGranted(null);
                } else {
                    Log.i(TAG, this.getClass().getSimpleName()
                            + ": " + getText(R.string.permission_error));
                    Toast.makeText(this, R.string.permission_error, Toast.LENGTH_LONG).show();
                    setResult(RESULT_NO_PERMISSIONS, null);
                    finish();
                }
                return;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
