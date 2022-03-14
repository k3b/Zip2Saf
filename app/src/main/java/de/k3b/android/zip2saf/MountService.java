package de.k3b.android.zip2saf;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import de.k3b.zip2saf.data.MountInfo;
import de.k3b.zip2saf.data.MountInfoRepository;

public class MountService {
    public static final int REQUEST_ZIP_FILE = 2001;

    private final Activity activity;
    private final MountInfoRepository repository;
    private MountInfoListItemRecyclerViewAdapter adapter;

    public MountService(Activity activity, MountInfoRepository repository) {
        this.activity = activity;
        this.repository = repository;
    }

    public RecyclerView.Adapter createAdapter() {
        adapter = new MountInfoListItemRecyclerViewAdapter(repository, new MountInfoListItemRecyclerViewAdapter.OnClickHandler() {
            @Override
            public void onListClick(MountInfo item) {
                onMountClick(item);
            }
        });
        return adapter;
    }

    private void onMountClick(MountInfo item) {
        if (item != null) {
            if (repository.isSpecialItem(item)) {
                requestMount();
            } else {
                repository.remove(item);
                Zip2SafHelper.clearThumbCache(activity, item.zipId);
                notifyChange();
            }
        }
    }

    private void notifyChange() {
        adapter.notifyDataSetChanged();
        activity.getContentResolver()
                .notifyChange(DocumentsContract.buildRootsUri(BuildConfig.DOCUMENTS_AUTHORITY), null);
        AndroidMountInfoRepositoryHelper.saveRepository(activity.getApplicationContext(), repository);
    }

    public void requestMount() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/zip");
        activity.startActivityForResult(intent, REQUEST_ZIP_FILE);
    }

    @NonNull
    public static String getZipID(@NonNull Uri mountUri) {
        String uriLastPathSegment = mountUri.getLastPathSegment();
        String zipID = uriLastPathSegment.substring(uriLastPathSegment.lastIndexOf('/') + 1);
        return zipID;
    }

    /**
     * @return errormessage or null if all is ok
     */
    public String mount(@NotNull String zipID, @NotNull String uri) {
        if (repository.getById(zipID) == null) {
            // new MountInfo("added", "path/to/added", "added details")
            MountInfo mountInfo = new MountInfo(zipID, uri, null);
            this.repository.add(mountInfo);
            AndroidMountInfoRepositoryHelper.saveRepository(this.activity.getApplicationContext(), this.repository);
            notifyChange();

            return null;
        } else {
            return activity.getString(R.string.zip_already_mounted_error);
        }
    }
}
