/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.collector;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.*;

import com.mongodb.*;

import de.idealo.mongodb.slowops.util.Util;

/**
 * 
 * 
 * @author kay.agahd
 * @since 25.02.2013
 * @version $Id: $
 * @copyright idealo internet GmbH
 */

public class ProfilingReader extends Thread{
    
    private static final Logger LOG = LoggerFactory.getLogger(ProfilingReader.class);
    private static final int RETRY_AFTER_SECONDS = 60*60;//1 hour
    
    private final ServerAddress serverAddress;
    private final String country;
    private final String database;
    private final String collection;
    private AtomicLong doneJobs = new AtomicLong(0);
    private boolean stop = false;
    
    private final BlockingQueue<ProfilingEntry> jobQueue;
    private DBCollection profileCollection;
    private Mongo mongo;
    private Date lastTs;
    private ServerChecker restarter;
    private DBCursor profileCursor;
     
    
    public ProfilingReader(BlockingQueue<ProfilingEntry> jobQueue, ServerAddress adr, Date lastTs, String country, ServerChecker restarter) {
        this.jobQueue = jobQueue;
        this.serverAddress = adr;
        this.country = country;
        this.restarter = restarter;
        if(lastTs == null) {
            this.lastTs = new Date(0);
        }else {
            this.lastTs = lastTs;
        }
        database = Util.getProperty(Util.PROFILED_DATABASE, null);
        collection = Util.getProperty(Util.PROFILED_COLLECTION, null);
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                closeConnections();
            }
        });
    }
    
    private void init() {
        LOG.info(">>> init");
        
        try {
            mongo = new Mongo(serverAddress);
            final DB db = mongo.getDB(database);
            
            final String login = Util.getProperty(Util.PROFILED_USER_LOGIN, null);
            final String pw = Util.getProperty(Util.PROFILED_USER_PASSWORD, null);
            if(login!= null && pw != null) {
                boolean ok = db.authenticate(login, pw.toCharArray());
                LOG.info("auth on " + serverAddress + " ok: " + ok);
            }
            
            final CommandResult cr = db.command("isMaster");
            final Object ismaster = cr.get("ismaster");
            if(ismaster != null && !((Boolean)(ismaster)).booleanValue()) {
                mongo.setReadPreference(ReadPreference.secondary());
            }
            
            profileCollection = db.getCollection("system.profile");
            if(profileCollection == null) {
                throw new IllegalArgumentException("Can't continue without profile collection for " + getServerAddress());
            }
        } catch (MongoException e) {
            LOG.error("Error while initializing mongo at address " + serverAddress, e);
            closeConnections();
            if(restarter != null) {
                restarter.checkForNewOrRemovedMongods(this);
            }
        }
        LOG.info("<<< init");
    }
    
    private void closeConnections() {
        LOG.info(">>> closeConnections " + getServerAddress());
        
        try {
            if(profileCursor != null) {
                profileCursor.close();
            }
        } catch (Throwable e) {
            LOG.error("Error while closing profileCursor ", e);
        }
        
        try {
            if(mongo != null) {
                mongo.close();
                mongo = null;
            }
        } catch (Throwable e) {
            LOG.error("Error while closing mongo ", e);
        }
        
        LOG.info("<<< closeConnections " + getServerAddress());
    }

    private DBCursor getProfileCursor(int limit) {
        LOG.info(">>> getProfileCursor for " + getServerAddress() + ", limit: " + limit + ", lastTs: " + lastTs);
        final BasicDBObject query = new BasicDBObject();
        final BasicDBObject orderBy = new BasicDBObject();
        
        query.put("ns", database + "." + collection);
        query.put("ts",  new BasicDBObject( "$gt" , lastTs ));
        orderBy.put("$natural", Long.valueOf(1));//asc from old to new
        
        return profileCollection.find(query).limit(limit).sort(orderBy);

    }
    
    private void readSystemProfile() {
        
        if(mongo == null) {
            LOG.error("Can't read entries from "+serverAddress+" since mongo is not initialized.");
            return;
        }
        
        profileCursor = getProfileCursor(1);
        
        try {
        
            if(profileCursor.hasNext()) {
                final DBObject doc = profileCursor.next();
                lastTs = (Date)doc.get("ts");
                LOG.info("last profile entry for " + getServerAddress() + " from: " + lastTs);
                LOG.info(""+doc);
                filterDoc(doc);
                profileCursor.close();
            }else {
                LOG.info("No newer profile entry found for " + getServerAddress());
                return;
            }

            while(!stop) {
                profileCursor = getProfileCursor(0);//get tailable cursor from oldest to newest profile entry
                profileCursor.addOption(Bytes.QUERYOPTION_TAILABLE);
                profileCursor.addOption(Bytes.QUERYOPTION_AWAITDATA);
                
             
                while(!stop && profileCursor.hasNext()) {
            
                    final DBObject doc = profileCursor.next();
                    lastTs = (Date)doc.get("ts");
                    filterDoc(doc);
                    LOG.info(""+doc);
                }
                
            }
        }catch(Exception e) {
            LOG.error("Exception occurred on " + getServerAddress() + " , will return and try again.", e);
        }finally {
            if(profileCursor != null) {
                profileCursor.close();
            }
        }
    }
    
    //3 examples of how the field "query" may be formatted:
    //query:
    //{ "query" : { "shopId" : 279073, "onlineProductIds" : { "$ne" : null } }, "user" : "" }
    //query.query:
    //{ "query" : { "query" : { "shopId" : 275417 }, "orderby" : { "_id" : NumberLong(1) } }, "user" : "pl-updMetaData" }
    //query.$query:
    //{ "query" : { "$query" : { "shopId" : 275418 }, "$comment" : "profiling comment" }, "user" : "profiling" }
    //{ "query" : { "$query" : { "shopId" : 283036, "smallPicture" : { "$ne" : null } }, "$orderby" : { "lastShopDataChange" : -1 } }, "user" : "" }
    private void filterDoc(DBObject doc) {
        
        final Object query = doc.get("query");
        Set<String> fields = null;
        Set<String> sort = null;
        if(query != null  && query instanceof DBObject) {
            final DBObject queryObj = (DBObject)query;
            final Object innerQuery = queryObj.get("query");//test if "query.query"
            if(innerQuery != null) {//format is "query.query"
                fields = getFields(innerQuery);
                final Object orderbyObj = queryObj.get("orderby");
                if(orderbyObj != null) {
                    sort = getFields(orderbyObj);
                }
            }else {//format is "query.$query" or "query"
                final Object innerDollarQuery = queryObj.get("$query");
                if(innerDollarQuery != null) {
                    fields = getFields(innerDollarQuery);
                    final Object orderbyObj = queryObj.get("$orderby");
                    if(orderbyObj != null) {
                        sort = getFields(orderbyObj);
                    }
                }else {//format is "query"
                    fields = getFields(query);
                    sort = null;
                }
            }
        }
        
        final ProfilingEntry entry = new ProfilingEntry((Date)doc.get("ts"), serverAddress, 
                "" + doc.get("op"), "" + doc.get("user"), fields, sort, getInteger(doc, "nreturned"), 
                getInteger(doc, "responseLength"), getInteger(doc, "millis"));
        
        try {
            
            jobQueue.put(entry);
            doneJobs.incrementAndGet();
            
        } catch (InterruptedException e) {
            LOG.error("InterruptedException while waiting to put entry into jobQueue", e);
        }
        
    }
    
    private Integer getInteger(DBObject dbObj, String name) {
        if(dbObj != null) {
            final Object obj = dbObj.get(name);
            if(obj != null) {
                return (Integer)(obj);
            }
        }
        return null;
    }
    
    private Set<String> getFields(Object obj) {
        if(obj != null && obj instanceof DBObject) {
            final DBObject dbObj = (DBObject)obj;
            return dbObj.keySet();
        }
        return new HashSet<String>();
    }
    
    public long getDoneJobs() {
        return doneJobs.get();
    }

    
    public ServerAddress getServerAddress() {
        return serverAddress;
    }
    
    public String getCountry() {
        return country;
    }
    
    
    public void terminate() {
        stop = true;
        interrupt(); //need to interrupt when sleeping or waiting on tailable cursor data
        closeConnections();
    }
    
    public boolean isStopped() {
        return stop;
    }

    
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        LOG.info("Run started " + getServerAddress());
        try {
            while(!stop) {
                init();
                readSystemProfile();
                closeConnections();
                if(!stop) {
                    try {
                        LOG.info("sleeping..." + getServerAddress());
                        Thread.sleep(1000*RETRY_AFTER_SECONDS);
                        
                    } catch (InterruptedException e) {
                        LOG.error("InterruptedException while sleeping.");
                    }
                }
            }
        }finally {
            terminate();
        }
        LOG.info("Run terminated " + getServerAddress());
    }
    


    public static void main(String[] args) throws Exception {
        final ServerAddress address =  new ServerAddress("localhost",27002);
        
        BlockingQueue<ProfilingEntry> jobQueue = new LinkedBlockingQueue<ProfilingEntry>();
        ProfilingReader reader = new ProfilingReader(jobQueue, address, null, "it", null);
        reader.start();
        //Thread.sleep(5000);
        //reader.stop();
        
        LOG.info("main end");
        
        
    }

    

    
}
