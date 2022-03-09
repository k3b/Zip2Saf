/*
    Copyright (C) 2022 k3b

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
package de.k3b.android.zip2saf;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import de.k3b.android.zip2saf.widget.FilePermissionActivity;
import de.k3b.zip2saf.data.MountInfoRepository;

public class MountListActivity extends FilePermissionActivity {
    private RecyclerView mountList = null;
    private MountService mountService = null;

    @Override
    public void onCreateEx(Bundle savedInstanceState) {
        mountService = new MountService(this, MountInfoRepository.getInstance());
        this.setContentView(R.layout.activity_mountinfo_list);
        this.mountList = findViewById(R.id.mountinfo_list);
        this.mountList.setAdapter(mountService.createAdapter());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MountService.REQUEST_ZIP_FILE) {
            Uri mountUri = (resultCode == Activity.RESULT_OK && data != null) ? data.getData() : null;
            // Answer from {@link MountService#requestMount()}
            if (mountUri != null) {
                String zipID = mountService.getZipID(mountUri);
                String errorMessage = mountService.mount(zipID, mountUri.toString());
                if (errorMessage != null) {
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG );
                }
            }
        } else {
          super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
