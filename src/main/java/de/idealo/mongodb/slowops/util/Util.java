/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.util;

import java.io.*;
import java.net.*;
import java.util.*;

import org.slf4j.*;

import com.google.common.collect.Lists;
import com.mongodb.*;

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
    
    public final static String CONFIG_PROPERTIES = "config.properties";
    public final static char PROPERTIES_SEPARATOR = ',';
    public final static String COLLECTOR_SERVER_ADDRESSES = "collector.server.addresses";
    public final static String COLLECTOR_DATABASE = "collector.database";
    public final static String COLLECTOR_COLLECTION = "collector.collection";
    public final static String COLLECTOR_LOGIN = "collector.login";
    public final static String COLLECTOR_PASSWORD = "collector.password";
    public final static String PROFILED_SEED_ADDRESSES_PREFIX = "profiled.mongod.seed.addresses.";
    public final static String PROFILED_MONGOD_NAMES = "profiled.mongod.names";
    public final static String PROFILED_DATABASE = "profiled.mongod.database";
    public final static String PROFILED_COLLECTION = "profiled.mongod.collection";
    public final static String PROFILED_ADMIN_LOGIN = "profiled.mongod.admin.login";
    public final static String PROFILED_ADMIN_PASSWORD = "profiled.mongod.admin.password";
    public final static String PROFILED_USER_LOGIN = "profiled.mongod.user.login";
    public final static String PROFILED_USER_PASSWORD = "profiled.mongod.user.password";
    public final static String Y_AXIS_SCALE = "y.axis.scale";
    public final static String Y_AXIS_SECONDS = "seconds";
    public final static String Y_AXIS_MILLISECONDS = "milliseconds";
    
    
    
    
    private final static Properties PROPS;
    
    static {
        PROPS = getProperties(CONFIG_PROPERTIES);
    }
    
    public static List<ServerAddress> getServerAddresses(String serverAddresses) {
        final List<ServerAddress> result = Lists.newArrayList();
        final String[] servers = serverAddresses.split(""+PROPERTIES_SEPARATOR);
        
        for (String server : servers) {
            try {
                result.add(new ServerAddress(server));
            } catch (UnknownHostException e) {
                LOG.error("Invalid server address: " + server, e);
            }
        }
        
        return result;
    }
    
    /**
     *  
     * Returns a list of all mongod ServerAddresses of the sharded system,
     * no matter if the given serverAddress is a router (mongos) or mongod (replSet).  
     * 
     * @param serverAddress
     * @param country
     * @return
     */
    public static List<ServerAddress> getMongodAddresses(String country) {
        
        final String seeds = PROPS.getProperty(PROFILED_SEED_ADDRESSES_PREFIX + country);
        final List<ServerAddress> seedAddresses = Util.getServerAddresses(seeds);
        
        return getMongodAddresses(seedAddresses.get(0));
            
    }
    
    /**
     *  
     * Returns a list of all mongod ServerAddresses of the sharded system,
     * no matter if the given serverAddress is a router (mongos) or mongod (replSet).  
     * 
     * @param serverAddress
     * @param country
     * @return
     */
    private static List<ServerAddress> getMongodAddresses(ServerAddress serverAddress) {
        
        final List<ServerAddress> result = Lists.newLinkedList();
        Mongo mongo = null;
        
        try {
            mongo = new Mongo(serverAddress);
            final DB db = mongo.getDB("admin");
            
            final String login = getProperty(PROFILED_ADMIN_LOGIN, null);
            final String pw = getProperty(PROFILED_ADMIN_PASSWORD, null);
            if(login!= null && pw != null) {
                boolean ok = db.authenticate(login, pw.toCharArray());
                LOG.info("auth on " + serverAddress + " ok: " + ok);
            }
            
            CommandResult cr = db.command("listShards");
            final Object shards = cr.get("shards");
            if(shards != null) {
                final BasicDBList list = (BasicDBList)shards;
                for (Object obj : list) {
                    final BasicDBObject dbo = (BasicDBObject)obj;
                    String hosts = dbo.getString("host");
                    int slashIndex = hosts.indexOf("/");
                    if(slashIndex > -1) {
                        hosts = hosts.substring(slashIndex+1);
                    }
                    hosts = hosts.replace(',', PROPERTIES_SEPARATOR);
                    
                    result.addAll(Util.getServerAddresses(hosts));
                }
                
            }else{//replSet or single mongod
                
                cr = db.command("replSetGetStatus");
                final Object members = cr.get("members");
                if(members != null && members instanceof BasicDBList) {
                    final BasicDBList list = (BasicDBList)members;
                    for (Object obj : list) {
                        final BasicDBObject dbo = (BasicDBObject)obj;
                        final String host = dbo.getString("name");
                        try {
                            result.add(new ServerAddress(host));
                        } catch (UnknownHostException e) {
                            LOG.error("Couldn't add replSet node, host: " + host, e);
                        }
                    }
                    
                }else {//single mongod
                    result.add(serverAddress);
                }
            }
            
        } catch (MongoException e) {
            LOG.error("Couldn't start mongo node at address " + serverAddress, e);
        } finally {
            if(mongo != null) {
                mongo.close();
            }
        }
        return result;
        
    }
    
    
    private static Properties getProperties(String fileName) {
        final Properties result = new Properties();
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
            LOG.error("Properties file '" + fileName + "' could not be found", e);
        } catch (final IOException e) {
            LOG.error("Error while reading properties file: " + fileName, e);
        }
        return result;
    }

    
    public static String getProperty(String propertyName, String defaultValue) {
        return PROPS.getProperty(propertyName, defaultValue);
    }
    
   
    
}
