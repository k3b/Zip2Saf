package de.k3b.android.zip2saf.data;

import androidx.annotation.NonNull;

/**
 * A data item representing a piece of content.
 */
public class MountInfo {
    @NonNull public final String id;
    @NonNull public final String content;
    public final String details;

    public MountInfo(@NonNull String id, @NonNull String content, String details) {
        this.id = id;
        this.content = content;
        this.details = details;
    }

    @Override
    @NonNull public String toString() {
        return content;
    }
}
