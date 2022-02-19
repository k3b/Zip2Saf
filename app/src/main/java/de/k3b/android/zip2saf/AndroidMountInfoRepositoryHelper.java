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

package de.k3b.android.zip2saf;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import de.k3b.zip2saf.data.MountInfoRepository;

/** Add android specific persistence of android independant {@link MountInfoRepository} */
public class AndroidMountInfoRepositoryHelper {

    public static void saveRepository(Context context) {
        SharedPreferences prefsInstance = PreferenceManager.getDefaultSharedPreferences(context);

        prefsInstance
                .edit()
                .putString("mounts", MountInfoRepository.getInstance().toString())
                .apply();
    }

    public static void loadRepository(Context context) {
        SharedPreferences prefsInstance = PreferenceManager.getDefaultSharedPreferences(context);

        String json = prefsInstance.getString("mounts","[]");
        MountInfoRepository.fromString(json);
    }
}
