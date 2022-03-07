package de.k3b.android.zip2saf;

import android.os.Bundle;

import androidx.recyclerview.widget.RecyclerView;

import de.k3b.android.zip2saf.widget.FilePermissionActivity;
import de.k3b.zip2saf.data.MountInfoRepository;

public class MountListActivity extends FilePermissionActivity {
    private RecyclerView mountList = null;
    private MountInfoRepository repo = MountInfoRepository.getInstance();

    @Override
    public void onCreateEx(Bundle savedInstanceState) {
        // super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_mountinfo_list);
        this.mountList = findViewById(R.id.mountinfo_list);
        this.refreshList();
    }

    private void refreshList() {
        this.mountList.setAdapter(new MountInfoListItemRecyclerViewAdapter(repo, null));
    }
}
