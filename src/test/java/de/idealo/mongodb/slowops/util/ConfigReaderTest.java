package de.idealo.mongodb.slowops.util;

import com.google.common.collect.Lists;
import com.mongodb.ServerAddress;
import de.idealo.mongodb.slowops.dto.CollectorServerDto;
import de.idealo.mongodb.slowops.dto.ProfiledServerDto;
import org.bson.Document;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by kay.agahd on 27.10.16.
 */
public class ConfigReaderTest {

    final ConfigReader configReader = new ConfigReader();
    final Document doc = Document.parse("{"
            +"\"collector\":{"
                +"\"hosts\":[\"localhost:27017\"],"
                +"\"db\":\"profiling\","
                +"\"collection\":\"slowops\","
                +"\"adminUser\":\"\","
                +"\"adminPw\":\"\""
            +"},"
            +"\"profiled\":["
                +"{\"label\":\"offerstore-de\","
                +"\"hosts\":[\"offerstore-en-router-01.ipx:27017\"],"
                +"\"ns\":[\"offerStore.offer\"],"
                +"\"adminUser\":\"admin\","
                +"\"adminPw\":\"XXX\","
                +"\"slowMs\":100"
                +"},"
                +"{\"collect\":true,"
                +"\"label\":\"offerstore-pl\","
                +"\"hosts\":[\"mongo-030.ipx:27017\",\"mongo-017.ipx:27017\",\"mongo-018.ipx:27017\"],"
                +"\"ns\":[\"offerStore.offer\"],"
                +"\"adminUser\":\"admin\","
                +"\"adminPw\":\"XXX\","
                +"\"slowMs\":100"
                +"}"
            +"],"
            +"\"yAxisScale\":\"milliseconds\""
            +"}");



    @Test
    public void getString() throws Exception {
        assertEquals("profiling", configReader.getString(doc, "collector.db", ""));
        assertEquals(null, configReader.getString(doc, "collector.nonExistingField", null));
        assertEquals("", configReader.getString(doc, "collector.nonExistingField", ""));
        assertEquals("default", configReader.getString(doc, "collector.nonExistingField", "default"));
        assertEquals("milliseconds", configReader.getString(doc, "yAxisScale", ""));

        assertNotEquals("other", configReader.getString(doc, "collector.db", ""));
        assertNotEquals("other", configReader.getString(doc, "collector.nonExistingField", null));
        assertNotEquals("other", configReader.getString(doc, "collector.nonExistingField", ""));
        assertNotEquals("other", configReader.getString(doc, "collector.nonExistingField", "default"));
        assertNotEquals("other", configReader.getString(doc, "yAxisScale", ""));

    }

    @Test
    public void getList() throws Exception {
        assertEquals(Lists.newArrayList("localhost:27017"), configReader.getList(doc, "collector.hosts", null));
        assertNotEquals(Lists.newArrayList("wronghost:27017"), configReader.getList(doc, "collector.hosts", null));
    }

    @Test
    public void getCollectorServer() throws Exception {
        CollectorServerDto collector = configReader.getCollectorServer(doc);
        assertArrayEquals(new ServerAddress[]{new ServerAddress("localhost:27017")}, collector.getHosts());
        assertEquals("profiling", collector.getDb());
        assertEquals("slowops", collector.getCollection());
        assertEquals("profiling", collector.getDb());
        assertEquals("", collector.getAdminUser());
        assertEquals("", collector.getAdminPw());
    }

    @Test
    public void getProfiledServers() throws Exception {
        List<ProfiledServerDto> dtoList = configReader.getProfiledServers(doc);
        assertEquals(2, dtoList.size());

        ProfiledServerDto dto = dtoList.get(0);
        assertFalse(dto.isEnabled());
        assertEquals("offerstore-de", dto.getLabel());
        assertArrayEquals(new ServerAddress[]{new ServerAddress("offerstore-en-router-01.ipx:27017")}, dto.getHosts());
        assertArrayEquals(new String[]{"offerStore.offer"}, dto.getNs());
        assertEquals("admin", dto.getAdminUser());
        assertEquals("XXX", dto.getAdminPw());
        assertEquals(100l, dto.getSlowMs());

        dto = dtoList.get(1);
        assertTrue(dto.isEnabled());
        assertEquals("offerstore-pl", dto.getLabel());
        assertArrayEquals(new ServerAddress[]{new ServerAddress("mongo-030.ipx:27017"),new ServerAddress("mongo-017.ipx:27017"),new ServerAddress("mongo-018.ipx:27017")}, dto.getHosts());
        assertArrayEquals(new String[]{"offerStore.offer"}, dto.getNs());
        assertEquals("admin", dto.getAdminUser());
        assertEquals("XXX", dto.getAdminPw());
        assertEquals(100l, dto.getSlowMs());
    }

}