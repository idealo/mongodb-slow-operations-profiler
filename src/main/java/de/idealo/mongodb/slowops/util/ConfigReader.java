/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.util;

import com.google.common.collect.Lists;
import com.mongodb.ServerAddress;
import de.idealo.mongodb.slowops.dto.ApplicationStatusDto;
import de.idealo.mongodb.slowops.dto.CollectorServerDto;
import de.idealo.mongodb.slowops.dto.ProfiledServerDto;
import de.idealo.mongodb.slowops.monitor.MongoDbAccessor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

/**
 * 
 * 
 * @author kay.agahd
 * @since 26.10.2016
 * @version $Id: $
 * @copyright idealo internet GmbH
 */
public class ConfigReader {
    
    private static final Logger LOG = LoggerFactory.getLogger(ConfigReader.class);

    public static Document CONFIG;

    
    static {
        CONFIG = getConfig(Util.CONFIG_FILE);
    }

    /**
     * returns true is the given cfg passed the validation test and replaced the current configuration
     *
     * @param cfg - config in json format
     * @return
     */
    public static boolean reloadConfig(String cfg){
        boolean result = false;
        final Document bak = CONFIG;
        try {
            CONFIG = Document.parse(cfg);
            if(isValidConfig()){
                result = true;
                ApplicationStatusDto.setMaxWebLogEntries(getLong(CONFIG, Util.MAX_WEBLOG_ENTRIES, ApplicationStatusDto.DEFAULT_MAX_WEBLOG_ENTRIES));
            }else{
                CONFIG = bak;
            }
        } catch (Exception e) {
            LOG.error("Error while reading config: {}", cfg, e);
        }
        return result;
    }

    //rudimental validation
    private static boolean isValidConfig(){
        boolean result = true;
        final CollectorServerDto collectorServer = getCollectorServer();

        result = result && !getProfiledServers(CONFIG).isEmpty();
        result = result && collectorServer.getHosts().length > 0;
        result = result && !collectorServer.getDb().isEmpty();
        result = result && !collectorServer.getCollection().isEmpty();

        return  result;
    }

    public static boolean getBoolean(Document doc, String propertyName, boolean defaultValue) {
        String propNameParts[] = propertyName.split("\\.");
        for(String part : propNameParts){
            Object obj = doc.get(part);
            if(obj != null){
                if(obj instanceof Boolean) {
                    return ((Boolean)obj).booleanValue();
                }else if(obj instanceof Document){
                    doc = (Document) obj;
                }
            }
        }

        return defaultValue;
    }

    public static String getString(Document doc, String propertyName, String defaultValue) {
        String propNameParts[] = propertyName.split("\\.");
        for(String part : propNameParts){
            Object obj = doc.get(part);
            if(obj != null){
                if(obj instanceof String) {
                    return obj.toString();
                }else if(obj instanceof Document){
                    doc = (Document) obj;
                }
            }
        }

        return defaultValue;
    }

    public static long getLong(Document doc, String propertyName, long defaultValue) {
        String propNameParts[] = propertyName.split("\\.");
        for(String part : propNameParts){
            Object obj = doc.get(part);
            if(obj != null){
                if(obj instanceof Number) {
                    return Long.valueOf(obj.toString()).longValue();
                }else if(obj instanceof Document){
                    doc = (Document) obj;
                }
            }
        }

        return defaultValue;
    }

    public static List getList(Document doc, String propertyName, List defaultValue) {
        String propNameParts[] = propertyName.split("\\.");
        for(String part : propNameParts){
            Object obj = doc.get(part);
            if(obj != null){
                if(obj instanceof List){
                    return (List)obj;
                }else if(obj instanceof Document){
                    doc = (Document) obj;
                }
            }
        }

        return defaultValue;
    }

    public static String  getConfig() {
        return CONFIG.toJson();
    }


