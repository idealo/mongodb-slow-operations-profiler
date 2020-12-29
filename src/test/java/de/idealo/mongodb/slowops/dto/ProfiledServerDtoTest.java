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
        String[] ns = {"db1.col1", "admin.foo", "local.bar", "db2.firstCollection", "db2.secondCollection", "db3.coll.with.dots"};
        List<String> defaultExcludedDbs = Lists.newArrayList();
        defaultExcludedDbs.add("admin");
        defaultExcludedDbs.add("local");
        ProfiledServerDto dto = new ProfiledServerDto(false, "label", hosts, ns, "adminUser", "adminPw", false, 100, 1000, defaultExcludedDbs, 1);
        HashMap<String, List<String>> computedCollectionsPerDb =  dto.getCollectionsPerDatabase();

        HashMap<String, List<String>> expectedCollectionsPerDb =  new HashMap<String, List<String>>();
        expectedCollectionsPerDb.put("db1", Lists.newArrayList("col1"));
        expectedCollectionsPerDb.put("db2", Lists.newArrayList("firstCollection", "secondCollection"));
        expectedCollectionsPerDb.put("db3", Lists.newArrayList("coll.with.dots"));

        assertEquals(expectedCollectionsPerDb, computedCollectionsPerDb);

        //test to exclude default and one specific db
        ns = new String[]{"db1.col1", "admin", "dbToBeExcluded.foo", "!dbToBeExcluded"};
        dto = new ProfiledServerDto(false, "label", hosts, ns, "adminUser", "adminPw", false, 100, 1000, defaultExcludedDbs, 1);
        computedCollectionsPerDb =  dto.getCollectionsPerDatabase();

        expectedCollectionsPerDb =  new HashMap<String, List<String>>();
        expectedCollectionsPerDb.put("db1", Lists.newArrayList("col1"));

        assertEquals(expectedCollectionsPerDb, computedCollectionsPerDb);

        //test wrong way to exclude db
        ns = new String[]{"db1.col1", "!dbToBeExcluded", "dbToBeExcluded.foo"};
        dto = new ProfiledServerDto(false, "label", hosts, ns, "adminUser", "adminPw", false, 100, 1000, defaultExcludedDbs, 1);
        computedCollectionsPerDb =  dto.getCollectionsPerDatabase();

        expectedCollectionsPerDb =  new HashMap<String, List<String>>();
        expectedCollectionsPerDb.put("db1", Lists.newArrayList("col1"));
        expectedCollectionsPerDb.put("dbToBeExcluded", Lists.newArrayList("foo"));

        assertEquals(expectedCollectionsPerDb, computedCollectionsPerDb);



    }

}