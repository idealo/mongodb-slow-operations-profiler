/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.util;

import com.google.common.collect.Lists;
import com.mongodb.ServerAddress;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
    public static final int MAX_THREADS = Runtime.getRuntime().availableProcessors() * 100;

    public final static String Y_AXIS_SCALE = "yAxisScale";
    public final static String Y_AXIS_SECONDS = "seconds";
    public final static String Y_AXIS_MILLISECONDS = "milliseconds";
    public final static String ADMIN_TOKEN = "adminToken";
    public final static String DEFAULT_RESPONSE_TIMEOUT_IN_MS = "defaultResponseTimeoutInMs";
    public final static String DEFAULT_SLOW_MS = "defaultSlowMS";
    public final static String MAX_WEBLOG_ENTRIES = "maxWeblogEntries";
    public final static String CONFIG_FILE = "config.json";


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


   
    
}
