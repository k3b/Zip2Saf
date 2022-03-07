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

package de.k3b.zip2saf.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * JSon based (android independent) in memory Repository implementation for {@link MountInfo} items.
 */
public class MountInfoRepository {
    private static Gson gson = setPrettyPrinting(false);
    private static final Type LIST_TYPE = new TypeToken<ArrayList<MountInfo>>() {}.getType();
    private static MountInfoRepository instance = null;
    @NotNull private static String createNewItemName = "[[add]]";

    private final List<MountInfo> ITEMS;
    private final HashMap<String, MountInfo> ID2MOUNT;

    /**
     * Workaround to avoid dependency to android(-string-resources)
     */
    public static void init(@NotNull String createNewItemName) {
        MountInfoRepository.createNewItemName = createNewItemName;
    }

    /** singleton */
    @NotNull public static MountInfoRepository getInstance() {
        if (instance == null) {
            instance = new MountInfoRepository(new ArrayList<>());
            DemoDataGenerator.addDemoItems(instance, 25);
        }
        return instance;
    }

    public static Gson setPrettyPrinting(boolean prettyPrinting) {
        GsonBuilder builder = new GsonBuilder();
        if (prettyPrinting) {
            builder.setPrettyPrinting();
        }
        gson = builder.create();
        return gson;
    }

    protected MountInfoRepository(@NotNull List<MountInfo> items) {
        ITEMS = items;
        ID2MOUNT = new HashMap<>();
        for (MountInfo item : ITEMS) {
            ID2MOUNT.put(item.zipId, item);
        }
        instance = this;

        fixCreateNewItem();
    }

    protected static class DemoDataGenerator {
        private static void addDemoItems(MountInfoRepository repository, int count) {
            // Add some sample items.
            for (int i = 1; i <= count; i++) {
                repository.add(createDemoItem(i));
            }
        }

        private static MountInfo createDemoItem(int position) {
            String zipId = "myZip_" + position + ".zip";
            return new MountInfo(zipId, "/path/to/" + zipId, makeDetails(position));
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

    public void add(@NotNull MountInfo item) {
        if (isSpecialItem(item)) {
            ITEMS.add(0,item);
        } else {
            ITEMS.add(item);
        }
        ID2MOUNT.put(item.zipId, item);
    }

    public void remove(@NotNull MountInfo item) {
        ITEMS.remove(item);
        ID2MOUNT.remove(item.zipId);
    }

    @Nullable public MountInfo getById(String s) {
        return ID2MOUNT.get(s);
    }

    @NotNull public List<MountInfo> getAll() {
        return ITEMS;
    }

    @Nullable public MountInfo getByPosition(int position) {
        if (position < 0 || position >= getCount()) {
            return null;
        }
        return ITEMS.get(position);
    }

    public int getCount() {
        return ITEMS.size();
    }

    /**
     * make shure that first item in repository is always "[[add]]" in the correct translation.
     */
    protected boolean fixCreateNewItem() {
        MountInfo first = getByPosition(0);
        boolean mustAdd = (first == null || !isSpecialItem(first));
        if (first != null && isSpecialItem(first) && 0 != first.zipId.compareTo(createNewItemName) ) {
            // wrong language translation
            remove(first);
            mustAdd = true;
        }
        if (mustAdd) {
            add(new MountInfo(createNewItemName, "", ""));
        }
        return mustAdd;
    }

    private boolean isSpecialItem(@NotNull MountInfo item) {
        return item.zipId.startsWith("[[");
    }

    /** persistance to json string */
    @Override
    @NotNull public String toString() {
        return gson.toJson(ITEMS);
    }

    /** persistance to json string */
    @NotNull public String toString(@NotNull MountInfo item) {
        return gson.toJson(item);
    }

    /** persistance from json string */
    @NotNull public static MountInfoRepository fromString(String jsonData) {
        return new MountInfoRepository(gson.fromJson(jsonData, LIST_TYPE));
    }
}