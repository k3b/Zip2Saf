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

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ArrayList;

public class MountInfoRepositoryTest {
    @Test
    public void testToString() {
        MountInfoRepository repository = new MountInfoRepository(new ArrayList<>());
        repository.add(new MountInfo("my_id","my_url","my_details"));

        assertEquals("[{'zipId':'[[add]]','uri':'','details':''},{'zipId':'my_id','uri':'my_url','details':'my_details'}]", repository.toString().replace('"', '\''));
    }

    @Test
    public void getById() {
        MountInfoRepository repository = MountInfoRepository.fromString ("[{'zipId':'my_id','uri':'my_url','details':'my_details'}]");
        assertEquals(2, repository.getCount());
        assertNotNull(repository.getById("my_id"));
        assertNull(repository.getById("hallo"));
    }

    @Test
    public void fixCreateNewItem() {
        MountInfoRepository repository = new MountInfoRepository(new ArrayList<>());
        repository.add(new MountInfo("[[must fix]]","",""));
        assertTrue("repair", repository.fixCreateNewItem());
        assertFalse("already repearied", repository.fixCreateNewItem());
    }

}
