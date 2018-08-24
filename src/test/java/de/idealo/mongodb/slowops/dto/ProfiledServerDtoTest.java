package de.idealo.mongodb.slowops.dto;

import com.google.common.collect.Lists;
import com.mongodb.ServerAddress;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by kay.agahd on 26.10.16.
 */
public class ProfiledServerDtoTest {

    @Test
    public void getCollectionsPerDatabase() throws Exception {

        ServerAddress[] hosts = {new ServerAddress("127.0.0.1:27017")};
        String[] ns = {"db1.col1", "db2.firstCollection", "db2.secondCollection", "db3.coll.with.dots"};
        ProfiledServerDto dto = new ProfiledServerDto(false, "label", hosts, ns, "adminUser", "adminPw", 100, 1000);
        HashMap<String, List<String>> computedCollectionsPerDb =  dto.getCollectionsPerDatabase();

        HashMap<String, List<String>> expectedCollectionsPerDb =  new HashMap<String, List<String>>();
        expectedCollectionsPerDb.put("db1", Lists.newArrayList("col1"));
        expectedCollectionsPerDb.put("db2", Lists.newArrayList("firstCollection", "secondCollection"));
        expectedCollectionsPerDb.put("db3", Lists.newArrayList("coll.with.dots"));

        assertEquals(expectedCollectionsPerDb, computedCollectionsPerDb);

    }

}