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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * JSon based (android independant) in memory Repository implementation for {@link MountInfo} items.
 */
public class MountInfoRepository {
    private static Gson gson = setPrettyPrinting(false);
    private static final Type LIST_TYPE = new TypeToken<ArrayList<MountInfo>>() {}.getType();
    private static MountInfoRepository instance = null;

    private final List<MountInfo> ITEMS;
    private final HashMap<String, MountInfo> ID2MOUNT;

    public static MountInfoRepository getInstance() {
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

    protected MountInfoRepository(List<MountInfo> items) {
        ITEMS = items;
        ID2MOUNT = new HashMap<>();
        for (MountInfo item : ITEMS) {
            ID2MOUNT.put(item.zipId, item);
        }
        instance = this;
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

    @Override
    public String toString() {
        return gson.toJson(ITEMS);
    }

    public String toString(MountInfo item) {
        return gson.toJson(item);
    }

    public static MountInfoRepository fromString(String jsonData) {
        return new MountInfoRepository(gson.fromJson(jsonData, LIST_TYPE));
    }
}