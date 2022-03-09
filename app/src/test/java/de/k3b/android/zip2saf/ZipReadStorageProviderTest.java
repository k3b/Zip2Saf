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

import static org.junit.Assert.*;

import android.database.MatrixCursor;

import net.lingala.zip4j.model.LocalFileHeader;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class ZipReadStorageProviderTest {

    @Test
    public void getRootId() {
        ZipReadStorageProvider.debug = false; // no logcat output
        assertEquals("root", ZipReadStorageProvider.getRootId("/root/path/file"));
        assertEquals("root", ZipReadStorageProvider.getRootId("root/path/file"));
        assertEquals("root", ZipReadStorageProvider.getRootId("root/"));
        assertEquals("root", ZipReadStorageProvider.getRootId("root"));
        assertEquals(null, ZipReadStorageProvider.getRootId(""));
        assertEquals(null, ZipReadStorageProvider.getRootId(null));
    }

    @Test
    public void getZipPath() {
        ZipReadStorageProvider.debug = false; // no logcat output
        assertEquals("path/file", ZipReadStorageProvider.getZipPath("/root/path/file"));
        assertEquals("path/file", ZipReadStorageProvider.getZipPath("root/path/file"));
        assertEquals("", ZipReadStorageProvider.getZipPath("root/"));
        assertEquals("", ZipReadStorageProvider.getZipPath("root"));
        assertEquals("", ZipReadStorageProvider.getZipPath(""));
        assertEquals("", ZipReadStorageProvider.getZipPath(null));
    }

    @Test
    public void getDirectoryID() {
        assertEquals("", ZipReadStorageProvider.getDirectoryID(null));
        assertEquals("", ZipReadStorageProvider.getDirectoryID(""));
        assertEquals("dir/", ZipReadStorageProvider.getDirectoryID("dir"));
        assertEquals("dir/", ZipReadStorageProvider.getDirectoryID("dir/"));
    }

    @Test
    public void getRelPath() {
        assertEquals("file.ext",ZipReadStorageProvider.getRelPath("file.ext", ""));

        assertEquals("file.ext",ZipReadStorageProvider.getRelPath("path/file.ext", "path/"));

        assertEquals(null,ZipReadStorageProvider.getRelPath("path/file.ext", "path/subdir/"));

        assertEquals("subdir/file.ext",ZipReadStorageProvider.getRelPath("path/subdir/file.ext", "path/"));
    }

    /** same as ZipReadStorageProvider
     * but without android depedencies
     * and with special includeResult()
     */
    class ZipReadStorageProviderTestable extends ZipReadStorageProvider {
        StringBuilder matrixResult = new StringBuilder();
        @Override
        void includeResult(MatrixCursor result, String filenameWithoutPath, String documentId, String mimeType, Long lastModifiedTime, Long uncompressedSize, int flags) {
            matrixResult.append(filenameWithoutPath).append(",")
                    .append(documentId).append(",")
                    .append(mimeType).append(",")
                    .append(lastModifiedTime).append(",")
                    .append(uncompressedSize).append(",")
                    .append(flags).append("\n");
        }

        @Override
        String getDocumentTypeImpl(String documentId) {
            if (documentId.contains(".jpg")) return "image/jpeg";
            return "my/mime";
        }
    }

    @Test
    public void includeLocalFileHeader() {
        ZipReadStorageProvider.debug = false; // no logcat output

        ZipReadStorageProviderTestable sut = new ZipReadStorageProviderTestable();
        execIncludeLocalFileHeader(sut,
                "ignoreFile.ext", // ignore
                "path/file.ext", // added file
                "path/myimage.jpg", // added file with thumbnail
                "path/subdir/otherFile.ext", // added subdir
                "path/subdir/"); // not added becaus it is a duplicate

        String expected = "file.ext,unittest.zip/path/file.ext,my/mime,0,0,0\n" +
                "myimage.jpg,unittest.zip/path/myimage.jpg,image/jpeg,0,0,1\n" +
                "subdir/,unittest.zip/path/subdir/,vnd.android.document/directory,null,null,0\n";
        assertEquals(expected, sut.matrixResult.toString());
    }

    private ZipReadStorageProviderTestable execIncludeLocalFileHeader(ZipReadStorageProviderTestable sut, String... fullFileNames) {
        String zipId = "unittest.zip";
        String dir = "path/";
        Set<String> duplicates = new HashSet<>();

        for (String fullFileName: fullFileNames) {
            LocalFileHeader lfh = new LocalFileHeader();
            lfh.setFileName(fullFileName);
            sut.includeLocalFileHeader(null, zipId, dir, lfh, duplicates);
        }
        return sut;
    }
}