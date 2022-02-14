package de.k3b.android.zip2saf.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class MountInfoRepository {

    private final List<MountInfo> ITEMS = new ArrayList<>();

    /**
     * A map of sample (data) items, by ID.
     */
    private final Map<String, MountInfo> ID2MOUNT = new HashMap<>();

    private static MountInfoRepository instance = null;

    private MountInfoRepository() {
        createDemoData();
    }

    private void createDemoData() {
        // Add some sample items.
        for (int i = 1; i <= 25; i++) {
            add(createPlaceholderItem(i));
        }
    }

    public static MountInfoRepository getInstance() {
        if (instance == null) {
            instance = new MountInfoRepository();
        }
        return instance;
    }

    private MountInfo createPlaceholderItem(int position) {
        return new MountInfo(String.valueOf(position), "Item " + position, makeDetails(position));
    }

    private String makeDetails(int position) {
        StringBuilder builder = new StringBuilder();
        builder.append("Details about Item: ").append(position);
        for (int i = 0; i < position; i++) {
            builder.append("\nMore details information here.");
        }
        return builder.toString();
    }

    public void add(MountInfo item) {
        ITEMS.add(item);
        ID2MOUNT.put(item.id, item);
    }

    public MountInfo getById(String s) {
        return ID2MOUNT.get(s);
    }

    public List<MountInfo> getAll() {
        return ITEMS;
    }
}