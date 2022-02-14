package de.k3b.android.zip2saf.data;

import androidx.annotation.NonNull;

import java.io.Serializable;

/**
 * A data item representing a piece of content.
 */
public class MountInfo implements Serializable {
    @NonNull public final String zipId;
    @NonNull public final String uri;
    public final String details;

    public static final MountInfo EMPTY = new MountInfo("","","");

    public MountInfo(@NonNull String zipId, @NonNull String uri, String details) {
        this.zipId = zipId;
        this.uri = uri;
        this.details = details;
    }

    @Override
    @NonNull public String toString() {
        return zipId + "[" + uri + "]";
    }
}
