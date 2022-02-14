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

    /**
     * An array of sample (data) items.
     */
    public static final List<MountInfo> ITEMS = new ArrayList<>();

    /**
     * A map of sample (data) items, by ID.
     */
    public static final Map<String, MountInfo> ITEM_MAP = new HashMap<>();

    private static final int COUNT = 25;

    static {
        // Add some sample items.
        for (int i = 1; i <= COUNT; i++) {
            addItem(createPlaceholderItem(i));
        }
    }

    private static void addItem(MountInfo item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }

    private static MountInfo createPlaceholderItem(int position) {
        return new MountInfo(String.valueOf(position), "Item " + position, makeDetails(position));
    }

    private static String makeDetails(int position) {
        StringBuilder builder = new StringBuilder();
        builder.append("Details about Item: ").append(position);
        for (int i = 0; i < position; i++) {
            builder.append("\nMore details information here.");
        }
        return builder.toString();
    }

}