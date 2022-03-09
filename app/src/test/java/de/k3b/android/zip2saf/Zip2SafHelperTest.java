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

import static org.junit.Assert.assertEquals;

import android.database.MatrixCursor;

import net.lingala.zip4j.model.LocalFileHeader;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class Zip2SafHelperTest {

    @Test
    public void getRootId() {
        ZipReadStorageProvider.debug = false; // no logcat output
        assertEquals("root", Zip2SafHelper.getRootId("/root/path/file"));
        assertEquals("root", Zip2SafHelper.getRootId("root/path/file"));
        assertEquals("root", Zip2SafHelper.getRootId("root/"));
        assertEquals("root", Zip2SafHelper.getRootId("root"));
        assertEquals(null, Zip2SafHelper.getRootId(""));
        assertEquals(null, Zip2SafHelper.getRootId(null));
    }

    @Test
    public void getZipPath() {
        ZipReadStorageProvider.debug = false; // no logcat output
        assertEquals("path/file", Zip2SafHelper.getZipPath("/root/path/file"));
        assertEquals("path/file", Zip2SafHelper.getZipPath("root/path/file"));
        assertEquals("", Zip2SafHelper.getZipPath("root/"));
        assertEquals("", Zip2SafHelper.getZipPath("root"));
        assertEquals("", Zip2SafHelper.getZipPath(""));
        assertEquals("", Zip2SafHelper.getZipPath(null));
    }

    @Test
    public void getDirectoryID() {
        assertEquals("", Zip2SafHelper.getDirectoryID(null));
        assertEquals("", Zip2SafHelper.getDirectoryID(""));
        assertEquals("dir/", Zip2SafHelper.getDirectoryID("dir"));
        assertEquals("dir/", Zip2SafHelper.getDirectoryID("dir/"));
    }

    @Test
    public void getRelPath() {
        assertEquals("file.ext", Zip2SafHelper.getRelPath("file.ext", ""));

        assertEquals("file.ext", Zip2SafHelper.getRelPath("path/file.ext", "path/"));

        assertEquals(null, Zip2SafHelper.getRelPath("path/file.ext", "path/subdir/"));

        assertEquals("subdir/file.ext", Zip2SafHelper.getRelPath("path/subdir/file.ext", "path/"));
    }

}