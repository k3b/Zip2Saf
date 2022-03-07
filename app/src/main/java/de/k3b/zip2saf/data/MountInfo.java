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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A data item representing a piece of content. (android independent) .
 */
public class MountInfo  {
    /** name of zip file without path. I.E: Test.zip */
    @NotNull
    public final String zipId;

    /** full path/uri to zip file, result of {@link android.content.Intent#ACTION_OPEN_DOCUMENT} */
    @NotNull public final String uri;

    @Nullable public final String details;

    public static final MountInfo EMPTY = new MountInfo("","","");

    public MountInfo(@NotNull String zipId, @NotNull String uri, @Nullable String details) {
        this.zipId = zipId;
        this.uri = uri;
        this.details = details;
    }

    @Override
    @NotNull public String toString() {
        return zipId + "[" + uri + "]";
    }
}