    private static Document  getConfig(String fileName) {
        Document result = null;
        try {
            InputStream in;
            final File file = new File(fileName);
            if (file.exists() && file.isFile() && file.canRead()) {
                LOG.info("Try to load config file '{}' ", file.getAbsolutePath());
                in = new FileInputStream(new File(fileName));
            } else {
                LOG.info("Try to load config file '{}' from within jar file", fileName);
                in = ConfigReader.class.getClassLoader().getResourceAsStream(fileName);
            }
            if (in != null) {
                StringBuilder builder = new StringBuilder();
                int ch;
                while((ch = in.read()) != -1){
                    builder.append((char)ch);
                }
                result = Document.parse(builder.toString());
                in.close();
                LOG.info("Config file '{}' successfully loaded", fileName);
                if (file.length() < 1024 * 1024) {//only print when less than 1 MB
                    LOG.info("Loaded config: {}", result);
                }
            } else {
                LOG.error("Config file '{}' could not be loaded", fileName);
            }
        } catch (final FileNotFoundException e) {
            LOG.error("Config file '{}' could not be found", fileName, e);
        } catch (final IOException e) {
            LOG.error("Error while reading config file: {}", fileName, e);
        }
        return result;
    }

    private static ServerAddress[] getServerAddresses(List<String> serverAddresses) {
        final List<ServerAddress> result = Lists.newArrayList();
        for (String serverAddress : serverAddresses) {
            try {
                result.add(new ServerAddress(serverAddress));
            } catch (Exception e) {
                LOG.error("Invalid server address: {}", serverAddress, e);
            }
        }
        return result.toArray(new ServerAddress[]{});
    }

    public static CollectorServerDto getCollectorServer(){
        return getCollectorServer(CONFIG);
    }

    public static CollectorServerDto getCollectorServer(Document doc){
        List<String> hostList = getList(doc, "collector.hosts", Lists.newArrayList());
        return new CollectorServerDto(
                getServerAddresses(hostList),
                getString(doc, "collector.db", "profiling"),
                getString(doc, "collector.collection", "slowops"),
                getString(doc, "collector.adminUser", null),
                getString(doc, "collector.adminPw", null),
                getBoolean(doc, "collector.ssl", false)
        );
    }


    public static List<ProfiledServerDto> getProfiledServers(Document doc){
        List<ProfiledServerDto> result = Lists.newArrayList();
        List<Document> profiledServerList = getList(doc, "profiled", Lists.newArrayList());

        for(Document serverDoc : profiledServerList){
            List<String> hostList = getList(serverDoc, "hosts", Lists.newArrayList());
            List<String> nsList = getList(serverDoc, "ns", Lists.newArrayList());
            ProfiledServerDto dto = new ProfiledServerDto(
                    getBoolean(serverDoc, "enabled", false),
                    getString(serverDoc, "label", null),
                    getServerAddresses(hostList),
                    nsList.toArray(new String[]{}),
                    getString(serverDoc, "adminUser", null),
                    getString(serverDoc, "adminPw", null),
                    getBoolean(serverDoc, "ssl", false),
                    getLong(serverDoc, "slowMS", ConfigReader.getLong(doc, Util.DEFAULT_SLOW_MS, MongoDbAccessor.DEFAULT_SLOW_MS)),
                    (int)getLong(serverDoc, "responseTimeoutInMs", getLong(doc, Util.DEFAULT_RESPONSE_TIMEOUT_IN_MS, MongoDbAccessor.DEFAULT_CONNECT_TIMEOUT_MS))
            );
            result.add(dto);
        }
        return result;
    }

    public static void main(String[] args) {
        LOG.info("{}", CONFIG.get("collector"));
        for(String key: CONFIG.keySet()){
            LOG.info(key);
        }
        LOG.info(getString(CONFIG, "collector.db", "no db"));
        LOG.info(getString(CONFIG, "profiled.label", "no label"));
        List list = getList(CONFIG, "profiled", Lists.newArrayList());
        for(Object obj : list){
            if(obj instanceof Document){
                LOG.info(getString((Document)obj, "label", "nix"));
                LOG.info(getString((Document)obj, "hosts", "nix"));
                List ns = getList((Document)obj, "ns", Lists.newArrayList());
                for(Object nsObj : ns){
                    LOG.info("{}", nsObj);
                }
            }
        }
    }
   
    
}
