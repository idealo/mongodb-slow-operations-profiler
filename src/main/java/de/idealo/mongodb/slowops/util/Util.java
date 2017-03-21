/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.util;

import com.google.common.collect.Lists;
import com.mongodb.*;
import de.idealo.mongodb.slowops.monitor.MongoDbAccessor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 
 * 
 * @author kay.agahd
 * @since 26.03.2013
 * @version $Id: $
 * @copyright idealo internet GmbH
 */
public class Util {
    
    private static final Logger LOG = LoggerFactory.getLogger(Util.class);
    
    public final static char PROPERTIES_SEPARATOR = ',';

    public final static String Y_AXIS_SCALE = "yAxisScale";
    public final static String Y_AXIS_SECONDS = "seconds";
    public final static String Y_AXIS_MILLISECONDS = "milliseconds";

    public final static String CONFIG_FILE = "config.json";


    public final static Document CONFIG;

    
    static {

        CONFIG = getConfig(CONFIG_FILE);
    }


    public static List<ServerAddress> getServerAddresses(String serverAddresses) {
        final List<ServerAddress> result = Lists.newArrayList();
        final String[] servers = serverAddresses.split(""+PROPERTIES_SEPARATOR);
        
        for (String server : servers) {
            try {
                result.add(new ServerAddress(server));
            } catch (Exception e) {
                LOG.error("Invalid server address: {}", server, e);
            }
        }
        
        return result;
    }

    /**
     * If the given serverAddress is a router (mongos) of a sharded system,
     * it returns a list of all mongod ServerAddresses of the sharded system.
     * If the given serverAddress is a mongod,
     * the returned List will contain the ServerAddresses of all replSet members
     * or, if not a replSet, the given ServerAddress.
     *
     * @param adminUser
     * @param adminPassword
     * @param serverAddress
     * @return
     */
    public static List<ServerAddress> getMongodAddresses(String adminUser, String adminPassword, ServerAddress serverAddress) {

        final List<ServerAddress> result = Lists.newLinkedList();
        MongoDbAccessor mongo = null;

        try {
            mongo = new MongoDbAccessor(adminUser, adminPassword, serverAddress);

            try {
                final Document doc = mongo.runCommand("admin", new BasicDBObject("listShards", 1));
                final Object shards = doc.get("shards");
                if(shards != null) {
                    final ArrayList<Document> list = (ArrayList<Document>)shards;
                    for (Object obj : list) {
                        final Document dbo = (Document)obj;
                        String hosts = dbo.getString("host");
                        int slashIndex = hosts.indexOf("/");
                        if(slashIndex > -1) {
                            hosts = hosts.substring(slashIndex+1);
                        }
                        //hosts = hosts.replace(',', ' ');

                        result.addAll(Util.getServerAddresses(hosts));
                    }
                    return result;
                }
            } catch (MongoCommandException e) {//replSets do not know the command listShards, thus throwing an Exception
                try {
                    final Document doc = mongo.runCommand("admin", new BasicDBObject("replSetGetStatus", 1));
                    final Object members = doc.get("members");
                    if (members != null && members instanceof ArrayList) {
                        final ArrayList<Document> list = (ArrayList<Document>) members;
                        for (Object obj : list) {
                            final Document dbo = (Document) obj;
                            final String host = dbo.getString("name");
                            result.add(new ServerAddress(host));
                        }
                        return result;
                    }
                }catch (MongoCommandException e2) {//single nodes do not know the command replSets, thus throwing an Exception
                    final Document doc = mongo.runCommand("admin", new BasicDBObject("serverStatus", 1));
                    final Object repl = doc.get("repl");//single nodes don't have serverStatus().repl
                    if(repl == null) result.add(serverAddress);
                }
            }

        } catch (MongoException e) {
            LOG.error("Couldn't start mongo node at address {}", serverAddress, e);
        } finally {
            if(mongo != null) {
                mongo.closeConnections();
            }
        }
        return result;

    }
    
    
    private static Properties getProperties(String fileName) {
        Properties result = new Properties();
        try {
            InputStream in;
            final File file = new File(fileName);
            if (file.exists() && file.isFile() && file.canRead()) {
                LOG.info("Try to load properties file '{}' ", file.getAbsolutePath());
                in = new FileInputStream(new File(fileName));
            } else {
                LOG.info("Try to load properties file '{}' from within jar file", fileName);
                in = Util.class.getClassLoader().getResourceAsStream(fileName);
            }
            if (in != null) {
                result.load(in);
                in.close();
                LOG.info("Properties file '{}' successfully loaded", fileName);
                if (file.length() < 1024 * 1024) {//only print when less than 1 MB
                    LOG.info("Loaded properties: {}", result);
                }
            } else {
                LOG.error("Properties file '{}' could not be loaded", fileName);
            }
        } catch (final FileNotFoundException e) {
            LOG.error("Properties file '{}' could not be found", fileName, e);
        } catch (final IOException e) {
            LOG.error("Error while reading properties file: {}", fileName, e);
        }
        return result;
    }


    public static String getString(Document doc, String propertyName, String defaultValue) {
        String propNameParts[] = propertyName.split("\\.");
        for(String part : propNameParts){
            Object obj = doc.get(part);
            if(obj != null && obj instanceof Document){
                continue;
            }else{
                return obj.toString();
            }
        }

        return null;
    }

    public static List getList(Document doc, String propertyName, String defaultValue) {
        String propNameParts[] = propertyName.split("\\.");
        for(String part : propNameParts){
            Object obj = doc.get(part);
            if(obj != null && obj instanceof Document){
                continue;
            }else if(obj != null && obj instanceof List){
                return (List)obj;
            }
        }

        return null;
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
                LOG.info("Try to load properties file '{}' from within jar file", fileName);
                in = Util.class.getClassLoader().getResourceAsStream(fileName);
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

    public static void main(String[] args) {
        LOG.info("{}", CONFIG.get("collector"));
        for(String key: CONFIG.keySet()){
            LOG.info(key);
        }
        LOG.info(getString(CONFIG, "profiled.label", "nix"));
        List list = getList(CONFIG, "profiled", "nix");
        for(Object obj : list){
            if(obj instanceof Document){
                LOG.info(getString((Document)obj, "label", "nix"));
                LOG.info(getString((Document)obj, "hosts", "nix"));
                List ns = getList((Document)obj, "ns", "nix");
                for(Object nsObj : ns){
                    LOG.info("{}", nsObj);
                }
            }
        }
    }
   
    
}
