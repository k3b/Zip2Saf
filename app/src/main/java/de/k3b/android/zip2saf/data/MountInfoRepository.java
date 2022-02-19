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
            add(createDemoItem(i));
        }
    }

    public static MountInfoRepository getInstance() {
        if (instance == null) {
            instance = new MountInfoRepository();
        }
        return instance;
    }

    private MountInfo createDemoItem(int position) {
        String zipId = "myZip_" + position + ".zip";
        return new MountInfo(zipId, "/path/to/" + zipId, makeDetails(position));
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
        ID2MOUNT.put(item.zipId, item);
    }

    public MountInfo getById(String s) {
        return ID2MOUNT.get(s);
    }

    public List<MountInfo> getAll() {
        return ITEMS;
    }

    public MountInfo getByPosition(int position) {
        if (position < 0 || position >= getCount()) {
            return null;
        }
        return ITEMS.get(position);
    }

    public int getCount() {
        return ITEMS.size();
    }
}