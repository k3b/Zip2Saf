package de.k3b.zip2saf.data;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ArrayList;

public class MountInfoRepositoryTest {
    @Test
    public void testToString() {
        MountInfoRepository repository = new MountInfoRepository(new ArrayList<>());
        repository.add(new MountInfo("my_id","my_url","my_details"));

        assertEquals("[{'zipId':'my_id','uri':'my_url','details':'my_details'}]", repository.toString().replace('"', '\''));
    }

    @Test
    public void getById() {
        MountInfoRepository repository = MountInfoRepository.fromString ("[{'zipId':'my_id','uri':'my_url','details':'my_details'}]");
        assertEquals(1, repository.getCount());
        assertNotNull(repository.getById("my_id"));
        assertNull(repository.getById("hallo"));
    }
}
