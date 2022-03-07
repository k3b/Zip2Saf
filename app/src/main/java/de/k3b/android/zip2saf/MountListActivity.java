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
        // super.onCreate(savedInstanceState);
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
